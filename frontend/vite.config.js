import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/products': 'http://localhost:8080',
      '/pricing-suggestions': 'http://localhost:8080',
      '/reorder-suggestions': 'http://localhost:8080',
      '/commerce': 'http://localhost:8080'
    }
  }
})
