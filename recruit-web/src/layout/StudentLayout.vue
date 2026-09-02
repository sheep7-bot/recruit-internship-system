<script setup>
// 学生端布局：清新蓝绿渐变顶栏 + 圆角内容区
// 学生是求职者，界面目标是"轻松找岗位"：视觉明快、导航突出岗位广场和 AI 助手
import { useRoute, useRouter } from 'vue-router'
import { user, clearLogin } from '../api/auth'

const route = useRoute()
const router = useRouter()

const menus = [
  { path: '/student/jobs', label: '岗位广场', icon: 'Search' },
  { path: '/student/applies', label: '我的投递', icon: 'Tickets' },
  { path: '/student/resume', label: '我的简历', icon: 'Document' },
  { path: '/student/chat', label: 'AI 求职助手', icon: 'ChatDotRound' },
]

function logout() {
  clearLogin()
  router.push('/login')
}
</script>

<template>
  <div class="student-shell">
    <header class="student-header">
      <div class="header-inner">
        <div class="brand">
          <div class="brand-logo">实</div>
          <div>
            <div class="brand-name">实习招聘及智能分析系统</div>
            <div class="brand-sub">学生求职中心 · AI 全程辅助</div>
          </div>
        </div>
        <nav class="nav">
          <router-link v-for="m in menus" :key="m.path" :to="m.path" class="nav-item" :class="{ active: route.path === m.path }">
            <el-icon><component :is="m.icon" /></el-icon>{{ m.label }}
          </router-link>
        </nav>
        <div class="user-box">
          <el-avatar :size="34" style="background:#ffffff33; font-weight:bold">{{ (user?.name || '学')[0] }}</el-avatar>
          <span class="user-name">{{ user?.name }}</span>
          <el-button text style="color:#ffffffcc" @click="logout">退出</el-button>
        </div>
      </div>
    </header>
    <main class="student-main">
      <router-view />
    </main>
  </div>
</template>

<style scoped>
.student-shell { min-height: 100vh; background: #f2f6fa; }
.student-header {
  background: linear-gradient(120deg, #2563eb 0%, #0891b2 100%);
  padding: 14px 0;
  box-shadow: 0 2px 12px rgba(37, 99, 235, .25);
  position: sticky; top: 0; z-index: 10;
}
.header-inner { max-width: 1200px; margin: 0 auto; padding: 0 24px; display: flex; align-items: center; gap: 32px; }
.brand { display: flex; align-items: center; gap: 10px; color: #fff; }
.brand-logo {
  width: 40px; height: 40px; border-radius: 12px; background: #ffffff2b;
  display: flex; align-items: center; justify-content: center; font-size: 20px; font-weight: bold;
  border: 1px solid #ffffff45;
}
.brand-name { font-size: 16px; font-weight: 600; }
.brand-sub { font-size: 12px; opacity: .75; }
.nav { display: flex; gap: 6px; flex: 1; }
.nav-item {
  display: flex; align-items: center; gap: 6px; color: #ffffffd9; text-decoration: none;
  padding: 8px 16px; border-radius: 20px; font-size: 14px; transition: all .2s;
}
.nav-item:hover { background: #ffffff1f; color: #fff; }
.nav-item.active { background: #fff; color: #2563eb; font-weight: 600; }
.user-box { display: flex; align-items: center; gap: 10px; }
.user-name { color: #fff; font-size: 14px; }
.student-main { max-width: 1200px; margin: 0 auto; padding: 24px; }
</style>
