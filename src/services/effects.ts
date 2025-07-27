import { invoke } from "@tauri-apps/api/core";
import { MessageEffect, UpdateAvailableMessageEffects, File, ReactionType, Message } from "../model";
import { listen } from "@tauri-apps/api/event";
import { store } from "../store";
import { downloadFile, getPhoto } from "./files";

export async function handleEffectsUpdates() {
  return [
    await listen<UpdateAvailableMessageEffects>('update-available-message-effects', async (event) => {
      const update = event.payload;
      for (const reactionId of update.reaction_effect_ids) {
        const effect = await getMessageEffect(reactionId);
        if (effect.static_icon) {
          const data = await loadReactionImage(effect.static_icon.sticker);
          if (data) {
            store.allReactions[effect.emoji] = data;
          }
        }
      }
    }),
  ]
}

async function getMessageEffect(effectId: number) {
  return await invoke<MessageEffect>('get_message_effect', { effectId });
}

async function loadReactionImage(reaction: File): Promise<string | null> {
  if (reaction.local.is_downloading_completed) {
    return await getPhoto(reaction.local.path);
  }
  const file = await downloadFile(reaction.id);
  if (file === null) {
    return null;
  }
  return await loadReactionImage(file);
}

export async function addMessageReaction(message: Message, emoji: string) {
  const reactionType: ReactionType = { '@type': 'reactionTypeEmoji', emoji };
  await invoke('add_message_reaction', { chatId: message.chat_id, messageId: message.id, reactionType });
}

export async function removeMessageReaction(message: Message, emoji: string) {
  const reactionType: ReactionType = { '@type': 'reactionTypeEmoji', emoji };
  await invoke('remove_message_reaction', { chatId: message.chat_id, messageId: message.id, reactionType });
}
