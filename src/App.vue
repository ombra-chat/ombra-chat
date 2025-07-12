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
import { handleChatsUpdates } from './services/chats';
import { invoke } from '@tauri-apps/api/core';
import { store } from './store';

enum MainWindowState {
  LOADING,
  INITIAL_CONFIG,
  PGP_PASSWORD,
  PHONE_NUMBER,
  AUTH_CODE,
  AUTH_PASSWORD,
  LOGGED_IN
}

const state = ref(MainWindowState.LOADING);

let unlisteners: UnlistenFn[] = [];

onBeforeMount(async () => {

  state.value = await getInitialState();

  unlisteners = [
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
    await listen<number>('my-id', (event) => {
      store.myId = event.payload;
    }),
    ...await handleChatsUpdates()
  ];
});

async function getInitialState() {
  if (await isInitialConfigDone()) {
    if (await isLoggedIn()) {
      return MainWindowState.LOGGED_IN;
    }
    return MainWindowState.PGP_PASSWORD;
  }
  return MainWindowState.INITIAL_CONFIG;
}

/**
 * The login state is set by the "logged-in" event. This function is needed to avoid 
 * that the PGP password prompt is displayed again when manually reloading the page 
 * (thing that you usually do only for debugging).
 */
async function isLoggedIn() {
  try {
    return await invoke<boolean>('is_logged_in');
  } catch (err) {
    console.error(err);
  }
  return false;
}

onDeactivated(() => {
  for (const unlist of unlisteners) {
    unlist();
  }
});
</script>

<template>
  <InitialConfiguration v-if="state === MainWindowState.INITIAL_CONFIG" />
  <EncryptionPassword v-else-if="state === MainWindowState.PGP_PASSWORD" />
  <PhoneNumber v-else-if="state === MainWindowState.PHONE_NUMBER" />
  <AuthenticationCode v-else-if="state === MainWindowState.AUTH_CODE" />
  <AuthenticationPassword v-else-if="state === MainWindowState.AUTH_PASSWORD" />
  <Main v-else-if="state === MainWindowState.LOGGED_IN" />
</template>
