<script setup lang="ts">
import { store } from './store';
import Dropdown from './components/Dropdown.vue';
import { onMounted, ref, watch } from 'vue';
import { getDefaultChatFolder, setDefaultChatFolder, setTheme } from './settings/settings';
import { exportPublicKey, exportSecretKey, getMyKeyFingerprint } from './services/pgp';
import { save } from '@tauri-apps/plugin-dialog';
import { getCurrentWindow, Theme } from '@tauri-apps/api/window';

const selectedId = ref(0);
const myKeyFingerprint = ref('');
const theme = ref<Theme>('light');

function closeModal() {
  store.toggleSettingsModal();
}

function selectFolder(id: number) {
  selectedId.value = id;
}

onMounted(async () => {
  selectedId.value = await getDefaultChatFolder();
  myKeyFingerprint.value = await getMyKeyFingerprint();
  theme.value = await getCurrentWindow().theme() || 'light';
});

async function saveSettings() {
  await setDefaultChatFolder(selectedId.value);
  await setTheme(theme.value);
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

watch(
  () => theme.value,
  async (newValue) => {
    await getCurrentWindow().setTheme(newValue);
  }
);
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

        <p class="menu-label mt-4">Theme</p>
        <div class="control">
          <label class="radio mr-2">
            <input type="radio" value="light" v-model="theme" name="theme-selector" />
            Light
          </label>
          <label class="radio">
            <input type="radio" value="dark" v-model="theme" name="theme-selector" />
            Dark
          </label>
        </div>
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
