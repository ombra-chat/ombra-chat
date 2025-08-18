<script setup lang="ts">
import { nextTick, onMounted } from 'vue';
import { MessageAnimatedEmoji, MessageWithStatus } from '../model';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessageAnimatedEmoji
}>();

async function imageLoaded() {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
}

onMounted(async () => {
  await nextTick(() => {
    if (!store.allReactions[props.content.emoji]) {
      store.messageLoaded(props.message.id);
    }
  });
});
</script>

<template>
  <figure class="mb-2">
    <img alt="" :src="store.allReactions[content.emoji]" v-if="store.allReactions[content.emoji]" @load="imageLoaded" />
    <span v-else>{{ content.emoji }}</span>
  </figure>
</template>
