use crate::store;
use image::ImageReader;
use sha2::{Digest, Sha256};
use std::{
    error::Error,
    fs::{self, File},
    io::{self, Cursor},
    path::{Path, PathBuf},
};
use tdlib::enums::{InputFile, InputThumbnail};

static THUMBNAIL_SIZE: u32 = 320;

pub fn create_thumbnail<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    path: &str,
) -> Result<InputThumbnail, Box<dyn Error>> {
    let img_path = Path::new(path);
    let image = ImageReader::open(img_path)?.decode()?;
    let image = image.resize(
        THUMBNAIL_SIZE,
        THUMBNAIL_SIZE,
        image::imageops::FilterType::Lanczos3,
    );

    let mut bytes: Vec<u8> = Vec::new();
    image.write_to(&mut Cursor::new(&mut bytes), image::ImageFormat::Png)?;

    let thumb_file_path = get_thumbnail_file_name(app, path)?;
    let mut thumb_file = File::create(thumb_file_path.clone())?;
    let mut buf = Cursor::new(bytes);
    io::copy(&mut buf, &mut thumb_file)?;

    let input_file = InputFile::Local(tdlib::types::InputFileLocal {
        path: thumb_file_path,
    });

    Ok(InputThumbnail::InputThumbnail(
        tdlib::types::InputThumbnail {
            thumbnail: input_file,
            width: THUMBNAIL_SIZE as i32,
            height: THUMBNAIL_SIZE as i32,
        },
    ))
}

pub fn remove_thumbnail<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    path: &str,
) -> Result<(), Box<dyn Error>> {
    let thumb_file_path = get_thumbnail_file_name(app, path)?;
    fs::remove_file(thumb_file_path)?;
    Ok(())
}

fn get_thumbnail_file_name<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
    path: &str,
) -> Result<String, Box<dyn Error>> {
    let thumb_dir = get_thumbnails_directory(app)?;
    let mut thumb_file_path = PathBuf::from(thumb_dir);
    let thumb_file_name = format!("{}.png", hex::encode(Sha256::digest(&path.as_bytes())));
    thumb_file_path.push(thumb_file_name);
    Ok(thumb_file_path.to_string_lossy().to_string())
}

fn get_thumbnails_directory<R: tauri::Runtime>(
    app: &tauri::AppHandle<R>,
) -> Result<String, Box<dyn Error>> {
    let app_folder = store::get_application_folder(app);
    let mut thumbnails_directory = PathBuf::from(app_folder);
    thumbnails_directory.push("thumbnails");
    fs::create_dir_all(thumbnails_directory.clone())?;
    Ok(thumbnails_directory.to_string_lossy().to_string())
}
