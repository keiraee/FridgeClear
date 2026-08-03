<script setup lang="ts">
defineProps<{
  totalItems: number
  expiringSoon: number
  recipesAvailable: number
}>()
</script>

<template>
  <div class="hero-status">
    <h3>冰箱库存概览</h3>
    <div class="status-stat">
      <span class="stat-label">库存食材</span>
      <span class="stat-value">{{ totalItems }} <span class="stat-unit">种</span></span>
    </div>
    <div class="status-stat">
      <span class="stat-label">即将过期</span>
      <span class="stat-value urgent">{{ expiringSoon }} <span class="stat-unit">种</span></span>
    </div>
    <div class="status-stat">
      <span class="stat-label">可做菜谱</span>
      <span class="stat-value ready">{{ recipesAvailable }} <span class="stat-unit">道</span></span>
    </div>
  </div>
</template>
