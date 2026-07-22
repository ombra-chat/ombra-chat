use std::path::Path;

use crate::{
    crypto,
    model::{
        InputMessageContent, InputMessageDocument, InputMessagePgpFile, InputMessagePgpText,
        InputMessagePhoto, InputMessageText,
    },
    thumbnails,
};

pub trait MessagePreparer<T, R: tauri::Runtime> {
    async fn prepare_message(&self, app: &tauri::AppHandle<R>, chat_id: i64) -> Result<T, String>;
}

pub trait MessageCleaner<R: tauri::Runtime> {
    async fn cleanup(&self, app: &tauri::AppHandle<R>);
}

impl<R: tauri::Runtime> MessagePreparer<tdlib::enums::InputMessageContent, R>
    for InputMessageContent
{
    async fn prepare_message(
        &self,
        app: &tauri::AppHandle<R>,
        chat_id: i64,
    ) -> Result<tdlib::enums::InputMessageContent, String> {
        Ok(match self {
            InputMessageContent::Text(input) => {
                tdlib::enums::InputMessageContent::InputMessageText(
                    input.prepare_message(app, chat_id).await?,
                )
            }
            InputMessageContent::Document(input) => {
                tdlib::enums::InputMessageContent::InputMessageDocument(
                    input.prepare_message(app, chat_id).await?,
                )
            }
            InputMessageContent::Photo(input) => {
                tdlib::enums::InputMessageContent::InputMessagePhoto(
                    input.prepare_message(app, chat_id).await?,
                )
            }
            InputMessageContent::PgpText(input) => {
                tdlib::enums::InputMessageContent::InputMessageDocument(
                    input.prepare_message(app, chat_id).await?,
                )
            }
            InputMessageContent::PgpFile(input) => {
                tdlib::enums::InputMessageContent::InputMessageDocument(
                    input.prepare_message(app, chat_id).await?,
                )
            }
        })
    }
}

impl<R: tauri::Runtime> MessageCleaner<R> for InputMessageContent {
    async fn cleanup(&self, app: &tauri::AppHandle<R>) {
        match self {
            InputMessageContent::Photo(input) => {
                input.cleanup(app).await;
            }
            _ => {}
        }
    }
}

impl<R: tauri::Runtime> MessagePreparer<tdlib::types::InputMessageText, R> for InputMessageText {
    async fn prepare_message(
        &self,
        _: &tauri::AppHandle<R>,
        _: i64,
    ) -> Result<tdlib::types::InputMessageText, String> {
        Ok(tdlib::types::InputMessageText {
            text: get_simple_formatted_text(&self.text),
            link_preview_options: None,
            clear_draft: true,
        })
    }
}

impl<R: tauri::Runtime> MessagePreparer<tdlib::types::InputMessageDocument, R>
    for InputMessageDocument
{
    async fn prepare_message(
        &self,
        _: &tauri::AppHandle<R>,
        _: i64,
    ) -> Result<tdlib::types::InputMessageDocument, String> {
        Ok(tdlib::types::InputMessageDocument {
            document: tdlib::types::InputDocument {
                document: tdlib::enums::InputFile::Local(tdlib::types::InputFileLocal {
                    path: self.path.clone(),
                }),
                thumbnail: None,
                disable_content_type_detection: true,
            },
            caption: self.caption.as_ref().map(|t| get_simple_formatted_text(t)),
        })
    }
}

impl<R: tauri::Runtime> MessagePreparer<tdlib::types::InputMessagePhoto, R> for InputMessagePhoto {
    async fn prepare_message(
        &self,
        app: &tauri::AppHandle<R>,
        _: i64,
    ) -> Result<tdlib::types::InputMessagePhoto, String> {
        let mut thumbnail: Option<tdlib::types::InputThumbnail> = None;
        match thumbnails::create_thumbnail(&app, &self.path) {
            Ok(tdlib::enums::InputThumbnail::InputThumbnail(t)) => {
                thumbnail = Some(t);
            }
            Err(e) => {
                log::warn!("Unable to generate thumbnail: {}", e.to_string());
            }
        }

        Ok(tdlib::types::InputMessagePhoto {
            photo: tdlib::types::InputPhoto {
                photo: tdlib::enums::InputFile::Local(tdlib::types::InputFileLocal {
                    path: self.path.clone(),
                }),
                thumbnail: thumbnail,
                video: None,
                added_sticker_file_ids: vec![],
                width: self.width,
                height: self.height,
            },
            caption: self.caption.as_ref().map(|t| get_simple_formatted_text(t)),
            show_caption_above_media: false,
            self_destruct_type: None,
            has_spoiler: false,
        })
    }
}

impl<R: tauri::Runtime> MessagePreparer<tdlib::types::InputMessageDocument, R>
    for InputMessagePgpText
{
    async fn prepare_message(
        &self,
        app: &tauri::AppHandle<R>,
        chat_id: i64,
    ) -> Result<tdlib::types::InputMessageDocument, String> {
        let target_path =
            crypto::pgp::get_pgp_file_path(&app, ".txt.pgp").map_err(|e| e.to_string())?;
        let keys =
            crypto::pgp::get_chat_encryption_keys(&app, chat_id).map_err(|e| e.to_string())?;
        crypto::pgp::encrypt_string_to_file(&keys, &self.text, &target_path)
            .map_err(|e| e.to_string())?;

        Ok(tdlib::types::InputMessageDocument {
            document: tdlib::types::InputDocument {
                document: tdlib::enums::InputFile::Local(tdlib::types::InputFileLocal {
                    path: target_path,
                }),
                thumbnail: None,
                disable_content_type_detection: true,
            },
            caption: None,
        })
    }
}

impl<R: tauri::Runtime> MessagePreparer<tdlib::types::InputMessageDocument, R>
    for InputMessagePgpFile
{
    async fn prepare_message(
        &self,
        app: &tauri::AppHandle<R>,
        chat_id: i64,
    ) -> Result<tdlib::types::InputMessageDocument, String> {
        let keys =
            crypto::pgp::get_chat_encryption_keys(&app, chat_id).map_err(|e| e.to_string())?;

        let file_name = Path::new(&self.path)
            .file_name()
            .ok_or_else(|| "Unable to extract file name")?
            .to_string_lossy()
            .into_owned();

        let cipher_file_name =
            crypto::pgp::encrypt_string_to_string(&keys, &file_name).map_err(|e| e.to_string())?;

        let mut ciphertexts: Vec<String> = vec![cipher_file_name];

        if let Some(caption) = &self.caption {
            let cipher_caption = crypto::pgp::encrypt_string_to_string(&keys, &caption)
                .map_err(|e| e.to_string())?;
            ciphertexts.push(cipher_caption);
        }

        let caption = serde_json::to_string(&ciphertexts).map_err(|e| e.to_string())?;

        let target_path =
            crypto::pgp::get_pgp_file_path(&app, ".pgp").map_err(|e| e.to_string())?;
        crypto::pgp::encrypt_file_to_file(&keys, &self.path, &target_path)
            .map_err(|e| e.to_string())?;

        Ok(tdlib::types::InputMessageDocument {
            document: tdlib::types::InputDocument {
                document: tdlib::enums::InputFile::Local(tdlib::types::InputFileLocal {
                    path: target_path,
                }),
                thumbnail: None,
                disable_content_type_detection: true,
            },
            caption: Some(get_simple_formatted_text(&caption)),
        })
    }
}

impl<R: tauri::Runtime> MessageCleaner<R> for InputMessagePhoto {
    async fn cleanup(&self, app: &tauri::AppHandle<R>) {
        thumbnails::remove_thumbnail(&app, &self.path);
    }
}

fn get_simple_formatted_text(text: &String) -> tdlib::types::FormattedText {
    tdlib::types::FormattedText {
        text: text.clone(),
        entities: vec![],
    }
}
