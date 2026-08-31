<template>
  <router-view />
</template>

<script setup lang="ts">
// Root Component
</script>

<style>
/* Global overrides to make Element Plus compatible with Sleek Dark Mode */
.el-card {
  --el-card-bg-color: rgba(30, 41, 59, 0.45) !important;
  --el-card-border-color: rgba(99, 102, 241, 0.15) !important;
  color: #f8fafc !important;
  backdrop-filter: blur(16px);
}
.el-card__header {
  border-bottom: 1px solid rgba(99, 102, 241, 0.15) !important;
}
.el-table {
  --el-table-border-color: rgba(99, 102, 241, 0.1) !important;
}
</style>
