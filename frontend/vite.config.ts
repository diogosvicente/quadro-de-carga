import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// Proxy de desenvolvimento: '/api' -> API Spring Boot (porta 8086).
export default defineConfig({
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8086',
    },
  },
});
