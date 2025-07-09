<script setup lang="ts">
import { store } from './store'
import Dropdown from './components/Dropdown.vue';

function selectFolder(id: number) {
  store.selectedChatFolderId = id;
}
</script>

<template>
  <Dropdown :values="store.chatFolders.map(f => ({ id: f.id, label: f.name }))" :default-value="store.selectedChatFolderId"
    @change="selectFolder" />
</template>
