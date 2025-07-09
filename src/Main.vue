<script setup lang="ts">
import { onMounted } from 'vue';
import ChatFolders from './ChatFolders.vue';
import { invoke } from '@tauri-apps/api/core';
import Sidebar from './Sidebar.vue';
import { store } from './store';

onMounted(async () => {
  let chatsLoaded = false;
  while (!chatsLoaded) {
    try {
      await invoke('load_chats');
    } catch (err) {
      chatsLoaded = true;
    }
  }
});
</script>

<template>
  <div id="main-wrapper">
    <Sidebar />
    <div id="top-bar">
      <a class="navbar-burger" role="button" aria-label="menu" aria-expanded="false" @click="store.toggleSidebar">
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
      </a>
    </div>
    <ChatFolders />
    <div id="main-container">
      <div id="chat-lists-container">
        TODO: chat lists
      </div>
      <div id="current-chat-container">
        TODO: current chat
      </div>
    </div>
  </div>
</template>

<style>
#main-wrapper {
  position: absolute;
  top: 0;
  right: 0;
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
</style>