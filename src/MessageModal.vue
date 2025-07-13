<script setup lang="ts">
import { deleteMessage } from './services/chats';
import { store } from './store';

function closeModal() {
  store.selectedMessage = null;
  store.toggleMessageModal();
}

async function deleteMsg() {
  const selectedMessage = store.selectedMessage;
  if (selectedMessage === null) {
    return;
  }
  await deleteMessage(selectedMessage.chat_id, selectedMessage.id);
  closeModal();
}
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.messageModalActive }" id="message-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">Message</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3">
        <button class="button is-danger" @click="deleteMsg">Delete message</button>
      </section>
      <footer class="modal-card-foot p-2">
      </footer>
    </div>
  </div>
</template>
