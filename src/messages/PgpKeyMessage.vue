<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { MessagePgpKey, MessageWithStatus } from '../model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faKey, faWarning } from '@fortawesome/free-solid-svg-icons';
import { openPath } from '@tauri-apps/plugin-opener';
import { store } from '../store';
import { downloadPgpKeyFile, saveChatKey } from '../services/pgp';
import { selectChat } from '../services/chats';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessagePgpKey
}>();

const path = ref<string | null>(null);
const fingerprint = ref<string | null>(null);

const isMyMessage = computed(() => {
  return props.message.sender_user_id === store.myId;
});

const isMyChat = computed(() => {
  const chat = store.getChat(props.message.chat_id);
  if (!chat) {
    return false;
  }
  return chat.user_id === store.myId;
});

async function download() {
  const data = await downloadPgpKeyFile(props.content.document_id);
  path.value = data.path;
  fingerprint.value = data.fingerprint;
}

async function openFile() {
  await openPath(`file://${path.value}`);
}

async function useKey() {
  if (fingerprint.value !== null && path.value !== null) {
    await saveChatKey(path.value, fingerprint.value, store.selectedChat!.id);
    store.selectedChatKey = fingerprint.value;
    await selectChat(store.selectedChat!.id, true);
  }
}

onMounted(async () => {
  if (props.content.path && props.content.fingerprint) {
    path.value = props.content.path;
    fingerprint.value = props.content.fingerprint;
  } else {
    await download();
  }
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
});
</script>

<template>
  <div class="notification has-background-warning-soft is-outlined p-2 mb-0">
    <p class="mb-2">Public PGP key</p>
    <p class="mb-2" v-if="fingerprint">
      <FontAwesomeIcon :icon="faKey" /><code class="ml-2">{{ fingerprint.toUpperCase() }}</code>
    </p>
    <div class="mb-2">
      <button class="button is-warning mr-2" type="button" @click="useKey" :disabled="!path"
        v-if="!isMyMessage || isMyChat">
        Use this key
      </button>
      <button class="button is-link" type="button" @click="openFile" :disabled="!path">
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