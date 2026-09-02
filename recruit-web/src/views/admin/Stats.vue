<script setup>
// 管理端：数据统计（ECharts 柱状图 + 待办卡片）
import * as echarts from 'echarts'
import { onMounted, ref } from 'vue'
import request from '../../api/request'

const stats = ref({})
const chartRef = ref(null)

onMounted(async () => {
  stats.value = await request.get('/admin/stats')

  // 柱状图：核心业务数据一览
  const chart = echarts.init(chartRef.value)
  chart.setOption({
    title: { text: '平台核心数据', left: 'center' },
    tooltip: {},
    xAxis: { type: 'category', data: ['学生数', '企业数', '在招岗位', '投递总数', '简历数'] },
    yAxis: { type: 'value', minInterval: 1 },
    series: [{
      type: 'bar', barWidth: 48,
      itemStyle: { color: '#1B6B7A' },
      label: { show: true, position: 'top' },
      data: [
        stats.value.studentCount, stats.value.companyCount, stats.value.jobCount,
        stats.value.applyCount, stats.value.resumeCount,
      ],
    }],
  })
})
</script>

<template>
  <div>
    <!-- 待办提醒 -->
    <el-row :gutter="16" style="margin-bottom:16px">
      <el-col :span="8">
        <el-card><el-statistic title="待审核企业" :value="stats.pendingCompanyCount || 0" /></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><el-statistic title="待审核岗位" :value="stats.pendingJobCount || 0" /></el-card>
      </el-col>
      <el-col :span="8">
        <el-card><el-statistic title="投递总数" :value="stats.applyCount || 0" /></el-card>
      </el-col>
    </el-row>
    <el-card>
      <div ref="chartRef" style="height:420px" />
    </el-card>
  </div>
</template>
