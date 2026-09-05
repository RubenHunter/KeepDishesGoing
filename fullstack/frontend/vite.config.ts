import { defineConfig } from "vite";

export default defineConfig({
	server: {
		port: 5173,
		proxy: {
			// restaurant-service (Spring Boot :8080)
			"/restaurant-api": {
				target: "http://localhost:8080",
				changeOrigin: true,
				rewrite: (p) => p.replace(/^\/restaurant-api/, ""),
			},
			// order-service (Spring Boot :8081)
			"/order-api": {
				target: "http://localhost:8081",
				changeOrigin: true,
				rewrite: (p) => p.replace(/^\/order-api/, ""),
			},
			// delivery-service (Spring Boot :8082)
			"/delivery-api": {
				target: "http://localhost:8082",
				changeOrigin: true,
				rewrite: (p) => p.replace(/^\/delivery-api/, ""),
			},
		},
	},
});
