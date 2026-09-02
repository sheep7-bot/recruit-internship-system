<script setup>
// 学生端：我的投递 —— 投递进度 + AI 评分 + 查看自己投的简历 + AI 投递分析
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const applies = ref([])
// 分页状态：当前页 / 每页条数 / 总条数
const page = ref(1)
const size = ref(10)
const total = ref(0)
const drawerVisible = ref(false)
const current = ref({})
const advice = ref('')
const adviceLoading = ref(false)

const statusType = { 已投递: 'info', 被查看: 'warning', 面试邀约: 'success' }
const statusStep = { 已投递: 1, 被查看: 2, 面试邀约: 3 }

function scoreColor(score) {
  return score >= 80 ? '#22c55e' : score >= 60 ? '#f59e0b' : '#ef4444'
}

async function load() {
  // 后端分页：返回 {records: 本页投递, total: 总条数}
  const data = await request.get('/student/applies', { params: { page: page.value, size: size.value } })
  applies.value = data.records
  total.value = data.total
}

onMounted(load)

// 打开抽屉：查看这次投递投出去的简历
function openResume(row) {
  current.value = row
  advice.value = ''
  drawerVisible.value = true
}

// AI 投递分析：大模型分析这份简历投这个岗位的匹配度和优化建议
async function askAdvice() {
  adviceLoading.value = true
  advice.value = ''
  try {
    advice.value = await request.get('/ai/apply-advice/' + current.value.id)
  } catch (e) {
    advice.value = 'AI 分析暂时不可用，请稍后再试'
  } finally {
    adviceLoading.value = false
  }
}
</script>

<template>
  <div class="page-title" style="margin-bottom:16px">我的投递</div>
  <el-empty v-if="!applies.length" description="还没有投递记录，去岗位广场投出第一份简历吧" />
  <div v-for="a in applies" :key="a.id" class="apply-card">
    <div class="apply-main">
      <div style="display:flex; justify-content:space-between; align-items:baseline">
        <span class="apply-title">{{ a.jobTitle }}</span>
        <span class="apply-salary">{{ a.salary }}</span>
      </div>
      <div style="color:#7a8194; font-size:13px; margin:4px 0 10px">{{ a.city }} · 投递于 {{ a.applyTime }}</div>
      <!-- 投递进度：已投递 → 被查看 → 面试邀约 -->
      <el-steps :active="statusStep[a.status] || 1" align-center style="max-width:420px">
        <el-step title="已投递" />
        <el-step title="被查看" />
        <el-step title="面试邀约" />
      </el-steps>
    </div>
    <div class="apply-side">
      <div class="ai-box">
        <div class="ai-label">AI 匹配评分</div>
        <template v-if="a.aiScore != null">
          <div class="ai-score" :style="{ color: scoreColor(a.aiScore) }">{{ a.aiScore }}<span style="font-size:14px">分</span></div>
          <el-progress :percentage="a.aiScore" :color="scoreColor(a.aiScore)" :show-text="false" />
          <div class="ai-reason">{{ a.aiReason }}</div>
        </template>
        <div v-else class="ai-none">企业尚未进行 AI 筛选</div>
      </div>
      <el-button @click="openResume(a)">查看投递简历</el-button>
      <el-tag :type="statusType[a.status]" size="large" effect="dark" round>{{ a.status }}</el-tag>
    </div>
  </div>

  <!-- 分页条：页码变化触发 load -->
  <div style="display:flex; justify-content:center; margin: 24px 0 8px">
    <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size"
      v-model:current-page="page" @current-change="load" />
  </div>

  <!-- 投递简历抽屉：简历内容 + AI 投递分析 -->
  <el-drawer v-model="drawerVisible" :title="(current.jobTitle || '') + ' · 投递简历'" size="480">
    <div style="display:flex; gap:10px; margin-bottom:12px">
      <el-button type="success" :loading="adviceLoading" @click="askAdvice">🤖 AI 投递分析</el-button>
      <span style="color:#9096a6; font-size:12px; align-self:center">分析这份简历投该岗位的匹配度和优化建议</span>
    </div>
    <!-- AI 分析结果 -->
    <el-card v-if="advice" shadow="never" style="margin-bottom:14px; background:#f0fbfd">
      <p style="white-space:pre-wrap; line-height:1.9; font-size:13px; margin:0">{{ advice }}</p>
    </el-card>
    <div class="page-title" style="font-size:14px; margin-bottom:8px">我投递的简历内容</div>
    <p style="white-space:pre-wrap; line-height:1.9; color:#444; font-size:13px">
      {{ current.resumeContent || '（尚未填写简历内容）' }}
    </p>
  </el-drawer>
</template>

<style scoped>
.apply-card {
  background: #fff; border-radius: 12px; padding: 20px 24px; margin-bottom: 14px;
  display: flex; gap: 24px; justify-content: space-between;
  box-shadow: 0 1px 3px rgba(30,40,70,.06); border: 1px solid #eef1f6;
}
.apply-title { font-size: 17px; font-weight: 600; color: #1e2235; }
.apply-salary { color: #f56c0d; font-weight: bold; }
.apply-side { display: flex; align-items: center; gap: 16px; }
.ai-box { width: 280px; background: #f8fafc; border-radius: 10px; padding: 12px 16px; border: 1px solid #eef1f6; }
.ai-label { font-size: 12px; color: #7a8194; margin-bottom: 2px; }
.ai-score { font-size: 26px; font-weight: 800; line-height: 1.2; }
.ai-reason { font-size: 12px; color: #7a8194; margin-top: 6px; line-height: 1.6; }
.ai-none { color: #b0b7c6; font-size: 13px; padding: 8px 0; }
</style>
