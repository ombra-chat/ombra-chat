use crate::settings;
use std::sync::mpsc::{channel, Receiver, Sender};
use std::thread;

pub struct Client<R: tauri::Runtime> {
    client_id: i32,
    app: tauri::AppHandle<R>,
    tx: Sender<tdlib::enums::Update>,
    rx: Receiver<tdlib::enums::Update>,
}

impl<R: tauri::Runtime> Client<R> {
    pub fn new(app: tauri::AppHandle<R>) -> Self {
        let (tx, rx) = channel::<tdlib::enums::Update>();
        Client {
            client_id: tdlib::create_client(),
            app,
            tx,
            rx,
        }
    }

    pub fn start(&self) {
        let tx = self.tx.clone();
        let client_id = self.client_id;
        thread::spawn(move || loop {
            if let Some((update, _client_id)) = tdlib::receive() {
                if let Err(e) = tx.send(update) {
                    log::error!("Error sending update through the channel: {}", e);
                }
            }
        });
        thread::spawn(move || init(client_id));
        while let Ok(update) = self.rx.recv() {
            trpl::run(async {
                self.handle_update(update).await;
            });
        }
    }

    async fn handle_update(&self, update: tdlib::enums::Update) {
        use tdlib::enums::AuthorizationState;
        use tdlib::enums::Update;

        match update {
            Update::AuthorizationState(state) => match state.authorization_state {
                AuthorizationState::WaitTdlibParameters => {
                    log::trace!("WaitTdlibParameters");
                    self.send_tdlib_parameters().await;
                }
                _ => {
                    // TODO
                }
            },
            _ => {
                // TODO
            }
        }
    }

    async fn send_tdlib_parameters(&self) {
        log::trace!("send_tdlib_parameters");

        let params = match settings::get_tdlib_parameters(&self.app) {
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
