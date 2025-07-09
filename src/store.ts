import { reactive } from 'vue'

export const store = reactive({
  sidebarExpanded: false,
  toggleSidebar() {
    this.sidebarExpanded = !this.sidebarExpanded;
  }
});
