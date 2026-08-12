<script setup lang="ts">
import { computed, nextTick, onMounted, ref } from 'vue';
import { MessagePgpKey, MessageWithStatus, PublicKeyCompleteInfo } from '../model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faKey, faUser, faWarning } from '@fortawesome/free-solid-svg-icons';
import { openPath } from '@tauri-apps/plugin-opener';
import { store } from '../store';
import { downloadPgpKeyFile, saveChatKey } from '../services/pgp';
import { selectChat } from '../services/chats';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessagePgpKey
}>();

const path = ref<string | null>(null);
const keyInfo = ref<PublicKeyCompleteInfo | null>(null);

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
  keyInfo.value = data.key_info;
}

async function openFile() {
  await openPath(`file://${path.value}`);
}

async function copyArmored() {
  if (!keyInfo.value) {
    return;
  }
  navigator.clipboard.writeText(keyInfo.value.armored_data);
}

async function useKey() {
  if (keyInfo.value !== null && path.value !== null) {
    await saveChatKey(path.value, keyInfo.value.encryption_key_fingerprint, store.selectedChat!.id);
    store.selectedChatKey = keyInfo.value.encryption_key_fingerprint;
    await selectChat(store.selectedChat!.id, true);
  }
}

onMounted(async () => {
  if (props.content.path && props.content.key_info) {
    path.value = props.content.path;
    keyInfo.value = props.content.key_info;
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
    <div class="mb-2" v-if="keyInfo">
      <div class="mb-1">
        <FontAwesomeIcon :icon="faUser" />
        <code class="ml-2">{{ keyInfo.id_key_fingerprint.toUpperCase() }}</code>
        <span class="ml-2 is-size-7 is-italic">identity</span>
      </div>
      <div>
        <FontAwesomeIcon :icon="faKey" />
        <code class="ml-2">{{ keyInfo.encryption_key_fingerprint.toUpperCase() }}</code>
        <span class="ml-2 is-size-7 is-italic">encryption</span>
      </div>
    </div>
    <div class="mb-2">
      <button class="button is-warning mr-2 mb-1" type="button" @click="useKey" :disabled="!path"
        v-if="!isMyMessage || isMyChat">
        Use this key
      </button>
      <button class="button is-link mr-2 mb-1" type="button" @click="openFile" :disabled="!path">
        Open key
      </button>
      <button class="button is-primary mb-1" type="button" @click="copyArmored" :disabled="!path">
        Copy armored
      </button>
    </div>
    <p class="mb-2 mt-1" v-if="!isMyMessage">
      <FontAwesomeIcon :icon="faWarning" />
      <strong class="ml-2">Click the button only if you trust this message completely. Consider verifying the key
        through another channel first.</strong>
    </p>
  </div>
</template>

<style>
.key-inline {
  display: inline-block
}
</style>
