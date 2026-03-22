use crate::state;

fn get_chat_list_from_folder_id(
    lists: &tdlib::enums::ChatLists,
    folder_id: i32,
) -> Option<tdlib::enums::ChatList> {
    let chat_lists = match lists {
        tdlib::enums::ChatLists::ChatLists(chat_lists) => chat_lists,
    };

    for chat_list in &chat_lists.chat_lists {
        if let tdlib::enums::ChatList::Folder(folder) = chat_list {
            if folder.chat_folder_id == folder_id {
                return Some(chat_list.clone());
            }
        }
    }

    None
}

#[tauri::command]
pub async fn create_chat_folder<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    title: String,
    included_chat_ids: Vec<i64>
) -> Result<(), String> {
    let client_id = state::get_client_id(&app);

    let name = tdlib::types::ChatFolderName {
        text: tdlib::types::FormattedText {
            text: title,
            entities: vec![],
        },
        animate_custom_emoji: false,
    };

    let folder = tdlib::types::ChatFolder {
        name,
        icon: None,
        color_id: -1,
        is_shareable: false,
        pinned_chat_ids: vec![],
        included_chat_ids,
        excluded_chat_ids: vec![],
        exclude_muted: false,
        exclude_read: false,
        exclude_archived: false,
        include_contacts: false,
        include_non_contacts: false,
        include_bots: false,
        include_groups: false,
        include_channels: false,
    };

    tdlib::functions::create_chat_folder(folder, client_id)
        .await
        .map_err(|e| e.message)?;

    Ok(())
}

#[tauri::command]
pub async fn delete_chat_folder<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    folder_id: i32,
) -> Result<(), String> {
    let client_id = state::get_client_id(&app);

    tdlib::functions::delete_chat_folder(folder_id, vec![], client_id)
        .await
        .map_err(|e| e.message)?;

    Ok(())
}

#[tauri::command]
pub async fn rename_chat_folder<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    folder_id: i32,
    new_title: String,
) -> Result<(), String> {
    let client_id = state::get_client_id(&app);

    let folder = tdlib::functions::get_chat_folder(folder_id, client_id)
        .await
        .map_err(|e| e.message)?;

    let mut folder = match folder {
        tdlib::enums::ChatFolder::ChatFolder(f) => f,
    };

    folder.name.text.text = new_title;

    tdlib::functions::edit_chat_folder(folder_id, folder, client_id)
        .await
        .map_err(|e| e.message)?;

    Ok(())
}

#[tauri::command]
pub async fn add_chat_to_folder<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    folder_id: i32,
) -> Result<(), String> {
    let client_id = state::get_client_id(&app);

    let lists = tdlib::functions::get_chat_lists_to_add_chat(chat_id, client_id)
        .await
        .map_err(|e| e.message)?;

    if let Some(chat_list) = get_chat_list_from_folder_id(&lists, folder_id) {
        tdlib::functions::add_chat_to_list(chat_id, chat_list, client_id)
            .await
            .map_err(|e| e.message)
    } else {
        Err(format!("Chat folder not found"))
    }
}

#[tauri::command]
pub async fn remove_chat_from_folder<R: tauri::Runtime>(
    app: tauri::AppHandle<R>,
    chat_id: i64,
    folder_id: i32,
) -> Result<(), String> {
    let client_id = state::get_client_id(&app);

    let folder = tdlib::functions::get_chat_folder(folder_id, client_id)
        .await
        .map_err(|e| e.message)?;

    let mut folder = match folder {
        tdlib::enums::ChatFolder::ChatFolder(f) => f,
    };

    if let Some(pos) = folder
        .included_chat_ids
        .iter()
        .position(|&id| id == chat_id)
    {
        folder.included_chat_ids.remove(pos);
    }

    tdlib::functions::edit_chat_folder(folder_id, folder, client_id)
        .await
        .map_err(|e| e.message)?;

    Ok(())
}
