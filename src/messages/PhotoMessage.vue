<script setup lang="ts">
import { computed, nextTick, ref, watch } from 'vue';
import { MessagePhoto, File, PhotoSize, MessageWithStatus } from '../model';
import { downloadFile, getPhoto } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
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
  const sizes = getUsablePhotoSize(content);
  if (sizes.length > 0) {
    const smallest = sizes[0];
    await setPhoto(content, smallest.photo, smallest);
  }
}

function getUsablePhotoSize(content: MessagePhoto): PhotoSize[] {
  return content.photo.sizes.filter(s => {
    const localFile = s.photo.local;
    return (
      // See https://core.telegram.org/api/files#image-thumbnail-types
      s.type !== 't' && s.type !== 'i' && s.type !== 'j' &&
      ((localFile.is_downloading_completed && localFile.path !== '') || localFile.can_be_downloaded)
    )
  })
}

async function setPhoto(content: MessagePhoto, photo: File, size: PhotoSize) {
  if (photo.local.is_downloading_completed) {
    if (photo.local.path !== '') {
      photoSrc.value = await getPhoto(photo.local.path);
    }
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
  const sizes = getUsablePhotoSize(props.content);
  if (sizes.length === 0) {
    return;
  }
  const largest = sizes[sizes.length - 1];
  await openPhotoInNewWindow(largest.photo, largest);
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
      :style="{ 'max-width': size.width }" @click="openPhoto" @load="photoLoaded" />
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