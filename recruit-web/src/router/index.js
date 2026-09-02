import { createRouter, createWebHistory } from 'vue-router'
import { token, role, clearLogin } from '../api/auth'

// 路由表：三端各自嵌套在自己的布局组件下，布局决定页面结构
const routes = [
  { path: '/', redirect: '/login' },
  { path: '/login', component: () => import('../views/Login.vue') },

  // 学生端：清新蓝绿风格，顶部导航
  {
    path: '/student',
    component: () => import('../layout/StudentLayout.vue'),
    meta: { role: 'student' },
    children: [
      { path: '', redirect: '/student/jobs' },
      { path: 'jobs', component: () => import('../views/student/JobList.vue') },
      { path: 'applies', component: () => import('../views/student/MyApplies.vue') },
      { path: 'resume', component: () => import('../views/student/MyResume.vue') },
      { path: 'chat', component: () => import('../views/student/AiChat.vue') },
    ],
  },

  // 企业端：商务靛蓝风格，企业工作台
  {
    path: '/company',
    component: () => import('../layout/CompanyLayout.vue'),
    meta: { role: 'company' },
    children: [
      { path: '', redirect: '/company/jobs' },
      { path: 'jobs', component: () => import('../views/company/MyJobs.vue') },
      { path: 'applies', component: () => import('../views/company/ApplyList.vue') },
    ],
  },

  // 管理端：经典管理控制台，左侧深色边栏
  {
    path: '/admin',
    component: () => import('../layout/AdminLayout.vue'),
    meta: { role: 'admin' },
    children: [
      { path: '', redirect: '/admin/audit-company' },
      { path: 'audit-company', component: () => import('../views/admin/AuditCompany.vue') },
      { path: 'audit-job', component: () => import('../views/admin/AuditJob.vue') },
      { path: 'users', component: () => import('../views/admin/Users.vue') },
      { path: 'stats', component: () => import('../views/admin/Stats.vue') },
    ],
  },
]

const router = createRouter({ history: createWebHistory(), routes })

// 全局路由守卫：未登录去登录页；角色不匹配跳回自己端的首页
router.beforeEach((to) => {
  if (to.path === '/login') {
    clearLogin()   // 回到登录页视为退出
    return true
  }
  if (!token.value) return '/login'
  if (to.meta.role && to.meta.role !== role.value) return '/' + role.value
  return true
})

export default router
