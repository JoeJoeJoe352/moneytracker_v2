import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],
  server: {
    host:true, // emiatt működik dockeren is, nem csak localhoston
    port: 3000,
    watch: {
      usePolling: true,
      interval: 100
    },
    proxy: {
      // minden /api kezdetű kérés át lesz irányítva a backend szerverre
      '/api': {
        target: 'http://backend:8080', // átírja a portot 8080-ra (backend a docker conténer neve)
        changeOrigin: true, // head-ben is átírja a hostot
        rewrite: (path) => path.replace(/^\/api/, ''), // leveszi az api kezdést
      },
    }
  }
})
