<script setup>
// 管理端：用户管理（启用/禁用账号）
import { onMounted, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import request from '../../api/request'

const users = ref([])
const roleMap = { student: '学生', company: '企业', admin: '管理员' }

async function load() {
  users.value = await request.get('/admin/users')
}

async function toggle(row) {
  const disable = row.status === 1
  await ElMessageBox.confirm(`确定${disable ? '禁用' : '启用'}账号「${row.username}」？`, '提示')
  await request.put(`/admin/users/${row.id}/status`, { status: disable ? 0 : 1 })
  ElMessage.success('操作成功')
  load()
}

onMounted(load)
</script>

<template>
  <el-card>
    <template #header><span>用户管理</span></template>
    <el-table :data="users" stripe>
      <el-table-column prop="id" label="ID" width="70" />
      <el-table-column prop="username" label="用户名" width="140" />
      <el-table-column prop="name" label="姓名" width="120" />
      <el-table-column label="角色" width="100">
        <template #default="{ row }"><el-tag>{{ roleMap[row.role] }}</el-tag></template>
      </el-table-column>
      <el-table-column prop="phone" label="电话" width="140" />
      <el-table-column label="状态" width="100">
        <template #default="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'danger'">{{ row.status === 1 ? '正常' : '已禁用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="120">
        <template #default="{ row }">
          <el-button size="small" :type="row.status === 1 ? 'danger' : 'success'" :disabled="row.role === 'admin'" @click="toggle(row)">
            {{ row.status === 1 ? '禁用' : '启用' }}
          </el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-card>
</template>
