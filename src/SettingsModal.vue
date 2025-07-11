<script setup lang="ts">
import { store } from './store';
import Dropdown from './components/Dropdown.vue';
import { onMounted, ref } from 'vue';
import { getDefaultChatFolder, setDefaultChatFolder } from './settings/settings';
import { exportPublicKey, exportSecretKey, getMyKeyFingerprint } from './services/keys';
import { save } from '@tauri-apps/plugin-dialog';

function closeModal() {
  store.toggleSettingsModal();
}

const selectedId = ref(0);
const myKeyFingerprint = ref('');

function selectFolder(id: number) {
  selectedId.value = id;
}

onMounted(async () => {
  selectedId.value = await getDefaultChatFolder();
  myKeyFingerprint.value = await getMyKeyFingerprint();
});

async function saveSettings() {
  await setDefaultChatFolder(selectedId.value);
  closeModal();
}

async function openSaveSecretKeyDialog() {
  const targetPath = await save();
  if (targetPath === null) {
    return;
  }
  await exportSecretKey(targetPath);
}

async function openSavePublicKeyDialog() {
  const targetPath = await save();
  if (targetPath === null) {
    return;
  }
  await exportPublicKey(targetPath);
}
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.settingsModalActive }" id="settings-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">Settings</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3">
        <p class="menu-label">Default chat folder</p>
        <Dropdown :values="store.chatFolders.map(f => ({ id: f.id, label: f.name }))" :default-value="selectedId"
          @change="selectFolder" />

        <p class="menu-label mt-4">PGP Key</p>
        <p v-if="myKeyFingerprint !== ''" class="mt-1 mb-2"><code>{{ myKeyFingerprint }}</code></p>
        <button class="button is-link" @click="openSaveSecretKeyDialog">Export secret key</button>
        <button class="button is-primary ml-2" @click="openSavePublicKeyDialog">Export public key</button>
      </section>
      <footer class="modal-card-foot p-2">
        <div class="buttons">
          <button class="button is-link" @click="saveSettings">Save</button>
          <button class="button" @click="closeModal">Cancel</button>
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
