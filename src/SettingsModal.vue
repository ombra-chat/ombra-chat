<script setup lang="ts">
import { store } from './store';
import Dropdown from './components/Dropdown.vue';
import { onMounted, ref, watch } from 'vue';
import { getDefaultChatFolder, getImageViewer, setDefaultChatFolder, setImageViewer, setTheme } from './settings/settings';
import { exportPublicKey, exportSecretKey, getMyKeyFingerprint } from './services/pgp';
import { save } from '@tauri-apps/plugin-dialog';
import { getCurrentWindow, Theme } from '@tauri-apps/api/window';
import { PublicKeyFingerprints } from './model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faLock, faKey } from '@fortawesome/free-solid-svg-icons';

const selectedId = ref(0);
const myKeyFingerprint = ref<PublicKeyFingerprints | null>(null);
const theme = ref<Theme>('light');
const keyError = ref('');
const imageViewer = ref<'system' | 'app'>('system');

function closeModal() {
  store.toggleSettingsModal();
}

function selectFolder(id: number) {
  selectedId.value = id;
}

onMounted(async () => {
  selectedId.value = await getDefaultChatFolder();
  try {
    myKeyFingerprint.value = await getMyKeyFingerprint();
  } catch (err) {
    if (err instanceof Error) {
      keyError.value = err.message;
    }
  }
  theme.value = await getCurrentWindow().theme() || 'light';
  imageViewer.value = await getImageViewer();
});

async function saveSettings() {
  await setDefaultChatFolder(selectedId.value);
  await setTheme(theme.value);
  await setImageViewer(imageViewer.value);
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
        <div v-if="myKeyFingerprint" class="mb-2">
          <div>
            <FontAwesomeIcon :icon="faKey" class="mr-1" />Primary: <code>{{ myKeyFingerprint.primary }}</code>
          </div>
          <div v-for="enc_key in myKeyFingerprint.encryption_keys">
            <FontAwesomeIcon :icon="faLock" class="mr-1" />Encryption: <code>{{ enc_key }}</code>
          </div>
        </div>
        <div v-if="keyError" class="message is-danger mb-2">
          <div class="message-body">
            {{ keyError }}
          </div>
        </div>

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

        <p class="menu-label mt-4">Image viewer</p>
        <div class="control">
          <label class="radio mr-2">
            <input type="radio" value="system" v-model="imageViewer" name="image-viewer-selector" />
            System
          </label>
          <label class="radio">
            <input type="radio" value="app" v-model="imageViewer" name="image-viewer-selector" />
            App
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
