<script setup lang="ts">
import PageHeader from './components/PageHeader.vue'
import PageFooter from './components/PageFooter.vue'
import AppToast from './components/AppToast.vue'
import AppConfirmDialog from './components/AppConfirmDialog.vue'
</script>

<template>
  <div class="site-shell">
    <PageHeader />
    <router-view v-slot="{ Component, route }">
      <keep-alive>
        <component :is="Component" v-if="route.meta.keepAlive" :key="route.name" />
      </keep-alive>
      <component :is="Component" v-if="!route.meta.keepAlive" :key="route.name" />
    </router-view>
    <PageFooter />
    <AppToast />
    <AppConfirmDialog />
  </div>
</template>
