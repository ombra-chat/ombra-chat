<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { MessageDocument } from '../model';
import { decryptFile, decryptNameAndCaption } from '../services/pgp';
import { downloadFile, saveFile } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';
import { save } from '@tauri-apps/plugin-dialog';

const props = defineProps<{
  content: MessageDocument
}>();

const downloading = ref(false);
const decrypting = ref(false);
const decryptingCaption = ref(false);
const fileName = ref('');
const caption = ref('');
const decryptedFilePath = ref('');

async function decryptCaption(ciphertext: string) {
  if (ciphertext === '') {
    return;
  }

  decryptingCaption.value = true;

  const plaintext = await decryptNameAndCaption(ciphertext);
  fileName.value = plaintext.fileName;

  if (plaintext.caption !== null) {
    caption.value = plaintext.caption;
  }

  decryptingCaption.value = false;
}

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.document.document.id);
  if (file?.local.is_downloading_completed) {
    downloading.value = false;
  }
}

async function decrypt(path: string) {
  if (path === '') {
    return;
  }
  decrypting.value = true;
  decryptedFilePath.value = await decryptFile(path);
  decrypting.value = false;
}

async function openFile() {
  const path = decryptedFilePath.value;
  if (path !== '') {
    await openPath(`file://${path}`);
  }
}

async function openSaveDialog() {
  const srcPath = decryptedFilePath.value;
  if (srcPath === '') {
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
    await decryptCaption(newContent.caption.text);
    if (newContent.document.document.local.is_downloading_completed) {
      await decrypt(newContent.document.document.local.path);
    }
  },
  { immediate: true }
);

const downloaded = computed(() => props.content.document.document.local.is_downloading_completed);
</script>

<template>
  <div v-if="decryptingCaption">...</div>
  <div v-else>
    <p>{{ fileName }}</p>

    <div class="mt-1">
      <button class="button is-link" type="button" v-if="downloaded && !decrypting" @click="openFile">
        Open
      </button>
      <button class="button is-primary ml-2" type="button" v-if="downloaded && !decrypting" @click="openSaveDialog">
        Save
      </button>
      <button class="button is-link" type="button" v-else @click="download" :disabled="downloading">
        Download
        <span class="is-loading" v-if="downloading"></span>
      </button>
    </div>

    <p class="mt-3" v-if="caption !== ''">
      {{ caption }}
    </p>
  </div>
</template>
