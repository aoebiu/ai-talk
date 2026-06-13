import { fileURLToPath, URL } from 'node:url'

import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import vueDevTools from 'vite-plugin-vue-devtools'

export default defineConfig(({ mode }) => ({
  plugins: [
    vue(),
    mode === 'development' && vueDevTools(),
  ],
  server: {
    proxy: {
      '/api': {
        target: 'http://localhost:7900',
        changeOrigin: true,
      },
    },
  },
  resolve: {
    alias: {
      '@': fileURLToPath(new URL('./src', import.meta.url))
    },
  },
  build: {
    chunkSizeWarningLimit: 600,
    rollupOptions: {
      output: {
        manualChunks: {
          vue: ['vue', 'vue-router', 'pinia'],
          codemirror: [
            'codemirror',
            'vue-codemirror',
            '@codemirror/lang-javascript',
            '@codemirror/theme-one-dark',
          ],
          marked: ['marked'],
          'highlight.js': ['highlight.js'],
          'js-beautify': ['js-beautify'],
        },
      },
    },
  },
}))
