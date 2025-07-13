<script setup lang="ts">
import { ref, watch } from 'vue';
import { MessageDocument } from '../model';
import { decryptFileToString } from '../services/pgp';
import { downloadFile } from '../services/files';

const props = defineProps<{
  content: MessageDocument
}>();

const downloading = ref(false);
const decrypting = ref(false);
const textContent = ref('');

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.document.document.id);
  if (file?.local.is_downloading_completed) {
    downloading.value = false;
    decrypt(file.local.path);
  }
}

async function decrypt(path: string) {
  decrypting.value = true;
  textContent.value = await decryptFileToString(path);
  decrypting.value = false;
}

watch(
  () => props.content,
  async (newContent) => {
    if (!newContent.document.document.local.is_downloading_completed) {
      await download();
    } else {
      decrypt(newContent.document.document.local.path);
    }
  },
  { immediate: true }
);
</script>

<template>
  <div v-if="downloading || decrypting">...</div>
  <p v-else>{{ textContent }}</p>
</template>
