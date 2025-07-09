<script setup lang="ts">
import { store } from './store';
import Dropdown from './components/Dropdown.vue';
import { onMounted, ref } from 'vue';
import { getDefaultChatFolder, setDefaultChatFolder } from './settings/settings';

function closeModal() {
  store.toggleSettingsModal();
}

const selectedId = ref(0);

function selectFolder(id: number) {
  selectedId.value = id;
}

onMounted(async () => {
  selectedId.value = await getDefaultChatFolder();
});

async function save() {
  await setDefaultChatFolder(selectedId.value);
  closeModal();
}
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.settingsModalActive }" id="settings-modal">
    <div class="modal-background"@click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">Settings</p>
        <button class="delete" aria-label="close"@click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3">
        <p class="menu-label">Default chat folder</p>
        <Dropdown :values="store.chatFolders.map(f => ({ id: f.id, label: f.name }))" :default-value="selectedId"
          @change="selectFolder" />
      </section>
      <footer class="modal-card-foot p-2">
        <div class="buttons">
          <button class="button is-link" @click="save">Save</button>
          <button class="button"@click="closeModal">Cancel</button>
        </div>
      </footer>
    </div>
  </div>
</template>

<style>
#settings-modal .modal-card {
  height: 95%;
}
</style>
