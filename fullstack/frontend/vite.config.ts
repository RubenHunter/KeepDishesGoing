import { defineConfig } from "vite";
import {dirname, resolve} from "node:path";
import {fileURLToPath} from "node:url";

const __dirname = dirname(fileURLToPath(import.meta.url));

export default defineConfig({
  base: "./",
	build: {
		rollupOptions: {
			input: {
				main: resolve(__dirname, 'index.html'),
				thumb: resolve(__dirname, 'html/thumb.html')
			}
		}
		//outDir: "../backend/src/main/resources/static",
	},
	// to deal with deprecation warnings until bootstrap moves to @use with v6(?)
	css: {
		preprocessorOptions: {
			scss: {
				api: 'modern-compiler', // or "modern"
				silenceDeprecations: ['mixed-decls', 'color-functions', 'global-builtin', 'import']
			}
		}
	}
});