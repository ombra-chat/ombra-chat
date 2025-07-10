<script setup lang="ts">
import { MessageContent } from '../model';

const props = defineProps<{
  content: MessageContent
}>();
</script>

<template>
  Type <code>{{ props.content['@type'] }}</code> not supported yet.
</template>
