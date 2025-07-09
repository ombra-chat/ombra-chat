<script setup lang="ts">
import { computed } from 'vue';
import { store } from './store';

const chats = computed(() => {
  const list = store.chatFoldersMap[store.selectedChatFolderId] || [];
  return list.map(id => store.chatsMap[id]).filter(c => c !== undefined);
});

</script>

<template>
  <div class="menu">
    <ul class="menu-list">
      <li v-for="chat in chats" class="chat-row">
        <a href="#">{{ chat.title }}</a>
      </li>
    </ul>
  </div>
</template>

<style>
.chat-row {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>