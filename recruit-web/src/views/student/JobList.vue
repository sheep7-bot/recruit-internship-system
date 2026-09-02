<script setup>
// 学生端：岗位广场 —— 搜索头图 + 岗位卡片流（分页）+ AI 智能推荐
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

const filters = reactive({ keyword: '', city: '', type: '' })
const jobs = ref([])
// 分页状态：当前页 / 每页条数 / 总条数（后端返回的 total 驱动分页条）
const page = ref(1)
const size = ref(9)
const total = ref(0)
const detailVisible = ref(false)
const detail = ref({})
const recommendVisible = ref(false)
const recommends = ref([])
const recommendLoading = ref(false)
// 已投递的岗位 id：推荐弹窗里的按钮投过就置灰
const appliedIds = ref([])

async function load() {
  // 后端分页：返回 {records: 本页岗位, total: 总条数}
  const data = await request.get('/student/jobs', { params: { ...filters, page: page.value, size: size.value } })
  jobs.value = data.records
  total.value = data.total
}

// 搜索/切页码都会触发 load；搜索时重置回第 1 页，避免停在已不存在的页码
function search() {
  page.value = 1
  load()
}

function openDetail(row) {
  detail.value = row
  detailVisible.value = true
}

async function apply(row) {
  await request.post('/student/applies', { jobId: row.id })
  appliedIds.value.push(row.id)
  ElMessage.success('投递成功，可在「我的投递」中跟踪状态')
  detailVisible.value = false
}

// AI 智能推荐
async function recommend() {
  recommendLoading.value = true
  recommendVisible.value = true
  try {
    recommends.value = await request.get('/ai/recommend')
  } finally {
    recommendLoading.value = false
  }
}

onMounted(load)
</script>

<template>
  <div>
    <!-- 搜索头图 -->
    <div class="hero">
      <h2>找到你的下一份实习</h2>
      <p>{{ total }} 个在招岗位正在等你</p>
      <div class="search-bar">
        <el-input v-model="filters.keyword" placeholder="搜索岗位名称或技能，如 Java" size="large" style="flex:1" @keyup.enter="search" />
        <el-input v-model="filters.city" placeholder="城市" size="large" style="width:130px" @keyup.enter="search" />
        <el-select v-model="filters.type" placeholder="类型" size="large" style="width:110px" clearable>
          <el-option label="实习" value="实习" /><el-option label="校招" value="校招" />
        </el-select>
        <el-button type="primary" size="large" @click="search">搜索岗位</el-button>
        <el-button size="large" color="#162235" style="color:#37dcf2" :loading="recommendLoading" @click="recommend">✨ AI 智能推荐</el-button>
      </div>
    </div>

    <!-- 岗位卡片流 -->
    <el-empty v-if="!jobs.length" description="没有符合条件的岗位，换个关键词试试" />
    <el-row :gutter="16">
      <el-col v-for="job in jobs" :key="job.id" :span="8" style="margin-bottom:16px">
        <el-card class="job-card" shadow="hover">
          <div class="job-head" @click="openDetail(job)">
            <span class="job-title">{{ job.title }}</span>
            <span class="job-salary">{{ job.salary }}</span>
          </div>
          <div class="job-company">{{ job.companyName }} · {{ job.city }}</div>
          <div class="job-tags">
            <el-tag size="small" type="primary" effect="plain">{{ job.type }}</el-tag>
            <el-tag size="small" type="warning" effect="plain">{{ job.eduReq }}</el-tag>
            <el-tag v-for="s in (job.skills || '').split(',').filter(Boolean).slice(0, 3)" :key="s" size="small" effect="plain">{{ s }}</el-tag>
          </div>
          <div class="job-foot">
            <el-button size="small" text type="primary" @click="openDetail(job)">查看详情</el-button>
            <el-button size="small" type="primary" round @click="apply(job)">立即投递</el-button>
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 分页条：页码变化触发 load，后端只返回当前页的岗位 -->
    <div style="display:flex; justify-content:center; margin: 24px 0 8px">
      <el-pagination background layout="prev, pager, next, total" :total="total" :page-size="size"
        v-model:current-page="page" @current-change="load" />
    </div>

    <!-- 岗位详情 -->
    <el-dialog v-model="detailVisible" :title="detail.title" width="560">
      <el-descriptions :column="2" border>
        <el-descriptions-item label="企业">{{ detail.companyName }}</el-descriptions-item>
        <el-descriptions-item label="薪资"><span style="color:#f56c0d; font-weight:bold">{{ detail.salary }}</span></el-descriptions-item>
        <el-descriptions-item label="城市">{{ detail.city }}</el-descriptions-item>
        <el-descriptions-item label="类型">{{ detail.type }}</el-descriptions-item>
        <el-descriptions-item label="技能要求" :span="2">{{ detail.skills }}</el-descriptions-item>
        <el-descriptions-item label="学历要求">{{ detail.eduReq }}</el-descriptions-item>
      </el-descriptions>
      <p style="line-height:1.8; color:#444">{{ detail.description }}</p>
      <template #footer>
        <el-button @click="detailVisible = false">关闭</el-button>
        <el-button type="primary" @click="apply(detail)">投递简历</el-button>
      </template>
    </el-dialog>

    <!-- AI 推荐结果：每条带一键投递 -->
    <el-dialog v-model="recommendVisible" title="✨ AI 为你推荐的岗位" width="640">
      <div v-loading="recommendLoading" style="min-height:120px">
        <el-empty v-if="!recommendLoading && !recommends.length" description="AI 未能给出推荐，请先完善简历" />
        <div v-for="r in recommends" :key="r.id" class="rec-item">
          <div style="display:flex; justify-content:space-between; align-items:center">
            <span style="font-weight:600">{{ r.title }}</span>
            <span style="color:#f56c0d; font-weight:bold">{{ r.salary }}</span>
          </div>
          <div style="color:#666; font-size:13px; margin:4px 0">{{ r.city }} | {{ r.type }} | 要求：{{ r.skills }}</div>
          <div style="display:flex; justify-content:space-between; align-items:center">
            <span style="color:#0891b2; font-size:13px">💡 {{ r.reason }}</span>
            <el-button size="small" type="primary" round :disabled="appliedIds.includes(r.id)" @click="apply(r)">
              {{ appliedIds.includes(r.id) ? '已投递' : '一键投递' }}
            </el-button>
          </div>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<style scoped>
.hero {
  background: linear-gradient(120deg, #2563eb, #0891b2); border-radius: 16px;
  color: #fff; padding: 32px 36px; margin-bottom: 20px;
}
.hero h2 { margin: 0 0 6px; font-size: 26px; }
.hero p { margin: 0 0 20px; opacity: .85; }
.search-bar { display: flex; gap: 10px; }
.job-card { cursor: default; }
.job-head { display: flex; justify-content: space-between; gap: 8px; align-items: baseline; }
.job-title { font-size: 16px; font-weight: 600; color: #1e2235; cursor: pointer; }
.job-title:hover { color: #2563eb; }
.job-salary { color: #f56c0d; font-weight: bold; white-space: nowrap; }
.job-company { color: #7a8194; font-size: 13px; margin: 6px 0 10px; }
.job-tags { display: flex; gap: 6px; flex-wrap: wrap; margin-bottom: 12px; }
.job-foot { display: flex; justify-content: space-between; align-items: center; border-top: 1px dashed #eef0f5; padding-top: 10px; }
.rec-item { border-bottom: 1px solid #f0f2f5; padding: 12px 0; }
.rec-item:last-child { border-bottom: none; }
</style>
