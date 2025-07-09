<script setup lang="ts">
import { computed, ref } from 'vue';

const props = defineProps<{
  values: Array<{ id: number, label: string }>,
  defaultValue: number
}>();
const emit = defineEmits(['change']);

const active = ref(false);
const selectedId = ref(0);
const changed = ref(false);

function toggleDropdown() {
  active.value = !active.value;
}

function selectItem(id: number) {
  selectedId.value = id;
  changed.value = true;
  emit('change', id);
  toggleDropdown();
}

const label = computed(() => {
  const id = changed.value ? selectedId.value : props.defaultValue;
  return props.values.find(v => v.id === id)?.label || '';
});
</script>

<template>
  <div class="dropdown" :class="{ 'is-active': active }">
    <div class="dropdown-trigger">
      <button class="button" aria-haspopup="true" aria-controls="chat-folders-dropdown" @click="toggleDropdown">
        <span>{{ label }}</span>
        <span class="icon is-small">
          <i class="fas fa-angle-down" aria-hidden="true"></i>
        </span>
      </button>
    </div>
    <div class="dropdown-menu" id="chat-folders-dropdown" role="menu">
      <div class="dropdown-content">
        <a href="#" class="dropdown-item" v-for="value in values" :id="`folder-${value.id}`"
          @click="() => selectItem(value.id)">
          {{ value.label }}
        </a>
      </div>
    </div>
  </div>
</template>
