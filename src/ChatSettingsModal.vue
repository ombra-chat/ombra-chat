<script setup lang="ts">
import { store } from './store';
import { computed, ref, watch } from 'vue';
import { open } from '@tauri-apps/plugin-dialog';
import { PublicKeyFingerprints } from './model';
import { loadPublicKey, removeChatKey, saveChatKey } from './services/pgp';
import Dropdown from './components/Dropdown.vue';

function closeModal() {
  pgpError.value = '';
  loadedKey.value = null
  selectedEncryptionSubkey.value = '';
  selectedKeyFile.value = '';
  store.toggleChatSettingsModal();
}

const enablePgp = ref(false);
const pgpError = ref('');
const selectedKeyFile = ref('');
const loadedKey = ref(null as PublicKeyFingerprints | null);
const selectedEncryptionSubkey = ref('');

async function openSelectKeyDialog() {
  const file = await open({ multiple: false, directory: false, });
  if (file === null) {
    return;
  }
  pgpError.value = '';
  loadedKey.value = null
  try {
    loadedKey.value = await loadPublicKey(file);
    selectedEncryptionSubkey.value = loadedKey.value.encryption_keys[0];
    selectedKeyFile.value = file;
  } catch (err) {
    pgpError.value = err as string;
  }
}

async function saveChatSettings() {
  try {
    if (enablePgp.value && selectedKeyFile.value) {
      await saveChatKey(selectedKeyFile.value, selectedEncryptionSubkey.value, store.selectedChat!.id);
      store.selectedChatKey = selectedEncryptionSubkey.value;
    } else {
      await removeChatKey(store.selectedChat!.id);
      store.selectedChatKey = '';
    }
  } catch (err) {
    pgpError.value = err as string;
    return;
  }

  closeModal();
}

const selectableEncryptionKeys = computed<Array<{ id: number, label: string }>>(() => {
  if (loadedKey.value === null) {
    return [];
  }
  return loadedKey.value.encryption_keys.map((k, i) => ({ id: i, label: k }));
});

function selectSubkey(index: number) {
  if (loadedKey.value === null) {
    return;
  }
  selectedEncryptionSubkey.value = loadedKey.value.encryption_keys[index];
}

watch(
  () => store.chatSettingsModalActive,
  async (active) => {
    if (active) {
      enablePgp.value = store.selectedChatKey !== '';
    }
  }
);
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.chatSettingsModalActive }" id="chat-settings-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">Chat settings</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3">
        <div class="mt-2">
          <label class="checkbox">
            <input type="checkbox" v-model="enablePgp" />
            Enable PGP
          </label>
        </div>
        <button class="button is-primary mt-2 mb-2" v-if="enablePgp" @click="openSelectKeyDialog">Select key</button>
        <div v-if="enablePgp && store.selectedChatKey">
          <p>Encryption subkey: <code>{{ store.selectedChatKey }}</code></p>
        </div>
        <div v-if="loadedKey !== null">
          <p>Master key: <code>{{ loadedKey.primary }}</code></p>
          <p v-if="loadedKey.encryption_keys.length === 1">
            Encryption subkey: <code>{{ loadedKey.encryption_keys[0] }}</code>
          </p>
          <div v-else>
            <p>Select encryption subkey:</p>
            <Dropdown :values="selectableEncryptionKeys" :default-value="0" @change="selectSubkey" />
          </div>
        </div>
        <div class="message is-danger mt-2" v-if="pgpError">
          <div class="message-body">
            {{ pgpError }}
          </div>
        </div>
      </section>
      <footer class="modal-card-foot p-2">
        <div class="buttons">
          <button class="button is-link" @click="saveChatSettings">Save</button>
          <button class="button" @click="closeModal">Cancel</button>
        </div>
      </footer>
    </div>
  </div>
</template>

<style>
#chat-settings-modal .modal-card {
  height: 95%;
}
</style>
