<script setup lang="ts">
import { nextTick, onMounted } from 'vue';
import { MessageWithStatus } from '../model';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus
}>();

onMounted(async () => {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
});
</script>

<template>
  Type <code>{{ props.message.content['@type'] }}</code> not supported yet.
</template>
