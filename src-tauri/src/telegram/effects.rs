use std::collections::HashMap;
use crate::{emit, state};
use tdlib::enums::{MessageEffect::MessageEffect, Update};

pub async fn handle_effects_update<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    update: &Update,
) -> bool {
    match update {
        Update::AvailableMessageEffects(update) => {
            let mut reactions: HashMap<String, String> = HashMap::new();

            for effect_id in &update.reaction_effect_ids {
                if let Ok(MessageEffect(effect)) = tdlib::functions::get_message_effect(
                    effect_id.clone(),
                    state::get_client_id(app),
                )
                .await
                {
                    if let Some(icon) = effect.static_icon {
                        if let Some(path) = load_reaction_image(app, icon.sticker).await {
                            reactions.insert(effect.emoji, path);
                        }
                    }
                }
            }

            emit(app, "set-reactions", reactions);
            return true;
        }
        _ => {
            return false;
        }
    }
}

async fn load_reaction_image<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    sticker: tdlib::types::File,
) -> Option<String> {
    if sticker.local.is_downloading_completed {
        return Some(sticker.local.path);
    }
    if let Ok(tdlib::enums::File::File(downloaded)) =
        tdlib::functions::download_file(sticker.id, 1, 0, 0, false, state::get_client_id(&app))
            .await
    {
        return Some(downloaded.local.path);
    }
    None
}
