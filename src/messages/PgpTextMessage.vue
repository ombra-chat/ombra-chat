<script setup lang="ts">
import { nextTick, ref, watch } from 'vue';
import { MessagePgpText, MessageWithStatus } from '../model';
import { decryptPgpTextMessage } from '../services/pgp';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessagePgpText
}>();

const textContent = ref<string | null>(null);
const decryptionError = ref(false);

watch(
  () => props.content,
  async (newContent) => {
    if (newContent.text === null) {
      try {
        textContent.value = await decryptPgpTextMessage(newContent.document_id);
      } catch {
        decryptionError.value = true;
      }
      await nextTick(() => {
        store.messageLoaded(props.message.id);
      });
    } else {
      textContent.value = newContent.text;
    }
  },
  { immediate: true }
);
</script>

<template>
  <div v-if="textContent === null">...</div>
  <div v-else-if="decryptionError" class="message is-danger">
    <div class="message-body">
      Unable to decrypt message
    </div>
  </div>
  <p v-else>{{ textContent }}</p>
</template>
