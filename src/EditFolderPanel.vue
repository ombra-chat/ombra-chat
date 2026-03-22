<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { Chat } from './model'
import { store } from './store'
import { addChatToFolder, removeChatFromFolder, renameFolder } from './services/folders';
import { faLock, faPencil, faTrash, faUndo } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

const props = defineProps<{
  selectedFolderId: number
}>();

const chatSearchFilter = ref('')
const editFolderTitle = ref(false)
const chatFolderTitle = ref('')

const chats = computed<Chat[]>(() => {
  const ids = store.chatFoldersMap[props.selectedFolderId]
  if (!ids) {
    return []
  }
  return ids.map(i => store.chatsMap[i])
})
const chatsToAdd = computed<Chat[]>(() => {
  if (chatSearchFilter.value.trim() === '') {
    return []
  }
  const ids = store.chatFoldersMap[0]
  if (!ids) {
    return []
  }
  const currentChatIds = chats.value.map(c => c.id)
  return ids
    .filter(i => !currentChatIds.includes(i))
    .map(i => store.chatsMap[i])
    .filter(
      c => c.title.toLocaleLowerCase().includes(chatSearchFilter.value.toLocaleLowerCase())
    )
})

async function addChat(chatId: number) {
  await addChatToFolder(chatId, props.selectedFolderId)
}

async function removeChat(chatId: number) {
  await removeChatFromFolder(chatId, props.selectedFolderId)
}

async function rename() {
  if (!chatFolderTitle.value) {
    return
  }
  await renameFolder(props.selectedFolderId, chatFolderTitle.value)
  editFolderTitle.value = false
}

function initChatFolderTitle() {
  chatFolderTitle.value = store.chatFolders.find(f => f.id === props.selectedFolderId)?.name || ''
}

onMounted(() => {
  initChatFolderTitle()
})
</script>

<template>
  <div class="field mt-3 has-addons has-addons-right" v-if="editFolderTitle">
    <div class="control is-expanded">
      <input class="input" type="text" v-model="chatFolderTitle" placeholder="Folder title" />
    </div>
    <div class="control">
      <button class="button pr-2" @click="() => { editFolderTitle = false; initChatFolderTitle() }">
        <FontAwesomeIcon :icon="faUndo" /> &nbsp;
      </button>
    </div>
    <div class="control">
      <button @click="rename" class="button is-link">Rename</button>
    </div>
  </div>
  <div v-else class="my-3 is-size-5">
    <strong>{{ chatFolderTitle }}</strong>
    <button @click="() => (editFolderTitle = true)" class="ml-2">
      <FontAwesomeIcon :icon="faPencil" />
    </button>
  </div>
  <div class="box">
    <div class="field has-addons">
      <input type="text" class="input" v-model="chatSearchFilter" placeholder="Chat to add..." />
    </div>
    <button type="button" class="button mx-1 my-1 is-primary" @click.prevent="() => addChat(chat.id)"
      v-for="chat in chatsToAdd" :key="chat.id">
      {{ chat.title }}
      <span class="ml-1" v-if="chat.type['@type'] === 'chatTypeSecret'">
        <FontAwesomeIcon :icon="faLock" />
      </span>
    </button>
  </div>
  <div class="menu">
    <ul class="menu-list">
      <li v-for="chat in chats" class="nowrap" :key="chat.id">
        <a href="#" @click.prevent="() => { }" class="is-flex is-flex-direction-row">
          <span class="is-flex-grow-1 is-align-self-center">
            {{ chat.title }}
            <span class="ml-1" v-if="chat.type['@type'] === 'chatTypeSecret'">
              <FontAwesomeIcon :icon="faLock" />
            </span>
          </span>
          <span>
            <button class="button is-danger" type="button" @click.stop.prevent="() => removeChat(chat.id)"
              v-if="chats.length > 1">
              <FontAwesomeIcon :icon="faTrash" />
            </button>
          </span>
        </a>
      </li>
    </ul>
  </div>
</template>