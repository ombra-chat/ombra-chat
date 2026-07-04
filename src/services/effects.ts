import { convertFileSrc, invoke } from "@tauri-apps/api/core";
import { ReactionType, Message } from "../model";
import { listen } from "@tauri-apps/api/event";
import { store } from "../store";

export async function handleEffectsUpdates() {
  return [
    await listen<Record<string, string>>('set-reactions', async (event) => {
      store.allReactions = Object.fromEntries(
        Object.entries(event.payload).map(([k, v]) => [k, convertFileSrc(v)])
      );
    })
  ]
}

export async function addMessageReaction(message: Message, emoji: string) {
  const reactionType: ReactionType = { '@type': 'reactionTypeEmoji', emoji };
  await invoke('add_message_reaction', { chatId: message.chat_id, messageId: message.id, reactionType });
}

export async function removeMessageReaction(message: Message, emoji: string) {
  const reactionType: ReactionType = { '@type': 'reactionTypeEmoji', emoji };
  await invoke('remove_message_reaction', { chatId: message.chat_id, messageId: message.id, reactionType });
}
