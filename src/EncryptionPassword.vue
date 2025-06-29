<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core';
import { ref } from 'vue';

const password = ref('');
const error = ref('');

async function next() {
  error.value = '';
  try {
    await invoke('check_gpg_password', { password: password.value });
  } catch (err) {
    const message = err as string;
    if (message === 'unexpected EOF') {
      error.value = 'Invalid password';
    } else {
      error.value = err as string;
    }
  }
}
</script>

<template>
  <form @submit.prevent="next" class="m-5">
    <div class="field mt-3">
      <label class="label mb-0" for="password">GPG password</label>
      <div class="control">
        <input class="input" type="password" id="password" v-model="password" />
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