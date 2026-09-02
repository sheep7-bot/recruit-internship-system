<script setup>
// 管理端布局：经典管理控制台 —— 左侧深色边栏 + 顶部信息条
// 管理员的核心诉求是"审核与全局数据"，左侧常驻菜单方便在四个功能间快速切换
import { useRoute, useRouter } from 'vue-router'
import { user, clearLogin } from '../api/auth'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/admin/audit-company', label: '企业审核', icon: 'OfficeBuilding' },
  { path: '/admin/audit-job', label: '岗位审核', icon: 'List' },
  { path: '/admin/users', label: '用户管理', icon: 'User' },
  { path: '/admin/stats', label: '数据统计', icon: 'DataAnalysis' },
]

function logout() {
  clearLogin()
  router.push('/login')
}
</script>

<template>
  <div class="admin-shell">
    <aside class="sidebar">
      <div class="side-brand">
        <div class="side-logo">管</div>
        <div>
          <div class="side-title">管理控制台</div>
          <div class="side-sub">实习招聘平台</div>
        </div>
      </div>
      <nav class="side-nav">
        <router-link v-for="m in menus" :key="m.path" :to="m.path" class="side-item" :class="{ active: route.path === m.path }">
          <el-icon :size="16"><component :is="m.icon" /></el-icon>{{ m.label }}
        </router-link>
      </nav>
      <div class="side-footer">
        <el-avatar :size="32" style="background:#37dcf2; color:#162235; font-weight:bold">{{ (user?.name || '管')[0] }}</el-avatar>
        <span class="side-user">{{ user?.name }}</span>
        <el-button text size="small" style="color:#8b93a7" @click="logout">退出</el-button>
      </div>
    </aside>
    <main class="admin-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.admin-shell { min-height: 100vh; display: flex; background: #f0f2f7; }
.sidebar {
  width: 220px; background: #1a2233; color: #cfd6e4; display: flex; flex-direction: column;
  position: sticky; top: 0; height: 100vh;
}
.side-brand { display: flex; align-items: center; gap: 10px; padding: 20px 18px; border-bottom: 1px solid #ffffff14; }
.side-logo {
  width: 38px; height: 38px; border-radius: 10px; background: linear-gradient(135deg, #37dcf2, #2b7fff);
  color: #162235; font-weight: bold; font-size: 18px;
  display: flex; align-items: center; justify-content: center;
}
.side-title { font-size: 15px; font-weight: 600; color: #fff; }
.side-sub { font-size: 11px; color: #7d8598; }
.side-nav { flex: 1; padding: 14px 10px; display: flex; flex-direction: column; gap: 4px; }
.side-item {
  display: flex; align-items: center; gap: 10px; color: #aab3c5; text-decoration: none;
  padding: 11px 14px; border-radius: 8px; font-size: 14px; transition: all .2s;
}
.side-item:hover { background: #ffffff12; color: #fff; }
.side-item.active { background: linear-gradient(90deg, #2b7fff33, transparent); color: #37dcf2; font-weight: 600; border-left: 3px solid #37dcf2; }
.side-footer { display: flex; align-items: center; gap: 8px; padding: 14px 16px; border-top: 1px solid #ffffff14; }
.side-user { flex: 1; font-size: 13px; }
.admin-main { flex: 1; padding: 24px; min-width: 0; }
</style>
