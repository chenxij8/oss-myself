/**
 * 跨端本地文件选择。
 *
 * H5 端不存在 uni.chooseMedia / uni.chooseFile（只有 App、微信小程序支持），
 * 因此 H5 走原生 input[type=file]，其余端走 uni 原生 API。
 * 两条路径统一返回 PickedFile，调用方无需关心平台差异。
 */

export interface PickedFile {
  /** 本地临时路径：H5 为 blob: URL，App/小程序为 uni 返回的临时路径 */
  path: string
  /** 真实文件名 */
  name: string
  /** H5 原始 File 对象，供 FormData 直传；非 H5 为 undefined */
  raw?: File
}

export interface PickOptions {
  /** 最多选择数量，默认 9 */
  count?: number
  /** H5 input 的 accept，如 'image/*,video/*' */
  accept?: string
  /** 原生端优先使用的 API：media（图床）/ file（普通文件） */
  nativeApi?: 'media' | 'file'
  /** 原生端 chooseMedia 的媒体类型 */
  mediaType?: ('image' | 'video')[]
}

/**
 * 是否 H5 端。
 * 不能用 document 判断：App-Vue 运行在 webview 中同样有 DOM，会被误判为 H5。
 * 使用 uni-app 编译期条件编译，H5 与 App/小程序各保留一个分支。
 */
function isH5(): boolean {
  // #ifdef H5
  return true
  // #endif
  // #ifndef H5
  return false
  // #endif
}

export function pickFiles(options: PickOptions): Promise<PickedFile[]> {
  return isH5() ? pickByDom(options) : pickByUni(options)
}

/** H5：原生 file input，可同时选图片与视频 */
function pickByDom(options: PickOptions): Promise<PickedFile[]> {
  return new Promise(resolve => {
    const input = document.createElement('input')
    input.type = 'file'
    input.accept = options.accept || '*/*'
    input.multiple = (options.count ?? 9) > 1
    input.style.display = 'none'

    const cleanup = () => {
      if (input.parentNode) input.parentNode.removeChild(input)
    }
    const finish = (files: File[]) => {
      cleanup()
      resolve(files.map(file => ({
        path: URL.createObjectURL(file),
        name: file.name,
        raw: file
      })))
    }

    input.onchange = () => finish(Array.from(input.files || []).slice(0, options.count ?? 9))
    // 用户取消选择时不要留下悬空 Promise
    input.oncancel = () => { cleanup(); resolve([]) }

    document.body.appendChild(input)
    input.click()
  })
}

/** App / 小程序：优先 chooseMedia，其次 chooseFile，最后 chooseImage 兜底 */
function pickByUni(options: PickOptions): Promise<PickedFile[]> {
  return new Promise((resolve, reject) => {
    const count = options.count ?? 9

    if (options.nativeApi === 'media' && typeof (uni as any).chooseMedia === 'function') {
      (uni as any).chooseMedia({
        count,
        mediaType: options.mediaType ?? ['image', 'video'],
        sourceType: ['album', 'camera'],
        success: (res: any) => resolve((res.tempFiles || []).map((f: any) => ({
          path: f.tempFilePath,
          name: fallbackName(f.tempFilePath, 'media')
        }))),
        fail: (err: any) => reject(new Error(err.errMsg || '选择媒体失败'))
      })
      return
    }

    if (typeof (uni as any).chooseFile === 'function') {
      (uni as any).chooseFile({
        count,
        type: 'file',
        success: (res: any) => resolve((res.tempFilePaths || []).map((p: string) => ({
          path: p,
          name: fallbackName(p, 'file')
        }))),
        fail: (err: any) => reject(new Error(err.errMsg || '选择文件失败'))
      })
      return
    }

    uni.chooseImage({
      count,
      success: res => resolve(res.tempFilePaths.map((p, i) => ({
        path: p,
        name: fallbackName(p, `image_${Date.now()}_${i}`)
      }))),
      fail: err => reject(new Error(err.errMsg || '选择图片失败'))
    })
  })
}

function fallbackName(path: string, prefix: string): string {
  const name = path.substring(path.lastIndexOf('/') + 1)
  return name || `${prefix}_${Date.now()}`
}
