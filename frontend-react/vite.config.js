import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// In dev the UI (5173) proxies /api to the Spring Boot backend (8081).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': { target: process.env.VITE_BACKEND_URL || 'http://localhost:8081', changeOrigin: true },
    },
  },
});
