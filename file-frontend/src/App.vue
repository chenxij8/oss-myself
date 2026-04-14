<script setup lang="ts">
import { onLaunch } from '@dcloudio/uni-app'

// 注意：uni-app 环境中，生命周期应使用 onLaunch
onLaunch(() => {
  console.log('App Launch')
  
  // 获取当前页面路径
  const pages = getCurrentPages()
  const currentPage = pages.length > 0 ? pages[pages.length - 1] : null
  const currentPath = currentPage?.route || ''
  
  // 检查登录状态（登录页不需要检查）
  const token = uni.getStorageSync('access_token')
  const isLoginPage = currentPath.includes('login')
  
  if (!token && !isLoginPage) {
    // 未登录且不在登录页，跳转到登录页
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