use crate::{settings, should_close, AppState};
use serde::Serialize;
use std::sync::atomic::Ordering;
use std::sync::{atomic::AtomicBool, mpsc::channel};
use std::sync::{Arc, Mutex};
use std::thread;
use tauri::{Emitter, Manager};

pub struct Client {
    client_id: i32,
}

impl Client {
    pub fn new() -> Self {
        Client {
            client_id: tdlib::create_client(),
        }
    }

    pub fn start<R: tauri::Runtime>(&self, app: &tauri::AppHandle<R>) {
        let (tx, rx) = channel::<tdlib::enums::Update>();
        let client_id = self.client_id;
        let close = Arc::new(AtomicBool::new(false));

        {
            let state = app.state::<Mutex<AppState>>();
            let mut state = state.lock().unwrap();
            state.client_id = client_id;
        }

        let close_clone = close.clone();
        thread::spawn(move || {
            while !close_clone.load(Ordering::Relaxed) {
                if let Some((update, _client_id)) = tdlib::receive() {
                    if let Err(e) = tx.send(update) {
                        log::error!("Error sending update through the channel: {}", e);
                    }
                }
            }
            log::trace!("tdlib::receive loop ended");
        });
        thread::spawn(move || init(client_id));
        while let Ok(update) = rx.recv() {
            trpl::run(async {
                self.handle_update(app, update).await;
            });
            if should_close(app) {
                break;
            }
        }
        log::trace!("rx.recv loop ended");
        close.store(true, Ordering::SeqCst);
    }

    async fn handle_update<R: tauri::Runtime>(
        &self,
        app: &tauri::AppHandle<R>,
        update: tdlib::enums::Update,
    ) {
        use tdlib::enums::AuthorizationState;
        use tdlib::enums::Update;

        match update {
            Update::AuthorizationState(state) => match state.authorization_state {
                AuthorizationState::WaitTdlibParameters => {
                    self.send_tdlib_parameters(app).await;
                }
                AuthorizationState::WaitPhoneNumber => {
                    emit(app, "ask-login-phone-number", ());
                }
                AuthorizationState::WaitCode { .. } => {
                    emit(app, "ask-login-code", ());
                }
                AuthorizationState::WaitPassword { .. } => {
                    emit(app, "ask-login-password", ());
                }
                AuthorizationState::Ready => {
                    emit(app, "logged-in", ());
                }
                AuthorizationState::LoggingOut => {
                    // ignored
                }
                _ => {
                    log::warn!("Unsupported authorization state {:?}", state);
                }
            },
            _ => {
                // TODO
            }
        }
    }

    async fn send_tdlib_parameters<R: tauri::Runtime>(&self, app: &tauri::AppHandle<R>) {
        log::trace!("send_tdlib_parameters");

        let params = match settings::get_tdlib_parameters(app) {
            Ok(s) => s,
            Err(e) => {
                log::error!("Error retrieving tdlib parameters: {}", e);
                return;
            }
        };

        tdlib::functions::set_tdlib_parameters(
            false,
            params.database_directory,
            String::new(),
            params.database_encryption_key,
            true,
            true,
            true,
            true,
            params.api_id,
            params.api_hash,
            "en".into(),
            "Desktop".into(),
            String::new(),
            "1.0".into(),
            true,
            false,
            self.client_id,
        )
        .await
        .unwrap_or_else(|err| {
            log::error!("Error setting tdlib_parameters: {}", err.message);
        });
    }
}

fn init(client_id: i32) {
    trpl::run(async {
        if let Err(e) = tdlib::functions::set_log_verbosity_level(
            if log::log_enabled!(log::Level::Trace) {
                5
            } else if log::log_enabled!(log::Level::Debug) {
                4
            } else if log::log_enabled!(log::Level::Info) {
                3
            } else if log::log_enabled!(log::Level::Warn) {
                2
            } else {
                0
            },
            client_id,
        )
        .await
        {
            log::warn!("Error setting the tdlib log level: {:?}", e);
        }

        if let Err(e) = tdlib::functions::get_option("version".into(), client_id).await {
            log::warn!("Error retrieving the tdlib version: {:?}", e);
        }
    });
}

fn emit<S: Serialize + Clone, R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    event: &str,
    payload: S,
) {
    log::trace!("Emitting event {}", event);
    app.emit(event, payload).unwrap_or_else(|err| {
        log::error!("Error emitting event: {}", err);
    });
}

pub fn get_client_id<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> i32 {
    let state = app.state::<Mutex<AppState>>();
    let state = state.lock().unwrap();
    state.client_id
}
