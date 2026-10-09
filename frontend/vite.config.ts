import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// VITE_BASE_PATH: prefixo de implantação atrás de um proxy (ex.: '/quadro-de-carga/').
// Sem a variável, o app roda na raiz ('/'), como em dev local e no compose padrão.
// Proxy de desenvolvimento: '/api' -> API Spring Boot (porta 8086).
export default defineConfig({
  base: process.env.VITE_BASE_PATH ?? '/',
  plugins: [react()],
  server: {
    proxy: {
      '/api': 'http://localhost:8086',
    },
  },
});
