<template>
  <div class="layout">
    <aside class="sidebar">
      <div class="brand">
        <div class="brand-logo">F</div>
        <span>文件中心</span>
      </div>
      <nav class="nav">
        <div class="nav-label">用户端</div>
        <RouterLink to="/files" class="nav-item" active-class="active">
          <span class="nav-ico">▦</span> 文件管理
        </RouterLink>
        <RouterLink to="/gallery" class="nav-item" active-class="active">
          <span class="nav-ico">▤</span> 图床
        </RouterLink>
        <RouterLink to="/shares" class="nav-item" active-class="active">
          <span class="nav-ico">⌁</span> 我的分享
        </RouterLink>

        <template v-if="userStore.isAdmin()">
          <div class="nav-label">管理端</div>
          <RouterLink to="/monitor" class="nav-item" active-class="active">
            <span class="nav-ico">◈</span> 系统监控
          </RouterLink>
          <RouterLink to="/users" class="nav-item" active-class="active">
            <span class="nav-ico">♙</span> 用户管理
          </RouterLink>
        </template>
      </nav>
      <div class="sidebar-foot">
        <div class="user-chip">
          <div class="avatar">{{ userStore.username.charAt(0).toUpperCase() || 'U' }}</div>
          <div class="user-info">
            <div class="user-name">{{ userStore.username }}</div>
            <div class="user-role">{{ userStore.isAdmin() ? '管理员' : '普通用户' }}</div>
          </div>
        </div>
        <button class="btn btn-sm logout-btn" @click="handleLogout">退出登录</button>
      </div>
    </aside>

    <div class="main">
      <header class="topbar">
        <div class="breadcrumb">{{ (route.meta.title as string) || '文件中心' }}</div>
        <div class="top-right">
          <span class="role-tag" :class="userStore.isAdmin() ? 'admin' : ''">
            {{ userStore.isAdmin() ? 'ADMIN' : 'USER' }}
          </span>
        </div>
      </header>
      <main class="content">
        <RouterView />
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

function handleLogout() {
  userStore.clear()
  router.push({ name: 'login' })
}
</script>

<style scoped>
.layout { display: flex; height: 100vh; overflow: hidden; }
.sidebar {
  width: 220px; flex-shrink: 0; background: #fff;
  border-right: 1px solid var(--border);
  display: flex; flex-direction: column;
}
.brand { display: flex; align-items: center; gap: 10px; padding: 20px; font-size: 16px; font-weight: 600; border-bottom: 1px solid var(--border); }
.brand-logo { width: 32px; height: 32px; border-radius: 9px; background: var(--primary); color: #fff; display: flex; align-items: center; justify-content: center; font-size: 17px; }
.nav { flex: 1; padding: 12px; overflow-y: auto; }
.nav-label { font-size: 11px; color: var(--text-faint); padding: 14px 12px 6px; letter-spacing: .05em; }
.nav-item {
  display: flex; align-items: center; gap: 10px;
  padding: 10px 12px; margin-bottom: 3px;
  border-radius: 8px; color: var(--text-sub);
  font-size: 14px; transition: all .15s;
}
.nav-item:hover { background: #f8fafc; color: var(--text); }
.nav-item.active { background: var(--primary-light); color: var(--primary); font-weight: 500; }
.nav-ico { width: 18px; text-align: center; font-size: 15px; }
.sidebar-foot { padding: 14px; border-top: 1px solid var(--border); }
.user-chip { display: flex; align-items: center; gap: 10px; margin-bottom: 10px; }
.avatar { width: 34px; height: 34px; border-radius: 50%; background: var(--primary-light); color: var(--primary); display: flex; align-items: center; justify-content: center; font-weight: 600; }
.user-info { min-width: 0; }
.user-name { font-size: 13px; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.user-role { font-size: 11px; color: var(--text-faint); margin-top: 2px; }
.logout-btn { width: 100%; }
.main { flex: 1; display: flex; flex-direction: column; min-width: 0; }
.topbar { height: 60px; background: #fff; border-bottom: 1px solid var(--border); display: flex; align-items: center; justify-content: space-between; padding: 0 24px; flex-shrink: 0; }
.breadcrumb { font-size: 15px; font-weight: 500; }
.top-right { display: flex; align-items: center; gap: 12px; }
.role-tag { font-size: 11px; padding: 3px 8px; border-radius: 10px; background: #f3f4f6; color: var(--text-sub); font-weight: 600; }
.role-tag.admin { background: var(--primary-light); color: var(--primary); }
.content { flex: 1; overflow-y: auto; padding: 24px; }
</style>
