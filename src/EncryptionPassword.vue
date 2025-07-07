<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core';
import { ref } from 'vue';

const passphrase = ref('');
const error = ref('');

async function next() {
  error.value = '';
  try {
    await invoke('check_gpg_passphrase', { passphrase: passphrase.value });
    await invoke('start_telegram_client');
  } catch (err) {
    const message = err as string;
    if (message === 'invalid input') {
      error.value = 'Invalid passphrase';
    } else {
      error.value = err as string;
    }
  }
}
</script>

<template>
  <form @submit.prevent="next" class="m-5">
    <div class="field mt-3">
      <label class="label mb-0" for="passphrase">GPG passphrase</label>
      <div class="control">
        <input class="input" type="passphrase" id="passphrase" v-model="passphrase" />
      </div>
    </div>
    <div class="message is-danger mt-3" v-if="error">
      <div class="message-body">
        {{ error }}
      </div>
    </div>
    <div class="field is-grouped mt-3">
      <div class="control mr-auto ml-auto">
        <button class="button is-link" type="submit">Next</button>
      </div>
    </div>
  </form>
</template>