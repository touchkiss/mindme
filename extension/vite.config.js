import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { resolve } from 'path'

export default defineConfig({
    plugins: [vue()],
    base: './',
    build: {
        outDir: 'dist',
        emptyOutDir: true,
        chunkSizeWarningLimit: 2000,
        rollupOptions: {
            input: {
                popup: resolve(__dirname, 'src/popup/index.html'),
                newtab: resolve(__dirname, 'src/newtab/index.html'),
                background: resolve(__dirname, 'src/background/index.js'),
                content: resolve(__dirname, 'src/content/index.js'),
            },
            output: {
                entryFileNames: '[name].js',
                chunkFileNames: 'assets/[name]-[hash].js',
                assetFileNames: 'assets/[name]-[hash][extname]',
                manualChunks(id) {
                    if (id.includes('node_modules')) {
                        if (id.includes('3d-force-graph') || id.includes('three')) {
                            return 'vendor-3d-graph';
                        }
                        if (id.includes('element-plus')) {
                            return 'vendor-element-plus';
                        }
                        if (id.includes('vue')) {
                            return 'vendor-vue';
                        }
                        // return 'vendor'; // Optional: bundle other vendors together
                    }
                }
            },
        },
    },
})
