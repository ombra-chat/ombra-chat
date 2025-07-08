<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core';
import { ref } from 'vue';

const password = ref('');
const error = ref('');
const disableNextBtn = ref(false);

async function next() {
  error.value = '';
  disableNextBtn.value = true;
  try {
    await invoke('set_authentication_password', { password: password.value });
  } catch (err) {
    disableNextBtn.value = false;
    error.value = err as string;
  }
}
</script>

<template>
  <form @submit.prevent="next" class="m-5">
    <div class="field mt-3">
      <label class="label mb-0" for="password">Authentication password</label>
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
        <button class="button is-link" type="submit" :disabled="disableNextBtn">Next</button>
      </div>
    </div>
  </form>
</template>
