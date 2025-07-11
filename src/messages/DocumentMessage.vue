<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { MessageDocument } from '../model';
import { openPath } from '@tauri-apps/plugin-opener';
import { downloadFile, saveFile } from '../services/files';
import { save } from '@tauri-apps/plugin-dialog';

const props = defineProps<{
  content: MessageDocument
}>();

const downloading = ref(false);

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.document.document.id);
  if (file?.local.is_downloading_completed) {
    downloading.value = false;
  }
}

async function openFile() {
  const path = props.content.document.document.local.path;
  if (path !== '') {
    await openPath(`file://${path}`);
  }
}

async function openSaveDialog() {
  const srcPath = props.content.document.document.local.path;
  if (srcPath === '') {
    return;
  }
  const targetPath = await save();
  if (targetPath === null) {
    return;
  }
  await saveFile(srcPath, targetPath);
}

const downloaded = computed(() => props.content.document.document.local.is_downloading_completed);
</script>

<template>
  <p>{{ content.document.file_name }}</p>

  <div class="mt-1">
    <button class="button is-link" type="button" v-if="downloaded" @click="openFile">
      Open
    </button>
    <button class="button is-primary ml-2" type="button" v-if="downloaded" @click="openSaveDialog">
      Save
    </button>
    <button class="button is-link" type="button" v-else @click="download" :disabled="downloading">
      Download
      <span class="is-loading" v-if="downloading"></span>
    </button>
  </div>

  <p v-if="props.content.caption">
    {{ props.content.caption.text }}
  </p>
</template>
