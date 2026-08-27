<script setup lang="ts">
import { computed } from 'vue';
import { store } from './store';

function closeModal() {
  store.toggleUserModal();
}

const user = computed(() => {
  if (!store.userModalUserId) {
    return undefined;
  }
  return store.getUser(store.userModalUserId)
})
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.userModalUserId !== null }" id="message-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card" v-if="user">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">{{ user.display_text }}</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body p-3">
        <p>
          <strong>Phone number</strong>:
          <code v-if="user.phone_number">+{{ user.phone_number }}</code>
          <span v-else>-</span>
        </p>
        <div v-if="user.usernames.length > 0">
          <strong>Username</strong>:
          <code v-for="username in user.usernames">
            @{{ username }}
          </code>
        </div>
      </section>
      <footer class="modal-card-foot p-2">
      </footer>
    </div>
  </div>
</template>
