<script setup lang="ts">
import { ref } from 'vue';
import MessageBubble from './messages/MessageBubble.vue';
import { loadPreviousMessages, sendMessage } from './services/chats';
import { store } from './store';
import { FormattedText, InputMessageContent, InputMessageText } from './model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faPaperPlane, faGear, faPaperclip } from '@fortawesome/free-solid-svg-icons';

const newMessageText = ref('');

async function chatContentScrolled(event: Event) {
  const element = event.target as HTMLElement;
  if (element.scrollHeight - element.getBoundingClientRect().height + element.scrollTop === 0) {
    await loadPreviousMessages();
  }
}

async function send() {
  const contents = getInputMessageContents();
  const chat = store.selectedChat;
  if (contents.length === 0 || !chat) {
    return;
  }
  for (const content of contents) {
    await sendMessage(chat.id, null, content);
    const chatContent = document.getElementById('chat-content');
    if (chatContent) {
      chatContent.scrollTo(0, chatContent.scrollHeight);
    }
  }
  newMessageText.value = '';
}

function getInputMessageContents(): InputMessageContent[] {
  const formattedText: FormattedText = { text: newMessageText.value, entities: [] };
  const message: InputMessageText = { '@type': 'inputMessageText', text: formattedText, clear_draft: true };
  return [message];
}
</script>

<template>
  <div v-if="store.selectedChat !== null" id="chat-page">
    <div id="chat-header" class="pb-1 pb-2">
      <div id="chat-title" class="has-text-link mt-1 has-text-weight-bold">
        {{ store.selectedChat.title }}
      </div>
      <button type="button" class="button is-text" aria-label="Settings">
        <FontAwesomeIcon :icon="faGear" />
      </button>
    </div>
    <div id="chat-content" class="p-1 has-background-link-light" @scroll="chatContentScrolled">
      <MessageBubble :message="message" v-for="message in store.currentMessages" />
    </div>
    <div id="send-message-box">
      <input type="text" class="input" id="new-message-text" v-model="newMessageText" @keyup.enter="send" />
      <button type="button" class="button is-primary" aria-label="Attach file">
        <FontAwesomeIcon :icon="faPaperclip" />
      </button>
      <button type="button" class="button is-link" @click="send" aria-label="Send">
        <FontAwesomeIcon :icon="faPaperPlane" />
      </button>
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