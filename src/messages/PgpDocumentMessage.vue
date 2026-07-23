<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { MessagePgpFile, MessageWithStatus } from '../model';
import { decryptFile } from '../services/pgp';
import { downloadFile, saveFile } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';
import { save } from '@tauri-apps/plugin-dialog';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessagePgpFile
}>();

const loading = ref(false);
const decryptingCaption = ref(false);
const fileName = ref('');
const caption = ref('');
const ciphertextPath = ref<string | null>(null);
const plaintextPath = ref<string | null>(null);
const decryptionError = ref(false);

async function download() {
  loading.value = true;
  const file = await downloadFile(props.content.document_id);
  if (!file) {
    return;
  }
  if (file.path) {
    ciphertextPath.value = file.path;
    props.content.ciphertext_path = file.path;
    await decrypt(file.path);
  } else {
    await download();
  }
}

async function decrypt(path: string) {
  if (path === '') {
    return;
  }
  loading.value = true;
  try {
    plaintextPath.value = await decryptFile(path);
  } catch (err) {
    console.error(err);
    decryptionError.value = true;
  } finally {
    loading.value = false;
  }
}

async function openFile() {
  const path = plaintextPath.value;
  if (path !== '') {
    await openPath(`file://${path}`);
  }
}

async function openSaveDialog() {
  const srcPath = plaintextPath.value;
  if (srcPath === null) {
    return;
  }
  const targetPath = await save();
  if (targetPath === null) {
    return;
  }
  await saveFile(srcPath, targetPath);
}

watch(
  () => props.content,
  async (newContent) => {
    fileName.value = newContent.file_name;
    caption.value = newContent.caption || '';
    ciphertextPath.value = newContent.ciphertext_path;
    plaintextPath.value = newContent.plaintext_path;
    await nextTick(() => {
      store.messageLoaded(props.message.id);
    });
  },
  { immediate: true }
);
</script>

<template>
  <div v-if="decryptingCaption">...</div>
  <div v-else-if="decryptionError" class="message is-danger">
    <div class="message-body">
      Unable to decrypt message
    </div>
  </div>
  <div v-else>
    <p>{{ fileName }}</p>

    <div class="mt-1">
      <button class="button is-link" type="button" v-if="plaintextPath" @click="openFile">
        Open
      </button>
      <button class="button is-primary ml-2" type="button" v-if="plaintextPath" @click="openSaveDialog">
        Save
      </button>
      <button class="button is-link" type="button" v-else @click="download" :disabled="loading">
        Download
        <span class="is-loading" v-if="loading"></span>
      </button>
    </div>

    <p class="mt-3" v-if="caption !== ''">
      {{ caption }}
    </p>
  </div>
</template>
