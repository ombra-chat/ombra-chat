<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue';
import { MessageDocument, MessageWithStatus } from '../model';
import { openPath } from '@tauri-apps/plugin-opener';
import { downloadFile, saveFile } from '../services/files';
import { save } from '@tauri-apps/plugin-dialog';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessageDocument
}>();

const downloading = ref(false);
const path = ref<string | null>(null);

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.document.id);
  if (!file) {
    return;
  }
  if (file.path) {
    downloading.value = false;
    path.value = file.path;
  } else {
    await download();
  }
}

async function openFile() {
  if (path.value) {
    await openPath(`file://${path.value}`);
  }
}

async function openSaveDialog() {
  const srcPath = path.value;
  if (!srcPath) {
    return;
  }
  const targetPath = await save();
  if (targetPath === null) {
    return;
  }
  await saveFile(srcPath, targetPath);
}

const downloaded = computed(() => path.value !== null);

watch(
  () => props.content,
  async (newContent) => {
    path.value = newContent.document.path;
  }
);

onMounted(async () => {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
});
</script>

<template>
  <p>{{ content.file_name }}</p>

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

  <p v-if="content.caption">
    {{ content.caption }}
  </p>
</template>
