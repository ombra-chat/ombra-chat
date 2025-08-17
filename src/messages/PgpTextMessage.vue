<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { MessageDocument, MessageWithStatus } from '../model';
import { decryptFileToString } from '../services/pgp';
import { downloadFile } from '../services/files';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessageDocument
}>();

const downloading = ref(false);
const decrypting = ref(false);
const textContent = ref('');
const decryptionError = ref(false);

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.document.document.id);
  if (file?.local.is_downloading_completed) {
    downloading.value = false;
    await decrypt(file.local.path);
  } else {
    await download();
  }
}

async function decrypt(path: string) {
  decrypting.value = true;
  try {
    textContent.value = await decryptFileToString(path);
  } catch (err) {
    console.error(err);
    decryptionError.value = true;
  } finally {
    decrypting.value = false;
    await nextTick(() => {
      store.messageLoaded(props.message.id);
    });
  }
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
  <div v-else-if="decryptionError" class="message is-danger">
    <div class="message-body">
      Unable to decrypt message
    </div>
  </div>
  <p v-else>{{ textContent }}</p>
</template>
