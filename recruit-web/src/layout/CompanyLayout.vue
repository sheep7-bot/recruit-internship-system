<script setup>
// 企业端布局：白色商务顶栏 + 靛蓝主题
// 头部主标题显示公司名称（从 /company/info 拉取），副标题展示 HR 姓名——
// 之前只显示"王经理的工作台"，看不出是哪家公司
import { onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { user, clearLogin } from '../api/auth'
import request from '../api/request'

const route = useRoute()
const router = useRouter()
const companyName = ref('')

// 各公司的 HR 都只看到自己公司的岗位和候选人（后端做了归属校验）
const menus = [
  { path: '/company/jobs', label: '岗位管理', icon: 'Briefcase' },
  { path: '/company/applies', label: '候选人筛选', icon: 'UserFilled' },
]

onMounted(async () => {
  try {
    const info = await request.get('/company/info')
    companyName.value = info?.name || ''
  } catch (e) { /* 拉取失败时退回通用标题，不影响使用 */ }
})

function logout() {
  clearLogin()
  router.push('/login')
}
</script>

<template>
  <div class="company-shell">
    <header class="company-header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-mark">HR</div>
          <div>
            <div class="brand-name">{{ companyName || '企业 HR 工作台' }}</div>
            <div class="brand-sub">HR {{ user?.name }} · 发布岗位 · AI 智能筛选辅助</div>
          </div>
        </div>
        <nav class="nav">
          <router-link v-for="m in menus" :key="m.path" :to="m.path" class="nav-item" :class="{ active: route.path === m.path }">
            <el-icon><component :is="m.icon" /></el-icon>{{ m.label }}
          </router-link>
        </nav>
        <el-button plain round @click="logout">退出登录</el-button>
      </div>
    </header>
    <main class="company-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.company-shell { min-height: 100vh; background: #f4f5fa; }
.company-header {
  background: #fff; border-bottom: 1px solid #e8eaf2; padding: 0;
  position: sticky; top: 0; z-index: 10;
}
.header-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; height: 64px; display: flex; align-items: center; gap: 40px; }
.brand { display: flex; align-items: center; gap: 12px; }
.brand-mark {
  width: 42px; height: 42px; border-radius: 10px; color: #fff; font-weight: bold; font-size: 15px;
  background: linear-gradient(135deg, #4f46e5, #7c3aed);
  display: flex; align-items: center; justify-content: center;
}
.brand-name { font-size: 16px; font-weight: 600; color: #1e2235; }
.brand-sub { font-size: 12px; color: #9096a6; }
.nav { display: flex; gap: 8px; flex: 1; }
.nav-item {
  display: flex; align-items: center; gap: 6px; color: #5a6072; text-decoration: none;
  padding: 9px 18px; border-radius: 8px; font-size: 14px; transition: all .2s;
}
.nav-item:hover { background: #eef0ff; color: #4f46e5; }
.nav-item.active { background: #4f46e5; color: #fff; font-weight: 600; }
.company-main { max-width: 1200px; margin: 0 auto; padding: 24px; }
</style>
