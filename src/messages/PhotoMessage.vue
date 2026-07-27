<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { MessagePhoto, File, PhotoSize, MessageWithStatus } from '../model';
import { downloadFile } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';
import { store } from '../store';
import { getAllWebviewWindows, WebviewWindow } from '@tauri-apps/api/webviewWindow';
import { getImageViewer } from '../settings/settings';
import { convertFileSrc } from '@tauri-apps/api/core';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessagePhoto
}>();

const photoSrc = ref('');

const size = computed(() => {
  const sizes = props.content.sizes;
  if (sizes.length > 0) {
    return { width: sizes[0].width, height: sizes[0].height };
  }
  return { width: 150, height: 150 };
})

async function selectPhotoSize(content: MessagePhoto) {
  const sizes = content.sizes;
  if (sizes.length > 0) {
    const smallest = sizes[0];
    await setPhoto(content, smallest.photo, smallest);
  }
}

async function setPhoto(content: MessagePhoto, photo: File, size: PhotoSize) {
  if (photo.path) {
    photoSrc.value = convertFileSrc(photo.path);
  } else {
    const file = await downloadFile(photo.id);
    if (file) {
      await setPhoto(content, file, size);
    }
  }
}

async function photoLoaded() {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
}

async function openPhoto() {
  const sizes = props.content.sizes;
  if (sizes.length === 0) {
    return;
  }
  const largest = sizes[sizes.length - 1];
  await openPhotoInNewWindow(largest.photo, largest);
}

async function openPhotoInNewWindow(photo: File, largerSize: PhotoSize) {
  if (photo.path) {
    const imageViewer = await getImageViewer();
    if (imageViewer === 'system') {
      await openPath(`file://${photo.path}`);
    } else {
      await openPhotoInTauriWindows(photo, largerSize);
    }
  } else {
    const file = await downloadFile(photo.id);
    if (file) {
      await openPhotoInNewWindow(file, largerSize);
    }
  }
}

async function openPhotoInTauriWindows(photo: File, largerSize: PhotoSize) {
  const windows = await getAllWebviewWindows();
  const webview = new WebviewWindow(`picture-${windows.length}`, {
    url: 'picture.html?path=' + encodeURIComponent(photo.path!),
    title: 'OmbraChat - Picture',
    width: largerSize.width,
    height: largerSize.height,
  });
  webview.once('tauri://error', function (e) {
    console.log(e);
  });
}

watch(
  () => props.content,
  async (newContent) => {
    await selectPhotoSize(newContent);
  },
  { immediate: true }
);
</script>

<template>
  <figure class="image msg-photo mb-2" :class="{ 'is-skeleton': !photoSrc }">
    <img alt="" :src="photoSrc" v-if="photoSrc" :width="size.width" :height="size.height"
      :style="{ 'max-width': size.width }" @click="openPhoto" @load="photoLoaded" />
  </figure>
  <p>{{ props.content.caption }}</p>
</template>

<style>
.msg-photo img {
  width: initial;
  border-radius: 5px;
  cursor: pointer;
}
</style>