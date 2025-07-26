<script setup lang="ts">
import { Message, MessageReplyToMessage, MessageWithStatus } from '../model';
import PhotoMessage from './PhotoMessage.vue';
import TextMessage from './TextMessage.vue';
import PgpTextMessage from './PgpTextMessage.vue';
import PgpDocumentMessage from './PgpDocumentMessage.vue';
import NotSupportedMessage from './NotSupportedMessage.vue';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faGear } from '@fortawesome/free-solid-svg-icons';
import { store } from '../store';
import { computed, onMounted, ref } from 'vue';
import DocumentMessage from './DocumentMessage.vue';
import { getMessageTextContent, getRepliedMessage, getSenderTitle } from '../services/chats';

const props = defineProps<{
  message: MessageWithStatus
}>();

const isMyMessage = computed(() => {
  const sender = props.message.sender_id;
  return sender['@type'] === 'messageSenderUser' && sender.user_id === store.myId;
});

const isPgpMessage = computed(() => {
  if (store.selectedChatKey === '') {
    return false;
  }
  const { content } = props.message;
  return content['@type'] === 'messageDocument'
    && content.document.file_name.startsWith('ombra-chat-')
    && content.document.file_name.endsWith('.pgp');
});

const isPgpTextMessage = computed(() => {
  if (store.selectedChatKey === '') {
    return false;
  }
  const { content } = props.message;
  return content['@type'] === 'messageDocument'
    && content.document.file_name.startsWith('ombra-chat-')
    && content.document.file_name.endsWith('.txt.pgp');
});

const senderTitle = computed(() => getSenderTitle(props.message.sender_id));

async function openMessageModal(message: Message) {
  store.selectedMessage = message;
  store.toggleMessageModal();
}

const replyToSenderTitle = ref<string | null>(null);
const replyToContent = ref<string | null>(null);

async function loadReplyToMessage() {
  const replyTo = props.message.reply_to;
  if (replyTo === null) {
    return null;
  }
  if (replyTo['@type'] !== 'messageReplyToMessage') {
    return null;
  }
  const message = await getRepliedMessage(props.message.chat_id, props.message.id);
  replyToSenderTitle.value = getSenderTitle(message.sender_id);
  if (replyTo.quote !== null) {
    replyToContent.value = truncateReplyContent(replyTo.quote.text.text);
  } else {
    replyToContent.value = truncateReplyContent(getMessageTextContent(message.content) || '');
  }
}

function truncateReplyContent(text: string) {
  const maxLength = 100;
  if (text.length > maxLength) {
    return text.substring(0, maxLength - 3) + '...';
  }
  return text;
}

onMounted(async () => {
  await loadReplyToMessage();
  store.messageBubbleLoaded(props.message.id);
});
</script>

<template>
  <div class="card m-2 message-bubble"
    :class="{ 'has-background-success-light': isMyMessage, 'is-pgp': isPgpMessage, 'unread': !message.read }"
    :data-message-id="message.id">
    <div class="card-content p-3">
      <div class="message-header">
        <div class="message-sender">
          <p v-if="senderTitle" class="mb-2 wrap">
            <strong class="has-text-link">{{ senderTitle }}</strong>
          </p>
        </div>
        <div class="message-actions">
          <a href="#" @click="() => openMessageModal(message)">
            <FontAwesomeIcon :icon="faGear" />
          </a>
        </div>
      </div>
      <div class="message-reply-to p-2 mb-2 has-background-primary-light" v-if="replyToSenderTitle !== null">
        <strong class="mr-2">{{ replyToSenderTitle }}</strong>
        <span>{{ replyToContent }}</span>
      </div>
      <PgpTextMessage v-if="message.content['@type'] === 'messageDocument' && isPgpTextMessage" :message="message"
        :content="message.content" />
      <PgpDocumentMessage v-else-if="message.content['@type'] === 'messageDocument' && isPgpMessage" :message="message"
        :content="message.content" />
      <TextMessage v-else-if="message.content['@type'] === 'messageText'" :message="message"
        :content="message.content" />
      <PhotoMessage v-else-if="message.content['@type'] === 'messagePhoto'" :message="message"
        :content="message.content" />
      <DocumentMessage v-else-if="message.content['@type'] === 'messageDocument'" :message="message"
        :content="message.content" />
      <NotSupportedMessage v-else :message="message" />
    </div>
  </div>
</template>

<style>
.is-pgp {
  border: 2px blue dashed;
}

.is-pgp.unread {
  border: 2px blueviolet dashed;
}

.unread {
  border: 2px blueviolet solid;
}

.message-bubble {
  overflow-wrap: break-word;
}

.message-header {
  display: flex;
  flex-direction: row;
}

.message-sender {
  flex-grow: 1;
}

.message-reply-to {
  border-radius: 6px;
}
</style>