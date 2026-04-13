import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import uni from '@dcloudio/vite-plugin-uni'
import { resolve } from 'path'

// https://vitejs.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    uni(),
  ],
  resolve: {
    alias: {
      '@': resolve(__dirname, './'),
    },
  },
  server: {
    port: 5173,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        rewrite: (path) => path,
      },
    },
  },
  build: {
    target: 'esnext',
    minify: 'terser',
  },
})
