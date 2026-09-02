import { ref } from 'vue'

// 登录态响应式 store（sessionStorage 版本）
//
// 为什么用 sessionStorage 而不是 localStorage？
// localStorage 全浏览器共享一份，两个标签页分别登录学生端和企业端时会互相顶掉登录态——
// 表现为"在学生端上传简历，切到企业端看简历，再回学生端页面变成了企业端"。
// sessionStorage 按标签页隔离，两边可以同时在线，符合"学生/企业两端对照着用"的场景。
// 代价：新开标签页需要重新登录（对实训演示影响很小）。
export const token = ref(sessionStorage.getItem('token') || '')
export const role = ref(sessionStorage.getItem('role') || '')
export const user = ref(JSON.parse(sessionStorage.getItem('user') || 'null'))

// 登录成功：内存 + sessionStorage 双写
export function setLogin(t, u) {
  token.value = t
  role.value = u.role
  user.value = u
  sessionStorage.setItem('token', t)
  sessionStorage.setItem('role', u.role)
  sessionStorage.setItem('user', JSON.stringify(u))
}

// 退出登录：内存 + sessionStorage 双清
export function clearLogin() {
  token.value = ''
  role.value = ''
  user.value = null
  sessionStorage.clear()
}

// 从 sessionStorage 重新同步登录态到内存
// 场景：当前标签页内存里的登录态和实际存储不一致（如请求发现 403 后），跳转前先同步
export function syncFromStorage() {
  token.value = sessionStorage.getItem('token') || ''
  role.value = sessionStorage.getItem('role') || ''
  user.value = JSON.parse(sessionStorage.getItem('user') || 'null')
}
