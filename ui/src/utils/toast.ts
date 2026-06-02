type ToastType = 'error' | 'success' | 'info'

let styleInjected = false
let container: HTMLDivElement | null = null
const recentMessages = new Map<string, number>()

function ensureStyle() {
  if (styleInjected) return
  const style = document.createElement('style')
  style.textContent = `
.app-toast-container {
  position: fixed;
  top: 20px;
  right: 20px;
  z-index: 9999;
  display: flex;
  flex-direction: column;
  gap: 10px;
  max-width: min(420px, calc(100vw - 32px));
}
.app-toast {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 12px 14px;
  border-radius: 10px;
  border: 1px solid transparent;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.18);
  background: #ffffff;
  color: #1f2328;
  font-size: 14px;
  line-height: 1.45;
  animation: app-toast-in .18s ease-out;
}
.app-toast-error {
  border-color: #ffc9c9;
  background: #fff5f5;
}
.app-toast-success {
  border-color: #b2f2bb;
  background: #ebfbee;
}
.app-toast-info {
  border-color: #d0ebff;
  background: #e7f5ff;
}
.app-toast-icon {
  flex: 0 0 auto;
  font-size: 16px;
  line-height: 1.2;
}
.app-toast-message {
  flex: 1;
  min-width: 0;
  word-break: break-word;
}
@keyframes app-toast-in {
  from {
    opacity: 0;
    transform: translateY(-8px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
`
  document.head.appendChild(style)
  styleInjected = true
}

function ensureContainer(): HTMLDivElement {
  if (container) return container
  container = document.createElement('div')
  container.className = 'app-toast-container'
  document.body.appendChild(container)
  return container
}

function shouldSkipDuplicate(message: string): boolean {
  const now = Date.now()
  const lastTime = recentMessages.get(message) ?? 0
  recentMessages.set(message, now)
  return now - lastTime < 1500
}

function createToast(type: ToastType, message: string, duration = 3200) {
  if (!message || shouldSkipDuplicate(message)) return
  ensureStyle()
  const wrap = ensureContainer()

  const toast = document.createElement('div')
  toast.className = `app-toast app-toast-${type}`

  const icon = document.createElement('span')
  icon.className = 'app-toast-icon'
  icon.textContent = type === 'error' ? '✖' : type === 'success' ? '✔' : 'i'

  const text = document.createElement('div')
  text.className = 'app-toast-message'
  text.textContent = message

  toast.appendChild(icon)
  toast.appendChild(text)
  wrap.appendChild(toast)

  while (wrap.children.length > 3) {
    wrap.removeChild(wrap.firstChild as Node)
  }

  window.setTimeout(() => {
    toast.remove()
  }, duration)
}

export function toastError(message: string) {
  createToast('error', message)
}

export function toastSuccess(message: string) {
  createToast('success', message)
}

export function toastInfo(message: string) {
  createToast('info', message)
}
