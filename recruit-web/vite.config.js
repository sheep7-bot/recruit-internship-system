import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'

// Vite 配置：Vue3 插件 + 开发服务器端口 5173
export default defineConfig({
  plugins: [vue()],
  server: { port: 5173 },
})
