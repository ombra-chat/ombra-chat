import { listen } from "@tauri-apps/api/event";
import { UpdateUser } from "../model";
import { store } from "../store";

export async function handleUsersUpdates() {
  return [
    await listen<UpdateUser>('update-user', (event) => {
      const { user } = event.payload;
      store.updateUser(user);
    }),
  ]
}

export function getUserDisplayText(userId: number): string {
  const user = store.getUser(userId);
  if (!user) {
    return '';
  }
  if (user.first_name) {
    if (user.last_name) {
      return `${user.first_name} ${user.last_name}`;
    }
    return user.first_name;
  }
  const { usernames } = user;
  if (usernames === null) {
    return '';
  }
  if (usernames.editable_username) {
    return usernames.editable_username;
  }
  if (usernames.active_usernames.length > 0) {
    return usernames.active_usernames[0];
  }
  return '';
}
