<script setup lang="ts">
import { computed, nextTick, ref } from 'vue';
import { deleteMessage, forwardMessage, getChatPosition, selectChat } from './services/chats';
import { store } from './store';

const selectingChat = ref(false);
const forwarding = ref(false);
const forwardingFilter = ref('');
const sendCopy = ref(false);

function closeModal() {
  selectingChat.value = false;
  forwardingFilter.value = '';
  forwarding.value = false;
  sendCopy.value = false;
  store.selectedMessage = null;
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

const chats = computed(() => {
  if (!selectingChat) {
    return [];
  }
  const list = store.chatFoldersMap[forwardingFilter.value.trim() === '' ? store.selectedChatFolderId : 0] || [];
  return list
    .map(id => store.chatsMap[id]).filter(c => c !== undefined)
    .filter(c => forwardingFilter.value.trim() === '' ? true : c.title.toLowerCase().includes(forwardingFilter.value.trim()))
    .sort((c1, c2) => getChatPosition(c1) < getChatPosition(c2) ? -1 : 1);
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
        <input type="text" class="input mb-3" v-model="forwardingFilter" id="forwarding-filter" />
        <div class="menu" id="forward-to-chat-selector">
          <ul class="menu-list">
            <li v-for="chat in chats" class="chat-row nowrap" :key="chat.id">
              <a href="#" class="nowrap" @click="() => forwardMsg(chat.id)">
                <span class="chat-title">
                  {{ chat.title }}
                </span>
              </a>
            </li>
          </ul>
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
        <button class="button is-danger" @click="deleteMsg">Delete message</button>
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
  border-top: 1px #eee solid;
}
</style>
