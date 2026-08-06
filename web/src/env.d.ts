/// <reference types="vite/client" />
/// <reference types="unplugin-icons/types/vue" />

interface ImportMetaEnv {
  readonly VITE_RECIPE_IMAGE_SEARCH_PREFIX?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
