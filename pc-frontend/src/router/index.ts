import { createRouter, createWebHashHistory, type RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'
import MainLayout from '@/layouts/MainLayout.vue'

const routes: RouteRecordRaw[] = [
  { path: '/', redirect: '/files' },
  {
    path: '/login',
    name: 'login',
    component: () => import('@/views/LoginView.vue'),
    meta: { public: true }
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('@/views/RegisterView.vue'),
    meta: { public: true }
  },
  {
    path: '/share/access',
    name: 'share-access',
    component: () => import('@/views/ShareAccessView.vue'),
    meta: { public: true }
  },
  {
    path: '/',
    component: MainLayout,
    children: [
      { path: 'files', name: 'files', component: () => import('@/views/FilesView.vue'), meta: { title: '文件管理', section: 'user' } },
      { path: 'gallery', name: 'gallery', component: () => import('@/views/GalleryView.vue'), meta: { title: '图床', section: 'user' } },
      { path: 'shares', name: 'shares', component: () => import('@/views/SharesView.vue'), meta: { title: '我的分享', section: 'user' } },
      { path: 'monitor', name: 'monitor', component: () => import('@/views/MonitorView.vue'), meta: { title: '系统监控', section: 'admin' } },
      { path: 'users', name: 'users', component: () => import('@/views/UsersView.vue'), meta: { title: '用户管理', section: 'admin' } }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/files' }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.beforeEach((to, _from, next) => {
  const userStore = useUserStore()
  const isPublic = to.meta.public === true

  if (!userStore.isLoggedIn()) {
    if (isPublic) return next()
    return next({ name: 'login', query: { redirect: to.fullPath } })
  }

  if (to.name === 'login' || to.name === 'register') {
    return next({ path: '/files' })
  }

  if (to.meta.section === 'admin' && !userStore.isAdmin()) {
    return next({ name: 'files' })
  }

  next()
})

export default router
