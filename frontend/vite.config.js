import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'
// https://vite.dev/config/
export default defineConfig({
  plugins: [react(),tailwindcss()],
  // "npm run dev" ile calisirken /api ve /auth isteklerini 8080'deki Spring Boot'a ilet.
  // Docker'da ayni isi nginx yapiyor; ikisinde de frontend ayni adresi kullanabiliyor.
  server: {
    proxy: {
      '/api': 'http://localhost:8080',
      '/auth': 'http://localhost:8080',
    },
  },
})