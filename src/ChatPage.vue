<script setup lang="ts">
import { computed, nextTick, onDeactivated, onMounted, ref, watch } from 'vue';
import MessageBubble from './messages/MessageBubble.vue';
import { getSenderTitle, loadNewMessages, loadPreviousMessages, sendMessage, closeCurrentChat } from './services/chats';
import { store } from './store';
import { open } from '@tauri-apps/plugin-dialog';
import { listen, UnlistenFn } from '@tauri-apps/api/event'
import { FormattedText, InputMessageContent, InputMessageDocument, InputMessagePhoto, InputMessageReplyTo, InputMessageText, InputTextQuote } from './model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faPaperPlane, faGear, faPaperclip, faX, faKey, faLock, faChevronLeft } from '@fortawesome/free-solid-svg-icons';
import { createThumbnail, getFileName, getImageSize, removeThumbnail } from './services/files';
import ChatSettingsModal from './ChatSettingsModal.vue';
import MessageModal from './MessageModal.vue';
import { createPgpFile, createPgpTextFile, encryptNameAndCaption } from './services/pgp';

type SimpleFile = { path: string };
type ImageFile = { path: string; image: boolean; width: number; height: number };
type SelectedFile = SimpleFile | ImageFile;

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
    if (content['@type'] === 'inputMessagePhoto') {
      await removeThumbnail(content.photo.path);
    }
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
  let quote: InputTextQuote | null = null;
  if (store.replyToQuote !== null) {
    quote = {
      text: {
        text: store.replyToQuote,
        entities: []
      },
      position: 0
    }
  }
  return {
    '@type': 'inputMessageReplyToMessage',
    message_id: store.replyToMessage.id,
    quote
  };
}

async function getInputMessageContents(): Promise<InputMessageContent[]> {
  if (store.selectedChatKey === '') {
    return await getStandardInputMessageContents();
  } else {
    return await getPgpInputMessageContents();
  }
}

async function getStandardInputMessageContents(): Promise<InputMessageContent[]> {
  const contents: InputMessageContent[] = [];
  let formattedText = getSimpleFormattedText(newMessageText.value);
  if (selectedFiles.value.length === 0) {
    if (formattedText !== null) {
      contents.push(
        { '@type': 'inputMessageText', text: formattedText, clear_draft: true } as InputMessageText
      )
    }
  } else {
    for (const selectedFile of selectedFiles.value) {
      if ('image' in selectedFile && selectedFile.image) {
        const content = await getInputMessagePhoto(selectedFile, formattedText);
        if (content !== null) {
          contents.push(content);
        }
      } else {
        contents.push(getInputMessageDocument(selectedFile.path, formattedText));
      }
      formattedText = null; // set formatted text only on first file
    }
  }
  return contents;
}

async function getPgpInputMessageContents(): Promise<InputMessageContent[]> {
  const contents: InputMessageContent[] = [];
  const chatId = store.selectedChat!.id;
  if (selectedFiles.value.length === 0) {
    if (newMessageText.value !== '') {
      const file = await createPgpTextFile(newMessageText.value, chatId);
      if (file !== null) {
        contents.push(
          getInputMessageDocument(file, null)
        );
      }
    }
  } else {
    let caption = newMessageText.value === '' ? null : newMessageText.value;
    for (const selectedFile of selectedFiles.value) {
      const file = await createPgpFile(selectedFile.path, chatId);
      if (file !== null) {
        const ciphertext = await encryptNameAndCaption(getFileName(selectedFile.path), caption, chatId);
        contents.push(
          getInputMessageDocument(file, getSimpleFormattedText(ciphertext))
        );
        caption = null; // set caption text only on first file
      }
    }
  }
  return contents;
}

function getSimpleFormattedText(text: string | null): FormattedText | null {
  if (text === null || text === '') {
    return null;
  }
  return { text, entities: [] };
}

function getInputMessageDocument(path: string, caption: FormattedText | null): InputMessageDocument {
  return {
    '@type': 'inputMessageDocument',
    document: {
      '@type': 'inputFileLocal',
      path
    },
    caption,
    thumbnail: null,
    disable_content_type_detection: true,
  };
}

async function getInputMessagePhoto(file: ImageFile, caption: FormattedText | null): Promise<InputMessagePhoto | null> {
  try {
    const thumbnail = await createThumbnail(file.path);
    return {
      '@type': 'inputMessagePhoto',
      added_sticker_file_ids: [],
      caption,
      has_spoiler: false,
      width: file.width,
      height: file.height,
      photo: {
        '@type': 'inputFileLocal',
        path: file.path
      },
      thumbnail,
      self_destruct_type: null,
      show_caption_above_media: false
    }
  } catch (err) {
    console.error(err);
    return null;
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
  const filesToAdd: SelectedFile[] = [];
  for (const file of files) {
    const image = await getImageInfo(file);
    filesToAdd.push(image !== null ? image : { path: file });
  }
  selectedFiles.value = selectedFiles.value.concat(filesToAdd);
}

async function getImageInfo(path: string): Promise<ImageFile | null> {
  if (store.selectedChatKey !== '') {
    return null;
  }
  if (!isImage(path)) {
    return null;
  }
  const dimensions = await getImageSize(path);
  if (dimensions === null) {
    return null;
  }
  return { path, image: true, width: dimensions.width, height: dimensions.height };
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
  return getSenderTitle(store.replyToMessage.sender_id);
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
      await nextTick(() => {
        scrollToMessage(store.scrollTargetMessageId);
        store.loadingNewMessages = false;
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
            {{ store.selectedChat.title }}
            <span class="ml-1" v-if="store.selectedChat.type['@type'] === 'chatTypeSecret'">
              <FontAwesomeIcon :icon="faLock" />
            </span>
          </div>
        </div>
        <div v-if="store.selectedChatKey !== ''" class="ml-2 mt-1 nowrap">
          <FontAwesomeIcon :icon="faKey" />
          <code class="ml-2">{{ store.selectedChatKey }}</code>
        </div>
      </div>
      <div id="chat-settings-btn-wrapper">
        <button type="button" class="button ml-2" @click="store.toggleChatSettingsModal" aria-label="Settings">
          <FontAwesomeIcon :icon="faGear" />
        </button>
      </div>
    </div>
    <div id="chat-content" class="p-1 has-background-link-light" @scroll="chatContentScrolled">
      <MessageBubble :message="message" v-for="message in store.currentMessages" :key="message.id" />
    </div>
    <div id="new-messages-box" class="has-background-info p-2" v-if="store.selectedChat.unread_count > 0"
      @click="newMessages">
      new messages
    </div>
    <div id="files-box" class="p-1" v-if="selectedFiles.length > 0">
      <div v-for="(file, index) in selectedFiles" class="file-box">
        <div class="selected-file-name ml-1 nowrap">{{ getFileName(file.path) }}</div>
        <div v-if="'image' in file" class="mr-3 selected-file-image-checkbox">
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
    <div id="reply-to-box" class="pl-1 pt-1 has-background-primary-light" v-if="store.replyToMessage !== null">
      <div class="nowrap" id="reply-to-sender-title">
        Reply to <strong>{{ replyToTitle }}</strong>
      </div>
      <div>
        <a href="#" class="has-text-danger mr-3" @click="() => (store.replyToMessage = null)">
          <FontAwesomeIcon :icon="faX" />
        </a>
      </div>
    </div>
    <div id="send-message-box" v-if="store.selectedChat.permissions.can_send_basic_messages">
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
  border-bottom: 1px #eee solid;
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
