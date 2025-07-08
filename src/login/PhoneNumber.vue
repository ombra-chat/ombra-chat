<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core';
import { ref } from 'vue';

const phoneNumber = ref('');
const error = ref('');
const disableNextBtn = ref(false);

async function next() {
  error.value = '';
  disableNextBtn.value = true;
  try {
    await invoke('set_authentication_phone_number', { phoneNumber: phoneNumber.value });
  } catch (err) {
    disableNextBtn.value = false;
    error.value = err as string;
  }
}
</script>

<template>
  <form @submit.prevent="next" class="m-5">
    <div class="field mt-3">
      <label class="label mb-0" for="phone_number">Phone number</label>
      <div class="control">
        <input class="input" type="text" id="phone_number" v-model="phoneNumber" />
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