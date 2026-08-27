<script setup lang="ts">
import { computed, nextTick, onDeactivated, onMounted, ref, watch } from 'vue';
import MessageBubble from './messages/MessageBubble.vue';
import { getSenderTitle, loadNewMessages, loadPreviousMessages, sendMessage, closeCurrentChat } from './services/chats';
import { store } from './store';
import { open } from '@tauri-apps/plugin-dialog';
import { listen, UnlistenFn } from '@tauri-apps/api/event'
import { InputMessageContent, InputMessageDocument, InputMessagePhoto, InputMessageReplyTo } from './model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faPaperPlane, faGear, faPaperclip, faX, faKey, faLock, faChevronLeft } from '@fortawesome/free-solid-svg-icons';
import { getFileName } from './services/files';
import ChatSettingsModal from './ChatSettingsModal.vue';
import MessageModal from './MessageModal.vue';
import ChatKeyModal from './ChatKeyModal.vue';
import UserModal from './UserModal.vue';

type SelectedFile = { path: string; image: boolean; }

const newMessageText = ref('');
const selectedFiles = ref([] as SelectedFile[]);
const dragging = ref(false);

async function chatContentScrolled(event: Event) {
  const element = event.target as HTMLElement;
  if (element.scrollTop === 0) {
    await newMessages();
  } else if (element.scrollHeight - element.getBoundingClientRect().height + element.scrollTop === 0) {
    await loadPreviousMessages();
    return;
  }
  await markVisibleMessagesAsRead();
}

function isFirstMessageVisible() {
  const container = document.getElementById('chat-content')!;
  const containerRect = container.getBoundingClientRect();
  const bubble = document.querySelector('.message-bubble');
  if (!bubble) {
    return false;
  }
  const bubbleRect = bubble.getBoundingClientRect();
  return bubbleRect.bottom >= containerRect.top && bubbleRect.bottom <= containerRect.bottom;
}

async function markVisibleMessagesAsRead() {
  const container = document.getElementById('chat-content')!;
  const containerRect = container.getBoundingClientRect();
  const bubbles = document.querySelectorAll('.message-bubble');
  for (const bubble of bubbles) {
    const bubbleRect = bubble.getBoundingClientRect();
    if (bubbleRect.bottom >= containerRect.top && bubbleRect.bottom <= containerRect.bottom) {
      const dataId = bubble.getAttribute('data-message-id');
      if (dataId) {
        store.markMessageAsRead(parseInt(dataId));
      }
    }
  }
}

function scrollToMessage(messageId: number) {
  const element = document.querySelector(`[data-message-id="${messageId}"]`);
  if (element) {
    element.scrollIntoView({ behavior: 'instant', block: 'end' });
  }
}

async function send() {
  const contents = await getInputMessageContents();
  const chat = store.selectedChat;
  if (contents.length === 0 || !chat) {
    return;
  }
  for (const content of contents) {
    await sendMessage(chat.id, getInputMessageReplyTo(), content);
    const chatContent = document.getElementById('chat-content');
    if (chatContent) {
      chatContent.scrollTo(0, chatContent.scrollHeight);
    }
  }
  clear();
}

function clear() {
  newMessageText.value = '';
  selectedFiles.value = [];
  store.replyToMessage = null;
  store.replyToQuote = null;
}

function getInputMessageReplyTo(): InputMessageReplyTo | null {
  if (store.replyToMessage === null) {
    return null;
  }
  return {
    message_id: store.replyToMessage.id,
    quote: store.replyToQuote
  };
}

async function getInputMessageContents(): Promise<InputMessageContent[]> {
  if (store.selectedChatKey === '') {
    return getStandardInputMessageContents();
  } else {
    return await getPgpInputMessageContents();
  }
}

function getStandardInputMessageContents(): InputMessageContent[] {
  const contents: InputMessageContent[] = [];
  let text: string | null = newMessageText.value;
  if (selectedFiles.value.length === 0) {
    if (text !== null) {
      contents.push(
        { '@type': 'inputMessageText', text: text }
      )
    }
  } else {
    for (const selectedFile of selectedFiles.value) {
      if ('image' in selectedFile && selectedFile.image) {
        contents.push(getInputMessagePhoto(selectedFile, text));
      } else {
        contents.push(getInputMessageDocument(selectedFile.path, text));
      }
      text = null; // set formatted text only on first file
    }
  }
  return contents;
}

async function getPgpInputMessageContents(): Promise<InputMessageContent[]> {
  const contents: InputMessageContent[] = [];
  if (selectedFiles.value.length === 0) {
    if (newMessageText.value !== '') {
      contents.push({
        '@type': 'inputMessagePgpText',
        text: newMessageText.value,
      });
    }
  } else {
    let caption = newMessageText.value === '' ? null : newMessageText.value;
    for (const selectedFile of selectedFiles.value) {
      contents.push(
        {
          '@type': 'inputMessagePgpFile',
          path: selectedFile.path,
          caption
        }
      );
      caption = null; // set caption text only on first file
    }
  }
  return contents;
}

function getInputMessageDocument(path: string, caption: string | null): InputMessageDocument {
  return {
    '@type': 'inputMessageDocument',
    path,
    caption,
  };
}

function getInputMessagePhoto(file: SelectedFile, caption: string | null): InputMessagePhoto {
  return {
    '@type': 'inputMessagePhoto',
    path: file.path,
    caption,
  }
}

async function selectFiles() {
  const files = await open({ multiple: true, directory: false, });
  if (files === null || files.length === 0) {
    return;
  }
  await addFiles(files);
}

async function addFiles(files: string[]) {
  const filesToAdd: SelectedFile[] = files.map(f => ({
    path: f,
    image: store.selectedChatKey === '' && isImage(f)
  }));
  selectedFiles.value = selectedFiles.value.concat(filesToAdd);
}

function isImage(path: string) {
  for (const extension of ['png', 'jpg', 'jpeg', 'jpg', 'gif']) {
    if (path.endsWith(extension)) {
      return true;
    }
  }
  return false;
}

async function removeFile(index: number) {
  selectedFiles.value = selectedFiles.value.filter((_, i) => i !== index);
}

async function newMessages() {
  if (store.loadingNewMessages) {
    return;
  }
  const lastMessageId = store.lastMessageId;
  store.scrollTargetMessageId = lastMessageId;
  store.loadingNewMessages = true;
  await nextTick(async () => {
    await loadNewMessages();
  });
}

const replyToTitle = computed(() => {
  if (store.replyToMessage === null) {
    return '';
  }
  return getSenderTitle(store.replyToMessage);
});

const secretChatState = computed(() => {
  if (!store.selectedChat) {
    return '';
  }
  if (!store.selectedChat.secret) {
    return ''
  }
  const id = store.selectedChat.secret_chat_id as number;
  const secretChat = store.secretChatsMap[id];
  if (!secretChat) {
    return '';
  }
  return secretChat.state;
});

const canWriteMessages = computed(() => {
  return !store.selectedChat || store.selectedChat.can_send
    || (secretChatState.value !== '' && secretChatState.value !== 'Ready')
});

const secretChatKey = computed<string>(() => {
  if (!store.selectedChat) {
    return '';
  }
  if (!store.selectedChat.secret) {
    return ''
  }
  const id = store.selectedChat.secret_chat_id as number;
  const secretChat = store.secretChatsMap[id];
  if (!secretChat) {
    return '';
  }
  return secretChat.key_hash_hex.join(' ');
});

let unlistener: UnlistenFn | undefined = undefined;

onMounted(async () => {
  unlistener = await listen('tauri://drag-drop', event => {
    const { payload } = event;
    addFiles((payload as any).paths);
  })
});

onDeactivated(() => {
  if (unlistener) {
    unlistener();
  }
});

watch(
  () => store.messagesToLoad.length + store.messagesBubblesToLoad.length,
  async (loadingMessages: number) => {
    if (loadingMessages === 0 && store.loadingNewMessages) {
      await nextTick(async () => {
        scrollToMessage(store.scrollTargetMessageId);
        store.loadingNewMessages = false;
        await markVisibleMessagesAsRead();
        if (store.selectedChat && store.selectedChat.unread_count > 0) {
          const chatContent = document.getElementById('chat-content');
          if (chatContent && chatContent.scrollTop === 0) {
            if (isFirstMessageVisible()) {
              // chat has only a few messages, autoload new ones
              await loadNewMessages();
            } else {
              // slightly move scroll up, to always detect user scroll event on new messages
              chatContent.scrollTop = -1;
            }
          }
        }
      });
    }
  }
);

// clear current message on chat change
watch(() => store.selectedChat?.id, () => clear());
</script>

<template>
  <div v-if="store.selectedChat !== null" id="chat-page" :class="{ 'dragging': dragging }"
    @dragover.prevent="() => (dragging = true)" @dragleave="() => (dragging = false)">
    <div id="chat-header" class="pb-1">
      <div id="chat-title">
        <div class="has-text-link has-text-weight-bold is-flex is-flex-direction-row"
          :class="{ 'pb-2': store.selectedChatKey === '' }">
          <div id="close-chat-button-wrapper">
            <button type="button" class="button is-text has-text-link" @click="() => closeCurrentChat()">
              <FontAwesomeIcon :icon="faChevronLeft" />
            </button>
          </div>
          <div class="is-flex-grow-1 has-text-centered is-align-self-center">
            <button type="button" class="is-text py-0 has-text-weight-bold"
              @click="() => store.toggleUserModal(store.selectedChat?.user_id)" v-if="store.selectedChat.user_id">
              {{ store.selectedChat.title }}
            </button>
            <strong v-else>{{ store.selectedChat.title }}</strong>
            <span class="ml-1" v-if="store.selectedChat.secret">
              <FontAwesomeIcon :icon="faLock" />
            </span>
          </div>
        </div>
        <button type="button" v-if="store.selectedChatKey !== ''" class="ml-2 mt-1 nowrap key-button"
          @click="store.toggleChatKeyModal">
          <FontAwesomeIcon :icon="faKey" />
          <code class="ml-2">{{ store.selectedChatKey.toUpperCase() }}</code>
        </button>
        <button type="button" v-if="secretChatKey !== ''" class="ml-2 mt-1 pt-1 nowrap key-button"
          @click="store.toggleChatKeyModal">
          <FontAwesomeIcon :icon="faLock" />
          <code class="ml-2">{{ secretChatKey }}</code>
        </button>
      </div>
      <div id="chat-settings-btn-wrapper">
        <button type="button" class="button mx-2" @click="store.toggleChatSettingsModal" aria-label="Settings">
          <FontAwesomeIcon :icon="faGear" />
        </button>
      </div>
    </div>
    <div id="chat-content" class="p-1 has-background-link-soft" @scroll="chatContentScrolled">
      <MessageBubble :message="message" v-for="message in store.currentMessages" :key="message.id" />
      <div v-if="secretChatState === 'Pending'" class="box m-5 has-background-warning-light">
        Secret chat is in pending state
      </div>
      <div v-if="secretChatState === 'Closed'" class="box m-5 has-background-danger-light">
        Secret chat is closed
      </div>
    </div>
    <div id="new-messages-box" class="has-background-info p-2" v-if="store.selectedChat.unread_count > 0"
      @click="newMessages">
      new messages
    </div>
    <div id="files-box" class="p-1" v-if="selectedFiles.length > 0">
      <div v-for="(file, index) in selectedFiles" class="file-box">
        <div class="selected-file-name ml-1 nowrap">{{ getFileName(file.path) }}</div>
        <div v-if="isImage(file.path) && store.selectedChatKey === ''" class="mr-3 selected-file-image-checkbox">
          <label class="checkbox">
            <input type="checkbox" v-model="file.image" />
            image
          </label>
        </div>
        <div>
          <a href="#" class="has-text-danger mr-3" @click="() => removeFile(index)">
            <FontAwesomeIcon :icon="faX" />
          </a>
        </div>
      </div>
    </div>
    <div id="reply-to-box" class="pl-1 pt-1 has-background-primary-soft" v-if="store.replyToMessage !== null">
      <div class="nowrap" id="reply-to-sender-title">
        Reply to <strong>{{ replyToTitle }}</strong>
      </div>
      <div>
        <a href="#" class="has-text-danger mr-3" @click="() => (store.replyToMessage = null)">
          <FontAwesomeIcon :icon="faX" />
        </a>
      </div>
    </div>
    <div id="send-message-box" v-if="canWriteMessages">
      <input type="text" class="input" id="new-message-text" v-model="newMessageText" @keyup.enter="send" />
      <button type="button" class="button is-primary" @click="selectFiles" aria-label="Attach file">
        <FontAwesomeIcon :icon="faPaperclip" />
      </button>
      <button type="button" class="button is-link" @click="send" aria-label="Send">
        <FontAwesomeIcon :icon="faPaperPlane" />
      </button>
    </div>
  </div>
  <ChatSettingsModal />
  <MessageModal />
  <ChatKeyModal />
  <UserModal />
</template>

<style>
#chat-page {
  display: flex;
  flex-direction: column;
  height: 100%;
}

#chat-header {
  display: flex;
  flex-direction: row;
  border-bottom: 1px var(--bulma-border-weak) solid;
}

#chat-title {
  flex-grow: 1;
  overflow: hidden;
}

#chat-settings-btn-wrapper {
  margin: auto;
}

#chat-title .has-text-link {
  text-align: center;
}

.key-button {
  max-width: calc(100% - 50px);
}

#chat-content {
  flex-grow: 1;
  display: flex;
  justify-content: flex-end;
  flex-direction: column;
  overflow-x: hidden;
  overflow-y: auto;
  overflow-anchor: none;
}

#send-message-box {
  display: flex;
  flex-direction: row;
}

#send-message-box input,
#send-message-box button {
  border-radius: 0;
}

.file-box {
  display: flex;
  flex-direction: row;
}

.selected-file-name {
  flex-grow: 1;
}

.selected-file-image-checkbox {
  white-space: nowrap;
}

.dragging {
  border: 3px rgb(71, 172, 255) dashed;
}

#new-messages-box {
  cursor: pointer;
  text-align: center;
}

#reply-to-box {
  display: flex;
  flex-direction: row;
}

#reply-to-sender-title {
  flex-grow: 1;
}

#close-chat-button-wrapper {
  display: none;
}

@media screen and (max-width: 400px) {
  #close-chat-button-wrapper {
    display: block;
  }
}
</style>
