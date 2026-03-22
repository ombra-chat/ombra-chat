<script setup lang="ts">
import { computed, ref } from 'vue';
import { createFolder } from './services/folders';
import { store } from './store';
import { Chat } from './model';
import { faLock, faTrash } from '@fortawesome/free-solid-svg-icons';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';

const chatSearchFilter = ref('')
const newFolderTitle = ref('')
const newFolderChats = ref<Chat[]>([])

const selectableChats = computed<Chat[]>(() => {
  if (chatSearchFilter.value.trim() === '') {
    return []
  }
  const ids = store.chatFoldersMap[0]
  if (!ids) {
    return []
  }
  return ids
    .filter(i => !newFolderChats.value.map(c => c.id).includes(i))
    .map(i => store.chatsMap[i])
    .filter(
      c => c.title.toLocaleLowerCase().includes(chatSearchFilter.value.toLocaleLowerCase())
    )
})

function removeSelectedChat(chat: Chat) {
  newFolderChats.value = newFolderChats.value.filter(c => c.id !== chat.id);
}

const emit = defineEmits(['back']);

async function createNewFolder() {
  await createFolder(newFolderTitle.value, newFolderChats.value.map(c => c.id))
  newFolderTitle.value = ''
  emit('back')
}

function back() {
  emit('back')
}
</script>

<template>
  <div class="field mt-3">
    <div class="control is-expanded">
      <input class="input" type="text" v-model="newFolderTitle" placeholder="New folder title" />
    </div>
  </div>
  <div class="box mb-2">
    <div class="field has-addons">
      <input type="text" class="input" v-model="chatSearchFilter" placeholder="Chat to add..." />
    </div>
    <button type="button" class="button mx-1 my-1 is-primary" @click.prevent="() => newFolderChats.push(chat)"
      v-for="chat in selectableChats" :key="chat.id">
      {{ chat.title }}
      <span class="ml-1" v-if="chat.type['@type'] === 'chatTypeSecret'">
        <FontAwesomeIcon :icon="faLock" />
      </span>
    </button>
  </div>
  <div class="box">
    <button class="button mr-2" v-for="chat in newFolderChats" :key="chat.id" @click="() => removeSelectedChat(chat)">
      {{ chat.title }}
      <span class="ml-1" v-if="chat.type['@type'] === 'chatTypeSecret'">
        <FontAwesomeIcon :icon="faLock" />
      </span>
      <span class="ml-2">
        <FontAwesomeIcon :icon="faTrash" />
      </span>
    </button>
    <p v-if="newFolderChats.length === 0">
      Please select at least one chat
    </p>
  </div>
  <div class="buttons">
    <button type="button" class="button is-link" @click="createNewFolder"
      :disabled="!newFolderTitle || newFolderChats.length === 0">
      Create folder
    </button>
    <button type="button" class="button" @click="back">
      Back
    </button>
  </div>
</template>