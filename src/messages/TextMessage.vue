<script setup lang="ts">
import { MessageText } from '../model';

const props = defineProps<{
  content: MessageText
}>();
</script>

<template>
{{ props.content.text.text }}
</template>
