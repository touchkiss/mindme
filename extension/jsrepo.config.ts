import { defineConfig } from 'jsrepo';

export default defineConfig({
    registries: ["https://vue-bits.dev"],
    include: ["src/newtab/components"],
    directory: "src/newtab/components",
    paths: {
        components: "src/newtab/components"
    }
});
