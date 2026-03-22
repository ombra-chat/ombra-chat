<script setup lang="ts">
import { ref, watch } from 'vue';
import { store } from './store';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faTrash } from '@fortawesome/free-solid-svg-icons';
import { deleteFolder } from './services/folders';
import CreateFolderPanel from './CreateFolderPanel.vue';
import EditFolderPanel from './EditFolderPanel.vue';

const selectedFolderId = ref<number | null>(null)
const showCreateNewFolder = ref(false)

function closeModal() {
  store.foldersModalActive = false
}

function resetValues() {
  selectedFolderId.value = null
  showCreateNewFolder.value = false
}

watch(() => store.foldersModalActive, () => {
  if (!store.foldersModalActive) {
    resetValues()
  }
})
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.foldersModalActive }" id="folders-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">Folders</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3" v-if="selectedFolderId === null && !showCreateNewFolder">
        <div class="menu">
          <ul class="menu-list">
            <li v-for="folder in store.chatFolders.filter(f => f.id !== 0)" class="nowrap" :key="folder.id">
              <a href="#" @click.prevent="() => (selectedFolderId = folder.id)" class="is-flex is-flex-direction-row">
                <span class="is-flex-grow-1 is-align-self-center">
                  {{ folder.name }}
                </span>
                <span>
                  <button class="button is-danger" type="button" @click.stop.prevent="() => deleteFolder(folder.id)">
                    <FontAwesomeIcon :icon="faTrash" />
                  </button>
                </span>
              </a>
            </li>
          </ul>
        </div>
        <button type="button" class="mt-3 button is-primary" @click="() => (showCreateNewFolder = true)">
          Create new folder
        </button>
      </section>
      <section class="modal-card-body p-3" v-if="showCreateNewFolder">
        <CreateFolderPanel @back="() => (showCreateNewFolder = false)" />
      </section>
      <section class="modal-card-body p-3" v-if="selectedFolderId !== null && !showCreateNewFolder">
        <EditFolderPanel :selected-folder-id="selectedFolderId" />
      </section>
      <footer class="modal-card-foot p-2">
        <div class="buttons">
          <button class="button" @click="resetValues" v-if="selectedFolderId !== null">
            Back
          </button>
        </div>
      </footer>
    </div>
  </div>
</template>