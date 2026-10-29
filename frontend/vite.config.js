import { defineConfig, loadEnv } from "vite";
import react from "@vitejs/plugin-react";
import { fileURLToPath } from "node:url";

// vite.config.js 기준으로 한 단계 위인 spoilock 폴더
const envDir = fileURLToPath(new URL("../", import.meta.url));

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, envDir, "VITE_");

  return {
    plugins: [react()],
    envDir,

    server: {
      port: 5173,
      strictPort: true,

      proxy: {
        "/api": {
          target: env.VITE_API_TARGET,
          changeOrigin: true,
        },

        "/ws": {
          target: env.VITE_WS_TARGET,
          ws: true,
        },

        "/oauth2": {
          target: env.VITE_API_TARGET,
          changeOrigin: false,
        },

        "/login/oauth2": {
          target: env.VITE_API_TARGET,
          changeOrigin: false,
        },
      },
    },
  };
});
