<script setup lang="ts">
import { MessagePhoto } from '../model';

const props = defineProps<{
  content: MessagePhoto
}>();
</script>

<template>
  <p>TODO: picture</p>
  <p>{{ props.content.caption.text }}</p>
</template>
