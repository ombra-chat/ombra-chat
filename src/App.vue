<script setup lang="ts">

import 'bulma';

import { ref, onBeforeMount } from "vue";
import { isInitialConfigDone } from "./settings/settings";
import InitialConfiguration from "./settings/InitialConfiguration.vue";
import EncryptionPassword from './EncryptionPassword.vue';

const loading = ref(true);
const showInitialConfig = ref(true);
const passwordInserted = ref(false);

onBeforeMount(async () => {
  showInitialConfig.value = !(await isInitialConfigDone());
  loading.value = false;
});
</script>
<template>
  <div v-if="showInitialConfig">
    <InitialConfiguration />
  </div>
  <div v-else-if="!passwordInserted">
    <EncryptionPassword />
  </div>
  <div v-else class="container">
    <!-- TODO -->
  </div>
</template>