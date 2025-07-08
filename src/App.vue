<script setup lang="ts">

import 'bulma';

import { listen, UnlistenFn } from '@tauri-apps/api/event';
import { ref, onBeforeMount, onDeactivated } from "vue";
import { isInitialConfigDone } from "./settings/settings";
import InitialConfiguration from "./settings/InitialConfiguration.vue";
import EncryptionPassword from './EncryptionPassword.vue';
import PhoneNumber from './login/PhoneNumber.vue';
import AuthenticationCode from './login/AuthenticationCode.vue';
import AuthenticationPassword from './login/AuthenticationPassword.vue';
import Main from './Main.vue';

enum MainWindowState {
  LOADING,
  INITIAL_CONFIG,
  GPG_PASSWORD,
  PHONE_NUMBER,
  AUTH_CODE,
  AUTH_PASSWORD,
  LOGGED_IN
}

const state = ref(MainWindowState.LOADING);

let unlisteners: UnlistenFn[] = [];

onBeforeMount(async () => {
  if (await isInitialConfigDone()) {
    state.value = MainWindowState.GPG_PASSWORD;
  } else {
    state.value = MainWindowState.INITIAL_CONFIG;
  }

  unlisteners.push(
    await listen<any>('ask-login-phone-number', () => {
      state.value = MainWindowState.PHONE_NUMBER;
    }),
    await listen<any>('ask-login-code', () => {
      state.value = MainWindowState.AUTH_CODE;
    }),
    await listen<any>('ask-login-password', () => {
      state.value = MainWindowState.AUTH_PASSWORD;
    }),
    await listen<any>('logged-in', () => {
      state.value = MainWindowState.LOGGED_IN;
    }),
  );
});

onDeactivated(() => {
  for (const unlist of unlisteners) {
    unlist();
  }
});

</script>
<template>
  <div v-if="state === MainWindowState.INITIAL_CONFIG">
    <InitialConfiguration />
  </div>
  <div v-else-if="state === MainWindowState.GPG_PASSWORD">
    <EncryptionPassword />
  </div>
  <div v-else-if="state === MainWindowState.PHONE_NUMBER">
    <PhoneNumber />
  </div>
  <div v-else-if="state === MainWindowState.AUTH_CODE">
    <AuthenticationCode />
  </div>
  <div v-else-if="state === MainWindowState.AUTH_PASSWORD">
    <AuthenticationPassword />
  </div>
  <div v-else-if="state === MainWindowState.LOGGED_IN" class="container">
    <Main />
  </div>
</template>
