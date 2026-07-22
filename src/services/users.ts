import { listen } from "@tauri-apps/api/event";
import {  User } from "../model";
import { store } from "../store";

export async function handleUsersUpdates() {
  return [
    await listen<User>('update-user', (event) => {
      store.updateUser(event.payload);
    }),
  ]
}
