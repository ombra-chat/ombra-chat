<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { deleteMessage, forwardMessage, selectChat } from './services/chats';
import { store } from './store';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faAngleDown, faAngleUp, faLock, faWarning } from '@fortawesome/free-solid-svg-icons';
import { addMessageReaction } from './services/effects';
import { getChatKey } from './services/pgp';

const selectingChat = ref(false);
const forwarding = ref(false);
const forwardingFilter = ref('');
const sendCopy = ref(false);
const reactionCardCollapsed = ref(true);
const showPgpWarningFor = ref<number | null>(null); // chat id

function closeModal() {
  selectingChat.value = false;
  forwardingFilter.value = '';
  forwarding.value = false;
  sendCopy.value = false;
  reactionCardCollapsed.value = true;
  store.selectedMessage = null;
  showPgpWarningFor.value = null;
  store.toggleMessageModal();
}

async function openChatSelection() {
  selectingChat.value = true;
  await nextTick(() => {
    const filterInput = document.getElementById('forwarding-filter');
    filterInput?.focus();
  });
}

async function forwardMsg(chatId: number) {
  const selectedMessage = store.selectedMessage;
  if (selectedMessage === null) {
    return;
  }
  if (forwarding.value) {
    return;
  }
  forwarding.value = true;
  const targetChatKey = await getChatKey(chatId);
  if (targetChatKey) {
    showPgpWarningFor.value = chatId;
    return;
  }
  await forwardMessage(selectedMessage, chatId, sendCopy.value);
  await selectChat(chatId);
  closeModal();
}

async function confirmForwardUnencrypted() {
  const selectedMessage = store.selectedMessage;
  const chatId = showPgpWarningFor.value;
  if (selectedMessage === null || chatId === null) {
    return;
  }
  await forwardMessage(selectedMessage, chatId, sendCopy.value);
  await selectChat(chatId);
  closeModal();
}

async function deleteMsg() {
  const selectedMessage = store.selectedMessage;
  if (selectedMessage === null) {
    return;
  }
  await deleteMessage(selectedMessage.chat_id, selectedMessage.id);
  closeModal();
}

function getMessageSelectedText(): string | null {
  const element = document.querySelector(`[data-message-id="${store.selectedMessage?.id}"]`);
  if (!element) {
    return null;
  }
  const selection = window.getSelection();
  if (selection === null || selection.rangeCount === 0) {
    return null;
  }
  const range = selection.getRangeAt(0);
  if (!element.contains(range.commonAncestorContainer)) {
    return null;
  }
  return range.toString();
}

function setReplyToMessage() {
  store.replyToMessage = store.selectedMessage;
  store.replyToQuote = getMessageSelectedText();
  closeModal();
}

async function addReaction(emoji: string) {
  await addMessageReaction(store.selectedMessage!, emoji);
  closeModal();
}

const chats = computed(() => {
  if (!selectingChat) {
    return [];
  }
  const list = store.chatFoldersMap[forwardingFilter.value.trim() === '' ? store.selectedChatFolderId : 0] || [];
  return list
    .map(id => store.chatsMap[id]).filter(c => c !== undefined)
    .filter(c => c.can_send)
    .filter(c => forwardingFilter.value.trim() === '' ? true : c.title.toLowerCase().includes(forwardingFilter.value.trim()))
    .sort((c1, c2) => c1.pos < c2.pos ? -1 : 1);
});

const deleteEnabled = computed(() => {
  const chat = store.selectedChat;
  if (chat === null) {
    return false;
  }
  return chat.can_delete_for_all || chat.can_delete_for_self;
});

const replyToEnabled = computed(() => {
  const chat = store.selectedChat;
  if (chat === null) {
    return false;
  }
  return chat.can_send;
});

const reactions = computed<Record<string, string>>(() => {
  if (!store.selectedChat) {
    return {};
  }
  const availableReactions = store.selectedChat.reactions;
  if (availableReactions === 'All') {
    return store.allReactions;
  } else {
    return Object.fromEntries(Object.entries(store.allReactions).filter(([e, _]) =>
      (store.selectedChat!.reactions as string[]).includes(e)
    ));
  }
});
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.messageModalActive }" id="message-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">Message</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3" v-if="selectingChat" id="forward-body">
        <div v-if="showPgpWarningFor">
          <div class="message is-warning my-3">
            <div class="message-body">
              <FontAwesomeIcon :icon="faWarning" />
              <strong>Warning</strong>: forwarded messages will be sent in clear even in PGP encrypted chats.
              Are you sure that you want to proceed?
            </div>
          </div>
          <div class="my-3">
            <button class="button is-primary mr-3" @click="confirmForwardUnencrypted">
              Yes, forward anyway
            </button>
            <button class="button is-danger" @click="closeModal">
              No, cancel
            </button>
          </div>
        </div>
        <div v-else>
          <input type="text" class="input mb-3" v-model="forwardingFilter" id="forwarding-filter" />
          <div class="menu" id="forward-to-chat-selector">
            <ul class="menu-list">
              <li v-for="chat in chats" class="chat-row nowrap" :key="chat.id">
                <a href="#" class="nowrap" @click="() => forwardMsg(chat.id)">
                  <span class="chat-title">
                    <span class="mr-1" v-if="chat.secret">
                      <FontAwesomeIcon :icon="faLock" />
                    </span>
                    {{ chat.title }}
                  </span>
                </a>
              </li>
            </ul>
          </div>
        </div>
        <div class="pt-2" id="send-copy-wrapper">
          <label class="checkbox">
            <input type="checkbox" v-model="sendCopy" class="mr-2" />
            <em>Send copy (do not reference to the original sender)</em>
          </label>
        </div>
      </section>
      <section class="modal-card-body p-3" v-else>
        <button class="button is-link mb-3" @click="openChatSelection">Forward message</button><br />

        <button class="button is-link mb-3" @click="setReplyToMessage" v-if="replyToEnabled">
          Reply to message
        </button><br />

        <div class="card" v-if="Object.entries(reactions).length > 0">
          <header class="card-header" @click="() => (reactionCardCollapsed = !reactionCardCollapsed)"
            id="reactions-card-header">
            <p class="card-header-title">Add reaction</p>
            <span class="card-header-icon">
              <span class="icon">
                <FontAwesomeIcon :icon="faAngleDown" v-if="reactionCardCollapsed" />
                <FontAwesomeIcon :icon="faAngleUp" v-else />
              </span>
            </span>
          </header>
          <div class="card-content p-3" v-if="!reactionCardCollapsed">
            <div class="content">
              <button class="button" type="button" v-for="[emoji, image] in Object.entries(reactions)"
                @click="() => addReaction(emoji)">
                <img :src="image" width="20" height="20" />
              </button>
            </div>
          </div>
        </div>

        <hr v-if="deleteEnabled" />
        <button class="button is-danger" @click="deleteMsg" v-if="deleteEnabled">
          Delete message
        </button>
      </section>
      <footer class="modal-card-foot p-2">
      </footer>
    </div>
  </div>
</template>

<style>
#forward-body {
  display: flex;
  flex-direction: column;
}

#forward-to-chat-selector {
  flex-grow: 1;
  overflow-y: auto;
  overflow-x: hidden;
}

#send-copy-wrapper {
  border-top: 1px var(--bulma-border-weak) solid;
}

#reactions-card-header {
  cursor: pointer;
}
</style>
