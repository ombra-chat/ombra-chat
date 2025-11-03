# Changelog

## 0.3.2

* added double checkmarks for read messages
* handled secret chats state
* fixed issue with "reply to" messages

## 0.3.1

* basic support for emoji message
* fixed bug caused by invalid "reply to" message
* added about modal
* added setting to display pictures using app window rather than system default app
* updated dependencies

## 0.3.0

⚠️ Breaking changes: you need to regenerate your keys. It is also better to delete the `~/.ombra-chat` folder and repeat the login.

* app was completely rewritten in Tauri (Rust + Vue.js), due to JavaFX inability to handle Wayland and touch events
* simplified key generation and key sharing
* supported dark theme
* supported chat deletion
* updated tdlib to 1.8.52

## 0.2.1

* fixed messages loading issue
* changed app icon and title when there are unread messages
* updated tdlib to 1.8.50

## 0.2.0

* enabled uploading of multiple files
* handled paste of files using Ctrl+V
* enabled drag and drop file upload
* implemented photo messages with thumbnail creation
* basic reaction support
* supported message forwarding
* supported reply to
* fixed several issues

## 0.1.0

Initial release.

* displaying chat folders
* text messages
* file upload and download
* viewing photos
* deleting messages
* secret chats
* tdlib encryption
* GPG support
* automatic selection of default folder
