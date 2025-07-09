<script setup lang="ts">
import { ref } from 'vue';
import { store } from './store'

const active = ref(false);

function toggleDropdown() {
  active.value = !active.value;
}

function selectFolder(id: number) {
  store.selectedChatFolderId = id;
  toggleDropdown();
}

</script>

<template>
  <div class="dropdown" :class="{ 'is-active': active }">
    <div class="dropdown-trigger">
      <button class="button" aria-haspopup="true" aria-controls="chat-folders-dropdown" @click="toggleDropdown">
        <span>{{store.chatFolders.find(f => f.id === store.selectedChatFolderId)?.name}}</span>
        <span class="icon is-small">
          <i class="fas fa-angle-down" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <div class="dropdown-menu" id="chat-folders-dropdown" role="menu">
      <div class="dropdown-content">
        <a href="#" class="dropdown-item" v-for="chatFolder in store.chatFolders" :id="`folder-${chatFolder.id}`"
          @click="() => selectFolder(chatFolder.id)">
          {{ chatFolder.name }}
        </a>
      </div>
    </div>
  </div>
</template>
