<script setup lang="ts">
import { faPlay } from '@fortawesome/free-solid-svg-icons';
import { MessageVoiceNote, MessageWithStatus } from '../model';
import { FontAwesomeIcon } from '@fortawesome/vue-fontawesome';
import { computed, nextTick, onMounted, ref } from 'vue';
import { downloadFile } from '../services/files';
import { openPath } from '@tauri-apps/plugin-opener';
import { store } from '../store';

const props = defineProps<{
  message: MessageWithStatus,
  content: MessageVoiceNote
}>();

const downloading = ref(false);

const downloaded = computed(() => props.content.voice_note.voice.local.is_downloading_completed);

async function download() {
  downloading.value = true;
  const file = await downloadFile(props.content.voice_note.voice.id);
  if (file?.local.is_downloading_completed) {
    downloading.value = false;
    props.content.voice_note.voice = file;
  } else {
    await download();
  }
}

async function playAudio() {
  if (!downloaded.value) {
    await download()
  }
  await openPath(`file://${props.content.voice_note.voice.local.path}`);
}

function secondsToHHMMSS(totalSeconds: number) {
  totalSeconds = Math.max(0, Math.floor(Number(totalSeconds) || 0));
  const hours = Math.floor(totalSeconds / 3600);
  const minutes = Math.floor((totalSeconds % 3600) / 60);
  const seconds = totalSeconds % 60;
  const pad = (n: number) => String(n).padStart(2, '0');
  return `${pad(hours)}:${pad(minutes)}:${pad(seconds)}`;
}

onMounted(async () => {
  await nextTick(() => {
    store.messageLoaded(props.message.id);
  });
});
</script>

<template>
  <div class="is-flex">
    <div>
      <button class="button" type="button" aria-label="Play audio" @click="playAudio" :disabled="downloading">
        <FontAwesomeIcon :icon="faPlay" />
      </button>
    </div>
    <div class="ml-3 is-align-content-center">
      {{ secondsToHHMMSS(content.voice_note.duration) }}
    </div>
  </div>
</template>
