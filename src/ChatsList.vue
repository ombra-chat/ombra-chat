<script setup lang="ts">
import { computed } from 'vue';
import { store } from './store';
import { selectChat } from './services/chats';

const chats = computed(() => {
  const list = store.chatFoldersMap[store.selectedChatFolderId] || [];
  return list.map(id => store.chatsMap[id]).filter(c => c !== undefined);
});
</script>

<template>
  <div class="menu">
    <ul class="menu-list">
      <li v-for="chat in chats" class="chat-row nowrap">
        <a href="#" :class="{ 'is-active': store.selectedChat?.id === chat.id }" class="nowrap"
          @click="() => selectChat(chat.id)">
          {{ chat.title }}
        </a>
      </li>
    </ul>
  </div>
</template>
