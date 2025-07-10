<script setup lang="ts">
import MessageBubble from './messages/MessageBubble.vue';
import { loadPreviousMessages } from './services/chats';
import { store } from './store';

async function chatContentScrolled(event: Event) {
  const element = event.target as HTMLElement;
  if (element.scrollTop === 0) {
    console.log('bottom!')
  } else if (element.scrollHeight - element.getBoundingClientRect().height + element.scrollTop === 0) {
    await loadPreviousMessages();
  }
}
</script>

<template>
  <div v-if="store.selectedChat !== null" id="chat-page">
    <div id="chat-header" class="pb-1 pb-2">
      <div id="chat-title" class="has-text-link">
        {{ store.selectedChat.title }}
      </div>
    </div>
    <div id="chat-content" class="p-1" @scroll="chatContentScrolled">
      <MessageBubble :message="message" v-for="message in store.currentMessages" />
    </div>
    <div id="send-message-box">
      <input type="text" class="input" id="new-message-text" />
      <button class="button is-link">Send</button>
    </div>
  </div>
</template>

<style>
#chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

#chat-header {
  display: flex;
  flex-direction: row;
  border-bottom: 1px #eee solid;
}

#chat-title {
  flex-grow: 1;
  text-align: center;
}

#chat-content {
  flex-grow: 1;
  display: flex;
  justify-content: flex-end;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
}

#send-message-box {
  display: flex;
  flex-direction: row;
}

#new-message-text {
  flex-grow: 1;
}
</style>