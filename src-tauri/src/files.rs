use std::{collections::BTreeMap, error::Error, path::Path};

use tauri::{
    ipc::RuntimeCapability,
    utils::acl::{
        capability::{Capability, CapabilityFile, PermissionEntry},
        Scopes, Value,
    },
    Manager,
};

use crate::{crypto::utils::generate_random_string, store};

/**
 * Tells Tauri that the opener plugin is allowed to open the file passed as parameter.
 */
pub fn allow_opening_file<R: tauri::Runtime>(app: &tauri::AppHandle<R>, path: &str) {
    log::debug!("Allowing opener plugin to access {}", path);
    if let Err(e) = app.add_capability(PathsCapability {
        paths: vec![format!("file://{}", path)],
    }) {
        log::error!("Unable to add file access capability: {}", e);
    }
}

/**
 * Tells Tauri that the opener plugin is allowed to open the paths passed as parameter.
 */
fn allow_opening_paths<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    paths: Vec<String>,
) -> Result<(), Box<dyn Error>> {
    app.add_capability(PathsCapability { paths: paths })?;
    Ok(())
}

fn get_allowed_paths<R: tauri::Runtime>(app: &tauri::AppHandle<R>) -> Vec<String> {
    let mut application_folder = store::get_application_folder(&app);
    if application_folder.ends_with("/") {
        application_folder.pop();
    }
    vec![
        String::from("tdlib/photos/*"),
        String::from("tdlib/voice/*"),
        String::from("tdlib/documents/*"),
        String::from("pgp/messages/*"),
        String::from("keys/*"),
    ]
    .iter()
    .map(|p| format!("file://{}/{}", application_folder, p))
    .collect()
}

pub fn set_allowed_files_paths<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<(), Box<dyn Error>> {
    allow_opening_paths(app, get_allowed_paths(app))
}

struct PathsCapability {
    paths: Vec<String>,
}

impl RuntimeCapability for PathsCapability {
    fn build(self) -> CapabilityFile {
        build_paths_capability(self.paths)
    }
}

fn build_paths_capability(paths: Vec<String>) -> CapabilityFile {
    let allowed_paths: Vec<Value> = paths
        .iter()
        .map(|path| {
            let mut map: BTreeMap<String, Value> = BTreeMap::new();
            map.insert("path".into(), Value::String(path.into()));
            return Value::Map(map);
        })
        .collect();

    CapabilityFile::Capability(Capability {
        identifier: generate_random_string(10),
        description: "".into(),
        remote: None,
        local: true,
        windows: vec!["main".into()],
        webviews: vec![],
        permissions: vec![PermissionEntry::ExtendedPermission {
            identifier: String::from("opener:allow-open-path").try_into().unwrap(),
            scope: Scopes {
                allow: Some(allowed_paths),
                deny: None,
            },
        }],
        platforms: None,
    })
}

pub fn is_file_accessible<R: tauri::Runtime>(app: &tauri::AppHandle<R>, file_path: &str) -> bool {
    let allowed_folders: Vec<String> = get_allowed_paths(app)
        .iter()
        .map(|p| p.replace("file://", "").replace("*", ""))
        .collect();
    for parent_folder in allowed_folders {
        if is_subpath(&Path::new(file_path), &Path::new(&parent_folder)) {
            return true;
        }
    }
    false
}

fn is_subpath(child: &Path, parent: &Path) -> bool {
    let mut child_iter = child.components();
    let mut parent_iter = parent.components();

    loop {
        match parent_iter.next() {
            None => return true, // parent fully matched
            Some(p) => match child_iter.next() {
                Some(c) if c == p => {}
                _ => return false,
            },
        }
    }
}
