<script setup lang="ts">
import { invoke } from '@tauri-apps/api/core';
import { store } from './store';

async function logout() {
  try {
    await invoke('logout');
  } catch (err) {
    console.error(err as string);
  }
}

function openSettingsModal() {
  store.toggleSettingsModal();
  store.toggleSidebar();
}

function openAboutModal() {
  store.toggleAboutModal();
  store.toggleSidebar();
}
</script>

<template>
  <aside class="menu has-background" id="sidebar" :class="{ expanded: store.sidebarExpanded }">
    <div class="is-clearfix">
      <a class="navbar-burger is-active is-pulled-right mr-0" role="button" aria-label="menu" aria-expanded="false"
        @click="store.toggleSidebar">
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
        <span aria-hidden="true"></span>
      </a>
    </div>
    <ul class="menu-list">
      <li><a href="#" @click="openSettingsModal">Settings</a></li>
      <li><a href="#" @click="logout">Logout</a></li>
      <li><a href="#" @click="openAboutModal">About</a></li>
    </ul>
  </aside>
</template>

<style>
#sidebar {
  width: 250px;
  position: fixed;
  top: 0;
  bottom: 0;
  left: -250px;
  z-index: 1000;
}

#sidebar.expanded {
  left: 0;
}

@media screen and (max-width: 250px) {
  #sidebar {
    max-width: calc(100% - 30px);
    left: calc(-100% + 30px);
  }
}
</style>