import { defineConfig } from "vite";
import { dirname, resolve } from "node:path";
import { fileURLToPath } from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
	base: "./",
	build: {
		rollupOptions: {
			input: {
				main: resolve(__dirname, "index.html"),
				thumb: resolve(__dirname, "html/thumb.html"),
			},
		},
	},
	css: {
		preprocessorOptions: {
			scss: {
				api: "modern-compiler",
				silenceDeprecations: ["mixed-decls", "color-functions", "global-builtin", "import"],
			},
		},
	},
	server: {
		port: 5173,
		proxy: {
			// restaurant-service (Spring app exposing /api/restaurants and /api/restaurants/{id}/menu)
			"/restaurant-api": {
				target: "http://localhost:8080",
				changeOrigin: true,
				rewrite: (p) => p.replace(/^\/restaurant-api/, ""),
			},
			// order-service (this service exposing /api/cart and /api/orders)
			"/order-api": {
				target: "http://localhost:8081",
				changeOrigin: true,
				rewrite: (p) => p.replace(/^\/order-api/, ""),
			},
		},
	},
});