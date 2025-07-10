<script setup lang="ts">
import { Message } from '../model';
import PhotoMessage from './PhotoMessage.vue';
import TextMessage from './TextMessage.vue';
import NotSupportedMessage from './NotSupportedMessage.vue';

const props = defineProps<{
  message: Message
}>();
</script>

<template>
  <div class="card m-2">
    <div class="card-content p-3">
      <TextMessage v-if="props.message.content['@type'] === 'messageText'" :content="props.message.content" />
      <PhotoMessage v-if="props.message.content['@type'] === 'messagePhoto'" :content="props.message.content" />
      <NotSupportedMessage v-else :content="props.message.content" />
    </div>
  </div>
</template>