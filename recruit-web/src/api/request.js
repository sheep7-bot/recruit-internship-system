import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'
import { token, role, syncFromStorage, clearLogin } from './auth'

// ============================================================
// axios 统一封装：全项目所有后端请求都从这里发出
// ============================================================
// 不封装的后果：每个页面都要写完整的后端地址、手动带 token、手动判断成功失败——
// 重复代码一大堆。封装成"请求拦截器 + 响应拦截器"后，页面代码只关心业务本身。
//
// baseURL：所有请求的地址前缀。页面里写 request.get('/student/jobs')，
// 实际请求的是 http://localhost:8081/api/student/jobs
// timeout：超过 120 秒没响应就报错（AI 功能比较慢，所以设得比较长）
const request = axios.create({ baseURL: 'http://localhost:8081/api', timeout: 120000 })

// ── 请求拦截器：每个请求发出之前都会经过这里 ──────────────────
// 作用：自动把登录凭证（token）放进请求头。登录后后端发了 token，
// 前端存起来，之后每次请求都带上，后端拦截器靠它识别"你是谁"。
request.interceptors.request.use((config) => {
  if (token.value) config.headers.token = token.value
  return config   // return 后请求才真正发出
})

// ── 响应拦截器：每个响应回来之后都会经过这里 ──────────────────
// 作用：统一处理"成功/失败"。后端返回格式固定为 {code, msg, data}：
//   code=0  成功 → 直接把 data 返回给调用页面（页面拿到的就是纯数据）
//   code!=0 失败 → 弹出错误提示，并把 Promise 标记为失败（页面里的 await 会抛异常）
request.interceptors.response.use(
  (resp) => {
    const res = resp.data
    if (res.code !== 0) {
      ElMessage.error(res.msg || '操作失败')
      // 登录过期 → 踢回登录页
      if (res.msg && res.msg.includes('未登录')) {
        clearLogin()
        router.push('/login')
      }
      // 角色不匹配（如学生号打开了企业页面）→ 同步登录态后自动送回该角色自己的首页，
      // 而不是停在报错页面让用户以为账号不能用
      if (res.msg && res.msg.includes('没有权限')) {
        syncFromStorage()
        const home = { student: '/student/jobs', company: '/company/jobs', admin: '/admin/audit-company' }
        const target = home[role.value]
        if (target && router.currentRoute.value.path !== target) {
          ElMessage.info('当前账号无权访问该页面，已为你返回对应工作台')
          router.push(target)
        }
      }
      return Promise.reject(new Error(res.msg))
    }
    return res.data   // ★ 页面里 await request.get(...) 拿到的就是这个 data
  },
  (error) => {
    // 走到这里说明 HTTP 请求本身失败了（后端没启动、断网、超时……）
    ElMessage.error('网络异常，请检查后端服务是否启动')
    return Promise.reject(error)
  }
)

export default request
