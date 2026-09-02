<script setup>
// 管理端：企业入驻审核 —— 待审核置顶
import { computed, onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const companies = ref([])

const approvedMap = { 0: { text: '待审核', type: 'warning' }, 1: { text: '已通过', type: 'success' }, 2: { text: '已驳回', type: 'danger' } }
const sorted = computed(() => [...companies.value].sort((a, b) => a.approved - b.approved))
const pendingCount = computed(() => companies.value.filter(c => c.approved === 0).length)

async function load() {
  companies.value = await request.get('/admin/companies')
}

async function audit(row, pass) {
  await ElMessageBox.confirm(
    `确定${pass ? '通过' : '驳回'}「${row.name}」的入驻申请？`, '企业审核', { type: 'warning' }
  )
  await request.put(`/admin/companies/${row.id}/audit`, { pass })
  ElMessage.success(pass ? '已通过，企业可以登录发布了' : '已驳回')
  load()
}

onMounted(load)
</script>

<template>
  <div>
    <div class="page-title" style="margin-bottom:16px">
      企业入驻审核
      <el-badge v-if="pendingCount" :value="pendingCount" type="warning" style="margin-left:4px" />
    </div>
    <el-card>
      <el-table :data="sorted" stripe>
        <el-table-column prop="name" label="企业名称" min-width="160" />
        <el-table-column prop="industry" label="行业" width="120" />
        <el-table-column prop="scale" label="规模" width="120" />
        <el-table-column prop="contact" label="联系人" width="110" />
        <el-table-column prop="createTime" label="申请时间" width="170" />
        <el-table-column label="审核状态" width="100">
          <template #default="{ row }"><el-tag :type="approvedMap[row.approved]?.type">{{ approvedMap[row.approved]?.text }}</el-tag></template>
        </el-table-column>
        <el-table-column label="操作" width="170" fixed="right">
          <template #default="{ row }">
            <template v-if="row.approved === 0">
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
