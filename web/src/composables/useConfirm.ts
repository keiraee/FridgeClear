export interface ConfirmOptions {
  title?: string
  message: string
  confirmLabel?: string
  cancelLabel?: string
  danger?: boolean
}

export function showConfirm(options: ConfirmOptions | string): Promise<boolean> {
  const payload: ConfirmOptions = typeof options === 'string'
    ? { message: options }
    : options

  return new Promise((resolve) => {
    window.dispatchEvent(new CustomEvent('fridgeclear:confirm', {
      detail: { ...payload, resolve },
    }))
  })
}
