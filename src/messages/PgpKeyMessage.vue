<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { MessageDocument, MessageWithStatus } from '../model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faKey, faWarning } from '@fortawesome/free-solid-svg-icons';
import { downloadFile } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';
import { store } from '../store';
import { saveChatKey } from '../services/pgp';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessageDocument
}>();

const downloading = ref(false);

const isMyMessage = computed(() => {
  const sender = props.message.sender_id;
  return sender['@type'] === 'messageSenderUser' && sender.user_id === store.myId;
});

const isMyChat = computed(() => {
  const chat = store.getChat(props.message.chat_id);
  if (!chat) {
    return false;
  }
  return chat.type['@type'] === 'chatTypePrivate' && chat.type.user_id === store.myId;
});

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.document.document.id);
  if (file?.local.is_downloading_completed) {
    downloading.value = false;
  } else {
    await download();
  }
}

async function openFile() {
  const path = props.content.document.document.local.path;
  if (path !== '') {
    await openPath(`file://${path}`);
  }
}

async function useKey() {
  const path = props.content.document.document.local.path;
  if (path !== '') {
    await saveChatKey(path, keyFingerprint.value, store.selectedChat!.id);
    store.selectedChatKey = keyFingerprint.value;
  }
}

const keyFingerprint = computed(() =>
  props.content.document.file_name.replace('ombra-chat-', '').replace('.key', '')
)

onMounted(async () => {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
  if (!props.content.document.document.local.is_downloading_completed) {
    await download();
  }
});
</script>

<template>
  <div class="notification has-background-warning-soft is-outlined p-2 mb-0">
    <p class="mb-2">Public PGP key</p>
    <p class="mb-2">
      <FontAwesomeIcon :icon="faKey" /><code class="ml-2">{{ keyFingerprint }}</code>
    </p>
    <div class="mb-2">
      <button class="button is-warning mr-2" type="button" @click="useKey" :disabled="downloading"
        v-if="!isMyMessage || isMyChat">
        Use this key
      </button>
      <button class="button is-link" type="button" @click="openFile" :disabled="downloading">
        Open key
      </button>
    </div>
    <p class="mb-2 mt-1" v-if="!isMyMessage">
      <FontAwesomeIcon :icon="faWarning" />
      <strong class="ml-2">Click the button only if you trust this message completely. Consider verifying the key
        through another channel first.</strong>
    </p>
  </div>
</template>