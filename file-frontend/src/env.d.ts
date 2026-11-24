/// <reference types="@dcloudio/types" />

/** 环境变量类型声明 */
interface ImportMetaEnv {
  readonly VITE_API_BASE_URL?: string
  readonly VITE_WEB_ORIGIN?: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
