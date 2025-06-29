<script setup lang="ts">

import 'bulma';

import { ref, onBeforeMount } from "vue";
import { isInitialConfigDone } from "./settings/settings";
import InitialConfiguration from "./settings/InitialConfiguration.vue";

const loading = ref(true);
const showInitialConfig = ref(true);

onBeforeMount(async () => {
  showInitialConfig.value = !(await isInitialConfigDone());
  loading.value = false;
});
</script>
<template>
  <div v-if="showInitialConfig">
    <InitialConfiguration />
  </div>
  <div v-else class="container">
    <!-- TODO -->
  </div>
</template>