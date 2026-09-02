<script setup>
// 企业端：岗位管理 —— 顶部数据概览 + 岗位列表（发布/编辑/下架）
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const jobs = ref([])
const dialogVisible = ref(false)
const form = reactive({ id: null, title: '', city: '', salary: '', type: '实习', skills: '', eduReq: '本科', description: '' })

const statusMap = { 0: { text: '待审核', type: 'warning' }, 1: { text: '发布中', type: 'success' }, 2: { text: '已下架/驳回', type: 'info' } }

// 顶部数据概览
const stats = computed(() => ({
  publishing: jobs.value.filter(j => j.status === 1).length,
  pending: jobs.value.filter(j => j.status === 0).length,
  offline: jobs.value.filter(j => j.status === 2).length,
}))

async function load() {
  jobs.value = await request.get('/company/jobs')
}

function openEdit(row) {
  Object.assign(form, row || { id: null, title: '', city: '', salary: '', type: '实习', skills: '', eduReq: '本科', description: '' })
  dialogVisible.value = true
}

async function save() {
  if (!form.title) return ElMessage.warning('请填写岗位名称')
  if (form.id) await request.put('/company/jobs', form)
  else await request.post('/company/jobs', form)
  ElMessage.success(form.id ? '修改成功' : '发布成功，等待管理员审核通过后上线')
  dialogVisible.value = false
  load()
}

async function offline(row) {
  await ElMessageBox.confirm(`确定下架岗位「${row.title}」？`, '提示', { type: 'warning' })
  await request.put(`/company/jobs/${row.id}/offline`)
  ElMessage.success('已下架')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <!-- 数据概览 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8">
        <div class="stat-chip" style="background:linear-gradient(120deg,#4f46e5,#7c3aed)">
          <div class="stat-num">{{ stats.publishing }}</div>
          <div>在招岗位</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-chip" style="background:linear-gradient(120deg,#f59e0b,#f97316)">
          <div class="stat-num">{{ stats.pending }}</div>
          <div>待平台审核</div>
        </div>
      </el-col>
      <el-col :span="8">
        <div class="stat-chip" style="background:linear-gradient(120deg,#64748b,#94a3b8)">
          <div class="stat-num">{{ stats.offline }}</div>
          <div>已下架/驳回</div>
        </div>
      </el-col>
    </el-row>

    <el-card>
      <template #header>
        <div style="display:flex; justify-content:space-between; align-items:center">
          <div class="page-title">我的岗位</div>
          <el-button type="primary" @click="openEdit(null)">+ 发布新岗位</el-button>
        </div>
      </template>

      <el-table :data="jobs" stripe>
        <el-table-column prop="title" label="岗位名称" min-width="150" />
        <el-table-column prop="city" label="城市" width="90" />
        <el-table-column prop="salary" label="薪资" width="130" />
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column prop="skills" label="技能要求" min-width="150" />
        <el-table-column label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="150">
          <template #default="{ row }">
            <el-button size="small" :disabled="row.status === 2" @click="openEdit(row)">编辑</el-button>
            <el-button size="small" type="danger" plain :disabled="row.status === 2" @click="offline(row)">下架</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="dialogVisible" :title="form.id ? '编辑岗位' : '发布新岗位'" width="560">
      <el-form label-width="80px">
        <el-form-item label="岗位名称"><el-input v-model="form.title" /></el-form-item>
        <el-row :gutter="12">
          <el-col :span="12"><el-form-item label="城市"><el-input v-model="form.city" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="薪资"><el-input v-model="form.salary" placeholder="如 3000-4500元/月" /></el-form-item></el-col>
          <el-col :span="12"><el-form-item label="类型">
            <el-select v-model="form.type" style="width:100%">
              <el-option label="实习" value="实习" /><el-option label="校招" value="校招" />
            </el-select>
          </el-form-item></el-col>
          <el-col :span="12"><el-form-item label="学历要求">
            <el-select v-model="form.eduReq" style="width:100%">
              <el-option v-for="e in ['不限','大专','本科','硕士']" :key="e" :label="e" :value="e" />
            </el-select>
          </el-form-item></el-col>
        </el-row>
        <el-form-item label="技能要求"><el-input v-model="form.skills" placeholder="逗号分隔，如：Java,Spring Boot" /></el-form-item>
        <el-form-item label="岗位描述"><el-input v-model="form.description" type="textarea" :rows="4" /></el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.stat-chip {
  border-radius: 14px; color: #fff; padding: 18px 22px; display: flex; align-items: center; gap: 16px;
  box-shadow: 0 4px 14px rgba(30, 40, 70, .12);
}
.stat-num { font-size: 30px; font-weight: 800; }
</style>
