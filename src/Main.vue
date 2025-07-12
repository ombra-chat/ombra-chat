<script setup lang="ts">
import { onMounted } from 'vue';
import ChatFolders from './ChatFolders.vue';
import Sidebar from './Sidebar.vue';
import { store } from './store';
import ChatsList from './ChatsList.vue';
import SettingsModal from './SettingsModal.vue';
import ChatPage from './ChatPage.vue';
import { loadChats } from './services/chats';

onMounted(async () => {
  await loadChats();
});
</script>

<template>
  <div id="main-wrapper">
    <Sidebar />
    <div id="top-bar" class="mb-2">
      <a class="navbar-burger" role="button" aria-label="menu" aria-expanded="false" @click="store.toggleSidebar">
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
      </a>
      <ChatFolders />
    </div>
    <div id="main-container">
      <div id="chat-lists-container">
        <ChatsList />
      </div>
      <div id="current-chat-container">
        <ChatPage />
      </div>
    </div>
    <SettingsModal />
  </div>
</template>

<style>
#top-bar {
  border-bottom: 1px #eee solid;
}

#main-wrapper {
  position: absolute;
  top: 0;
  /* this ugly margin is needed to prevent WebKit scrollbar issue... */
  right: 20px;
  left: 0;
  bottom: 0;
  display: flex;
  flex-direction: column;
}

#main-container {
  flex: 1;
  display: flex;
  position: relative;
}

#chat-lists-container {
  width: 30%;
  position: absolute;
  top: 0;
  bottom: 0;
  left: 0;
  border-right: 1px #eee solid;
  overflow-y: auto;
  overflow-x: hidden;
}

#current-chat-container {
  width: 70%;
  position: absolute;
  top: 0;
  right: 0;
  bottom: 0;
}

@media screen and (max-width: 400px) {
  #chat-lists-container {
    width: 100%;
  }

  #current-chat-container {
    display: none;
  }
}

.navbar-burger {
  /* prevent hiding the button on large screens */
  display: inline-flex !important;
}

.nowrap {
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}
</style>