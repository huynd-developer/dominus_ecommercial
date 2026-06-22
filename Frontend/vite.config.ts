import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

// 1. Nhúng thêm các công cụ tự động nhận diện Element Plus
import AutoImport from 'unplugin-auto-import/vite'
import Components from 'unplugin-vue-components/vite'
import { ElementPlusResolver } from 'unplugin-vue-components/resolvers'

// https://vite.dev/config/
export default defineConfig({
  plugins: [
    vue(),
    vueDevTools(),
    
    // 2. Cấu hình tự động import các hàm/API (ví dụ: ElMessage, ElMessageBox...)
    AutoImport({
      resolvers: [ElementPlusResolver()],
    }),
    
    // 3. Cấu hình tự động import các Thẻ giao diện (ví dụ: <el-input>, <el-button>...)
    Components({
      resolvers: [ElementPlusResolver()],
    }),
  ],
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
})