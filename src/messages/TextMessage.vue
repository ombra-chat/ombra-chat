<script setup lang="ts">
import { nextTick, onMounted } from 'vue';
import { MessageText, MessageWithStatus } from '../model';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessageText
}>();

onMounted(async () => {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
});

</script>

<template>
{{ props.content.text.text }}
</template>
