<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

// 注意：uni-app 环境中，生命周期应使用 onLaunch
onLaunch(() => {
  console.log('App Launch')
  
  // 检查登录状态
  const token = uni.getStorageSync('access_token')
  if (!token) {
    // 使用 reLaunch 确保清空路由栈到登录页
    uni.reLaunch({
      url: '/pages/login/login'
    })
  }
})
</script>

<style>
/* 全局样式可以保留，但建议删掉 body 选择器，uni-app 会自动处理 */
/* 如果是 H5 端，可以保留部分通用样式 */
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

page { /* uni-app 替代 body 的写法 */
  background-color: #f5f5f5;
  height: 100%;
}
</style>