<script setup>
// 管理端：岗位审核 —— 待审核的排在最前面，一眼看到待办
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const jobs = ref([])

const statusMap = { 0: { text: '待审核', type: 'warning' }, 1: { text: '发布中', type: 'success' }, 2: { text: '已下架/驳回', type: 'info' } }

// 待审核(0)排最前，其次发布中(1)，最后已处理(2)
const sorted = computed(() => [...jobs.value].sort((a, b) => a.status - b.status))
const pendingCount = computed(() => jobs.value.filter(j => j.status === 0).length)

async function load() {
  jobs.value = await request.get('/admin/jobs')
}

async function audit(row, pass) {
  await ElMessageBox.confirm(
    `确定${pass ? '通过并上架' : '驳回'}岗位「${row.title}」？`, '岗位审核', { type: 'warning' }
  )
  await request.put(`/admin/jobs/${row.id}/audit`, { pass })
  ElMessage.success(pass ? '已通过并上架' : '已驳回')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-title" style="margin-bottom:16px">
      岗位审核
      <el-badge v-if="pendingCount" :value="pendingCount" type="warning" style="margin-left:4px" />
    </div>
    <el-card>
      <el-table :data="sorted" stripe>
        <el-table-column prop="title" label="岗位名称" min-width="150" />
        <el-table-column prop="companyName" label="发布企业" min-width="140" />
        <el-table-column prop="city" label="城市" width="90" />
        <el-table-column prop="salary" label="薪资" width="130" />
        <el-table-column prop="type" label="类型" width="80" />
        <el-table-column prop="skills" label="技能要求" min-width="140" show-overflow-tooltip />
        <el-table-column prop="description" label="描述" min-width="180" show-overflow-tooltip />
        <el-table-column label="状态" width="110">
          <template #default="{ row }"><el-tag :type="statusMap[row.status]?.type">{{ statusMap[row.status]?.text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.status === 0">
              <el-button size="small" type="success" @click="audit(row, true)">通过</el-button>
              <el-button size="small" type="danger" @click="audit(row, false)">驳回</el-button>
            </template>
            <span v-else style="color:#bbb; font-size:12px">已处理</span>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>
