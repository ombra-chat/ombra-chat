<script setup lang="ts">
import { Message } from '../model';
import PhotoMessage from './PhotoMessage.vue';
import TextMessage from './TextMessage.vue';
import NotSupportedMessage from './NotSupportedMessage.vue';
import { store } from '../store';
import { computed } from 'vue';

const props = defineProps<{
  message: Message
}>();

const isMyMessage = computed(() => {
  const sender = props.message.sender_id;
  return sender['@type'] === 'messageSenderUser' && sender.user_id === store.myId;
});
</script>

<template>
  <div class="card m-2" :class="{ 'has-background-success-light': isMyMessage }">
    <div class="card-content p-3">
      <TextMessage v-if="props.message.content['@type'] === 'messageText'" :content="props.message.content" />
      <PhotoMessage v-else-if="props.message.content['@type'] === 'messagePhoto'" :content="props.message.content" />
      <NotSupportedMessage v-else :content="props.message.content" />
    </div>
  </div>
</template>