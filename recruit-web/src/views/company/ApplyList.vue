<script setup>
// 企业端：候选人筛选 —— 选岗位 → AI 双通道筛选 → 评分排序 → 邀约
import { onMounted, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const jobs = ref([])
const jobId = ref(null)
const applies = ref([])
// 分页状态：当前页 / 每页条数 / 总条数
const page = ref(1)
const size = ref(10)
const total = ref(0)
// AI 筛选结果是一次性全量返回（按分排序），不走分页；切岗位/翻页时回到普通分页列表
const isFiltered = ref(false)
const filtering = ref(false)
const drawerVisible = ref(false)
const resumeText = ref('')
const candidate = ref({})

onMounted(async () => {
  jobs.value = await request.get('/company/jobs')
  const publishing = jobs.value.filter(j => j.status === 1)
  if (publishing.length) {
    // 默认选中"有投递记录"的在招岗位，避免打开页面就是空表（都没有投递则选第一个）
    jobId.value = publishing[0].id
    for (const job of publishing) {
      const data = await request.get('/company/applies', { params: { jobId: job.id, page: 1, size: 10 } })
      if (data.records.length) { jobId.value = job.id; break }
    }
    loadApplies()
  }
})

async function loadApplies() {
  if (!jobId.value) return
  isFiltered.value = false
  // 后端分页：返回 {records: 本页候选人, total: 总条数}
  const data = await request.get('/company/applies', { params: { jobId: jobId.value, page: page.value, size: size.value } })
  applies.value = data.records
  total.value = data.total
}

// 切换岗位时重置回第 1 页，避免上一岗位的页码带过来
function changeJob() {
  page.value = 1
  loadApplies()
}

// AI 筛选：规则过滤 + LLM 评分，返回按分数排序的候选人（全量，不分页）
async function aiFilter() {
  filtering.value = true
  try {
    const data = await request.post('/company/ai-filter/' + jobId.value)
    applies.value = data
    total.value = data.length
    isFiltered.value = true
    ElMessage.success('AI 筛选完成，候选人已按匹配分排序')
  } finally {
    filtering.value = false
  }
}

// 状态流转：被查看 / 面试邀约
async function setStatus(row, status) {
  await request.put(`/company/applies/${row.id}/status`, { status })
  ElMessage.success(status === '面试邀约' ? '已发起面试邀约' : '已标记为被查看')
  loadApplies()
}

// 简历侧边抽屉
function showResume(row) {
  candidate.value = row
  resumeText.value = row.resumeContent || '（该学生未填写简历原文）'
  drawerVisible.value = true
}
</script>

<template>
  <div>
    <el-card style="margin-bottom:16px">
      <div class="page-title" style="margin-bottom:12px">候选人筛选</div>
      <div style="display:flex; gap:12px; align-items:center">
        <span style="color:#5a6072; font-size:14px">选择岗位：</span>
        <el-select v-model="jobId" style="width:300px" placeholder="选择岗位" @change="changeJob">
          <el-option v-for="j in jobs.filter(j => j.status === 1)" :key="j.id" :label="j.title" :value="j.id" />
        </el-select>
        <el-button type="success" :loading="filtering" :disabled="!jobId" @click="aiFilter">
          ⚡ AI 筛选候选人
        </el-button>
        <span style="color:#9096a6; font-size:12px">
          双通道：先按学历/技能硬性过滤，再由大模型对通过者打分排序
        </span>
      </div>
    </el-card>

    <el-card>
      <el-empty v-if="!applies.length" description="该岗位暂无投递，换一个岗位或等待学生投递" />
      <el-table v-else :data="applies" stripe>
        <el-table-column prop="studentName" label="候选人" width="100" />
        <el-table-column prop="school" label="学校" width="150" />
        <el-table-column prop="edu" label="学历" width="80" />
        <el-table-column prop="skills" label="技能" min-width="150" />
        <el-table-column prop="applyTime" label="投递时间" width="170" />
        <el-table-column label="AI 匹配" width="100">
          <template #default="{ row }">
            <el-tag v-if="row.aiScore != null" size="large" effect="dark" round
              :type="row.aiScore >= 80 ? 'success' : row.aiScore >= 60 ? 'warning' : 'danger'">
              {{ row.aiScore }} 分
            </el-tag>
            <span v-else style="color:#bbb">未筛选</span>
          </template>
        </el-table-column>
        <el-table-column prop="aiReason" label="评分理由" min-width="200" show-overflow-tooltip />
        <el-table-column label="投递状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === '面试邀约' ? 'success' : row.status === '被查看' ? 'warning' : 'info'">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="230" fixed="right">
          <template #default="{ row }">
            <el-button size="small" @click="showResume(row)">简历</el-button>
            <el-button size="small" type="warning" plain @click="setStatus(row, '被查看')">已查看</el-button>
            <el-button size="small" type="primary" @click="setStatus(row, '面试邀约')">邀约</el-button>
          </template>
        </el-table-column>
      </el-table>
      <!-- 分页条：AI 筛选结果不分页，只有普通列表才显示 -->
      <div v-if="!isFiltered" style="display:flex; justify-content:center; margin-top:16px">
        <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size"
          v-model:current-page="page" @current-change="loadApplies" />
      </div>
    </el-card>

    <!-- 简历抽屉 -->
    <el-drawer v-model="drawerVisible" :title="(candidate.studentName || '') + ' 的简历'" size="450">
      <el-descriptions :column="1" border style="margin-bottom:16px">
        <el-descriptions-item label="学校">{{ candidate.school || '—' }}</el-descriptions-item>
        <el-descriptions-item label="学历">{{ candidate.edu || '—' }}</el-descriptions-item>
        <el-descriptions-item label="技能">{{ candidate.skills || '—' }}</el-descriptions-item>
      </el-descriptions>
      <div class="page-title" style="font-size:15px; margin-bottom:8px">简历原文</div>
      <p style="white-space:pre-wrap; line-height:1.9; color:#444">{{ resumeText }}</p>
    </el-drawer>
  </div>
</template>
