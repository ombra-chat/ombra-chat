<script setup lang="ts">
import { getVersion } from '@tauri-apps/api/app';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { faHeart } from '@fortawesome/free-solid-svg-icons';
import { store } from './store';
import { onMounted, ref } from 'vue';

const version = ref('');

function closeModal() {
  store.toggleAboutModal();
}

onMounted(async () => {
  version.value = await getVersion();
})
</script>

<template>
  <div class="modal" :class="{ 'is-active': store.aboutModalActive }" id="message-modal">
    <div class="modal-background" @click="closeModal"></div>
    <div class="modal-card">
      <header class="modal-card-head p-2">
        <p class="modal-card-title mt-1">About</p>
        <button class="delete" aria-label="close" @click="closeModal"></button>
      </header>
      <section class="modal-card-body has-text-centered p-3">
        <figure class="image is-128x128 m-auto">
          <img src="./assets/ombra-chat-logo.svg" />
        </figure>
        <p class="mt-2"><strong>OmbraChat {{ version }}</strong></p>
        <p class="mt-2">
          Made with
          <FontAwesomeIcon :icon="faHeart" />
          by <strong>zonia3000</strong>
        </p>
      </section>
      <footer class="modal-card-foot p-2">
      </footer>
    </div>
  </div>
</template>