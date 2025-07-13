<script setup lang="ts">
import { Message } from '../model';
import PhotoMessage from './PhotoMessage.vue';
import TextMessage from './TextMessage.vue';
import PgpTextMessage from './PgpTextMessage.vue';
import PgpDocumentMessage from './PgpDocumentMessage.vue';
import NotSupportedMessage from './NotSupportedMessage.vue';
import { store } from '../store';
import { computed } from 'vue';
import DocumentMessage from './DocumentMessage.vue';

const props = defineProps<{
  message: Message
}>();

const isMyMessage = computed(() => {
  const sender = props.message.sender_id;
  return sender['@type'] === 'messageSenderUser' && sender.user_id === store.myId;
});

const isPgpMessage = computed(() => {
  const { content } = props.message;
  return content['@type'] === 'messageDocument'
    && content.document.file_name.startsWith('ombra-chat-')
    && content.document.file_name.endsWith('.pgp')
});

const isPgpTextMessage = computed(() => {
  const { content } = props.message;
  return content['@type'] === 'messageDocument'
    && content.document.file_name.startsWith('ombra-chat-')
    && content.document.file_name.endsWith('.txt.pgp')
});
</script>

<template>
  <div class="card m-2 message-bubble" :class="{ 'has-background-success-light': isMyMessage, 'is-pgp': isPgpMessage }">
    <div class="card-content p-3">
      <PgpTextMessage v-if="props.message.content['@type'] === 'messageDocument' && isPgpTextMessage"
        :content="props.message.content" />
      <PgpDocumentMessage v-else-if="props.message.content['@type'] === 'messageDocument' && isPgpMessage"
        :content="props.message.content" />
      <TextMessage v-else-if="props.message.content['@type'] === 'messageText'" :content="props.message.content" />
      <PhotoMessage v-else-if="props.message.content['@type'] === 'messagePhoto'" :content="props.message.content" />
      <DocumentMessage v-else-if="props.message.content['@type'] === 'messageDocument'"
        :content="props.message.content" />
      <NotSupportedMessage v-else :content="props.message.content" />
    </div>
  </div>
</template>

<style>
.is-pgp {
  border: 2px blue dashed;
}

.message-bubble {
  overflow-wrap: break-word;
}
</style>