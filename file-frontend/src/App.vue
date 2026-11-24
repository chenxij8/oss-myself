<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

onLaunch(() => {
  // 未登录时直接进登录页。
  // 注意：不要在 onLaunch 用 getCurrentPages() 判断当前页，
  // 此时页面通常尚未入栈，判断结果恒为 false。
  const token = uni.getStorageSync('access_token')
  if (!token) {
    uni.reLaunch({ url: '/pages/login/login' })
  }
})
</script>

<style>
page {
  background-color: #f5f5f5;
  height: 100%;
}
</style>
