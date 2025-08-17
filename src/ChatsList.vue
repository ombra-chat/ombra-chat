<script setup lang="ts">
import { computed } from 'vue';
import { store } from './store';
import { getChatPosition, selectChat } from './services/chats';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faLock } from '@fortawesome/free-solid-svg-icons';

const chats = computed(() => {
  const list = store.chatFoldersMap[store.selectedChatFolderId] || [];
  return list
    .map(id => store.chatsMap[id]).filter(c => c !== undefined)
    .sort((c1, c2) => getChatPosition(c1) < getChatPosition(c2) ? 1 : -1);
});
</script>

<template>
  <div class="menu">
    <ul class="menu-list">
      <li v-for="chat in chats" class="chat-row nowrap" :key="chat.id">
        <a href="#" :class="{ 'is-active': store.selectedChat?.id === chat.id }" class="nowrap"
          @click="() => selectChat(chat.id)">
          <span class="chat-title nowrap">
            <span class="mr-1" v-if="chat.type['@type'] === 'chatTypeSecret'">
              <FontAwesomeIcon :icon="faLock" />
            </span>
            {{ chat.title }}
          </span>
          <span class="unread-count" v-if="chat.unread_count > 0"
            :class="{ 'muted': chat.notification_settings.mute_for > 0 }">
            {{ chat.unread_count }}
          </span>
        </a>
      </li>
    </ul>
  </div>
</template>

<style>
.chat-row a {
  display: flex;
  flex-direction: row;
}

.chat-title {
  display: block;
  flex-grow: 1;
  margin: auto 0;
}

.unread-count {
  color: #fff;
  display: block;
  border-radius: 20px;
  padding: 2px;
  min-width: 24px;
  height: 24px;
  line-height: 24px;
  text-align: center;
  background-color: rgb(0, 204, 255);
  margin-right: -5px;
}

.unread-count.muted {
  background-color: rgb(187, 187, 187);
}
</style>
