<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { MessagePhoto, File, PhotoSize } from '../model';
import { downloadFile, getPhoto } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';

const props = defineProps<{
  content: MessagePhoto
}>();

const photoSrc = ref('');

const size = computed(() => {
  const sizes = props.content.photo.sizes;
  if (sizes.length > 0) {
    return { width: sizes[0].width, height: sizes[0].height };
  }
  return { width: 150, height: 150 };
})

async function selectPhotoSize(content: MessagePhoto) {
  if (content.photo.sizes.length > 0) {
    const size = content.photo.sizes[0];
    await setPhoto(content, size.photo, size);
  }
}

async function setPhoto(content: MessagePhoto, photo: File, size: PhotoSize) {
  if (photo.local.is_downloading_completed) {
    photoSrc.value = await getPhoto(photo.local.path);
  } else {
    const file = await downloadFile(photo.id);
    if (file) {
      await setPhoto(content, photo, size);
    }
  }
}

async function openPhoto() {
  const sizes = props.content.photo.sizes;
  if (sizes.length === 0) {
    return;
  }
  const largerSize = sizes[sizes.length - 1];
  await openPhotoInNewWindow(largerSize.photo, largerSize);
}

async function openPhotoInNewWindow(photo: File, largerSize: PhotoSize) {
  if (photo.local.is_downloading_completed) {
    await openPath(`file://${photo.local.path}`);
  } else {
    const file = await downloadFile(photo.id);
    if (file) {
      await openPhotoInNewWindow(file, largerSize);
    }
  }
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
      :style="{ 'max-width': size.width }" @click="openPhoto" />
  </figure>
  <p>{{ props.content.caption.text }}</p>
</template>

<style>
.msg-photo img {
  width: initial;
  border-radius: 5px;
  cursor: pointer;
}
</style>