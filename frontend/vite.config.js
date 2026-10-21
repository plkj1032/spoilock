import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

export default defineConfig({
  plugins: [react()],
  server: {
    host: "localhost",
    port: 5173,
    strictPort: true,
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      // 나중에 준호의 WebSocket을 연동할 때 사용할 수 있도록 준비
      "/ws": {
        target: "ws://localhost:8080",
        ws: true,
      },
    },
  },
});
