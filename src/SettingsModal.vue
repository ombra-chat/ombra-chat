<script setup lang="ts">
import { store } from './store';
import Dropdown from './components/Dropdown.vue';
import { onMounted, ref, watch } from 'vue';
import { getDefaultChatFolder, getHideMarginRight, getImageViewer, setDefaultChatFolder, setHideMarginRight, setImageViewer, setTheme } from './settings/settings';
import { changeKeyPassphrase, exportPublicKey, exportSecretKey, getMyKeyFingerprint } from './services/pgp';
import { save } from '@tauri-apps/plugin-dialog';
import { getCurrentWindow, Theme } from '@tauri-apps/api/window';
import { PublicKeyFingerprints } from './model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faLock, faKey } from '@fortawesome/free-solid-svg-icons';

const selectedId = ref(0);
const myKeyFingerprint = ref<PublicKeyFingerprints | null>(null);

const newPassphrase = ref('');
const newPassphraseConfirm = ref('');
const passphraseError = ref('');
const passphraseUpdated = ref(false);
const updatingPassphrase = ref(false);

const theme = ref<Theme>('light');
const keyError = ref('');
const imageViewer = ref<'system' | 'app'>('system');
const hideMarginRight = ref<boolean>(false);

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
  hideMarginRight.value = await getHideMarginRight();
});

async function saveSettings() {
  await setDefaultChatFolder(selectedId.value);
  await setTheme(theme.value);
  await setImageViewer(imageViewer.value);
  await setHideMarginRight(hideMarginRight.value);
  store.hideMarginRight = hideMarginRight.value;
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

async function changePassphrase() {
  if (updatingPassphrase.value) {
    return;
  }
  passphraseError.value = '';
  passphraseUpdated.value = false;
  if (newPassphrase.value !== newPassphraseConfirm.value) {
    passphraseError.value = "Passphrases don't match";
    return;
  }
  try {
    updatingPassphrase.value = true;
    await changeKeyPassphrase(newPassphrase.value);
    newPassphrase.value = '';
    newPassphraseConfirm.value = '';
    passphraseUpdated.value = true;
  } catch (err) {
    passphraseError.value = err.message;
  } finally {
    updatingPassphrase.value = false;
  }
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

        <p class="menu-label mt-4">Change passphrase</p>
        <form @submit.prevent="changePassphrase">
          <div class="field">
            <label class="label mb-0" for="new-passphrase">New PGP passphrase</label>
            <div class="control">
              <input class="input" type="password" id="new-passphrase" v-model="newPassphrase" />
            </div>
          </div>
          <div class="field">
            <label class="label mb-0" for="new-passphrase-confirm">Confirm New PGP passphrase</label>
            <div class="control">
              <input class="input" type="password" id="new-passphrase-confirm" v-model="newPassphraseConfirm" />
            </div>
          </div>
          <div v-if="passphraseError" class="message is-danger mb-2">
            <div class="message-body">
              {{ passphraseError }}
            </div>
          </div>
          <div v-if="passphraseUpdated" class="message is-success mb-2">
            <div class="message-body">
              Passphrase successfully updated
            </div>
          </div>
          <div class="field is-grouped mt-1">
            <div class="control">
              <button class="button is-link" type="submit" :disabled="updatingPassphrase">
                Change
              </button>
            </div>
          </div>
        </form>

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

        <p class="menu-label mt-4">Margin right</p>
        <div class="control">
          <label class="radio mr-2">
            <input type="radio" :value="true" v-model="hideMarginRight" name="margin-right-selector" />
            Hide
          </label>
          <label class="radio">
            <input type="radio" :value="false" v-model="hideMarginRight" name="margin-right-selector" />
            Show
          </label>
        </div>
        <p><em>This is a workaround to prevent a scrollbar issue with WebKit</em></p>
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
