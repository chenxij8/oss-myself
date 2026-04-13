<template>
  <div id="app">
    <router-view />
  </div>
</template>

<script setup lang="ts">
import { onMounted } from 'vue'
import { defineComponent } from 'vue'

// 应用全局配置
onMounted(() => {
  // 检查登录状态
  const token = uni.getStorageSync('access_token')
  if (!token) {
    // 如果没有token，导航到登录页
    uni.navigateTo({
      url: '/pages/login/login'
    })
  }
})
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

body {
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, 'Helvetica Neue', sans-serif;
  font-size: 14px;
  color: #333;
  background-color: #f5f5f5;
}

#app {
  width: 100%;
  height: 100%;
}
</style>
