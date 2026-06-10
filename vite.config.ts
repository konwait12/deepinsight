import vue from '@vitejs/plugin-vue';
import path from 'path';
import {defineConfig, loadEnv} from 'vite';
import tailwindcss from '@tailwindcss/vite';

export default defineConfig(({mode}) => {
  const projectRoot = __dirname;
  const env = loadEnv(mode, projectRoot, '');
  return {
    root: projectRoot,
    envDir: projectRoot,
    plugins: [vue(), tailwindcss()],
    define: {
      'import.meta.env.VITE_API_BASE_URL': JSON.stringify(env.VITE_API_BASE_URL || ''),
    },
    resolve: {
      alias: {
        '@': path.resolve(projectRoot, './src'),
      },
    },
    server: {
      port: 5173,
      host: '0.0.0.0',
      hmr: process.env.DISABLE_HMR !== 'true',
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
        '/uploads': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        },
      },
    },
    build: {
      rollupOptions: {
        output: {
          manualChunks(id) {
            if (!id.includes('node_modules')) {
              return undefined;
            }

            if (id.includes('echarts') || id.includes('zrender')) {
              return 'charts';
            }
            if (id.includes('@antv')) {
              return 'antv';
            }
            if (id.includes('three')) {
              return 'three';
            }
            if (id.includes('element-plus') || id.includes('@element-plus')) {
              return 'element-plus';
            }
            if (id.includes('markdown-it') || id.includes('highlight.js')) {
              return 'markdown';
            }
            if (id.includes('@google/genai')) {
              return 'ai-sdk';
            }
            if (id.includes('gsap') || id.includes('motion')) {
              return 'motion-vendor';
            }
            if (id.includes('vue') || id.includes('pinia')) {
              return 'vue-vendor';
            }

            return 'vendor';
          },
        },
      },
    },
  };
});
