<script setup>
// 学生端：我的简历 —— 文件上传（PDF/TXT）+ 左侧编辑表单 + 右侧 AI 解析说明卡
import * as pdfjsLib from 'pdfjs-dist'
import workerUrl from 'pdfjs-dist/build/pdf.worker.min.mjs?url'
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'

// PDF.js 的 worker：负责在后台线程解析 PDF，不卡页面
pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl

const form = reactive({
  id: null, name: '', school: '', edu: '', age: null,
  skills: '', experience: '', content: '',
})
const parsing = ref(false)
const uploading = ref(false)

onMounted(async () => {
  const resume = await request.get('/student/resume')
  if (resume) Object.assign(form, resume)
})

// 上传简历文件：TXT 直接读文本；PDF 用 pdf.js 在浏览器里提取文字（不需要后端解析）
// 提取成功后自动「保存 → AI 解析」，一步完成信息回填
async function handleFile(uploadFile) {
  const raw = uploadFile.raw
  if (!raw) return
  const name = raw.name.toLowerCase()
  uploading.value = true
  try {
    if (name.endsWith('.txt') || name.endsWith('.md')) {
      form.content = await raw.text()
    } else if (name.endsWith('.pdf')) {
      form.content = await extractPdfText(raw)
    } else {
      return ElMessage.warning('暂时只支持 PDF / TXT 简历文件（Word 简历请先另存为 PDF）')
    }
    if (!form.content.trim()) {
      return ElMessage.warning('文件里没有提取到文字（可能是扫描件/图片版 PDF），请手动粘贴简历内容')
    }
    ElMessage.success(`已提取 ${form.content.length} 字简历内容，正在自动保存并 AI 解析…`)
    // 自动链路：保存简历 → AI 解析回填结构化字段，用户无需再点按钮
    await save()
    await aiParse()
  } catch (e) {
    ElMessage.error('文件解析失败，请换一个文件或手动粘贴简历内容')
  } finally {
    uploading.value = false
  }
}

// 用 pdf.js 提取 PDF 每一页的文字
async function extractPdfText(file) {
  const data = await file.arrayBuffer()
  const pdf = await pdfjsLib.getDocument({ data }).promise
  let text = ''
  for (let i = 1; i <= pdf.numPages; i++) {
    const page = await pdf.getPage(i)
    const content = await page.getTextContent()
    text += content.items.map(item => item.str).join(' ') + '\n'
  }
  return text.trim()
}

// 保存简历（有则更新，无则新建），保存后回读拿到 id
async function save() {
  await request.put('/student/resume', form)
  const resume = await request.get('/student/resume')
  if (resume) Object.assign(form, resume)
  ElMessage.success('简历保存成功')
}

// AI 解析：让大模型从简历原文抽取 学历/技能/经历 回填表单
async function aiParse() {
  if (!form.id) return ElMessage.warning('请先保存简历，再使用 AI 解析')
  parsing.value = true
  try {
    const resume = await request.post('/ai/parse-resume/' + form.id)
    Object.assign(form, resume)
    ElMessage.success('AI 解析完成，已自动回填结构化字段')
  } finally {
    parsing.value = false
  }
}
</script>

<template>
  <div class="page-title" style="margin-bottom:16px">我的简历</div>
  <el-row :gutter="16">
    <!-- 左侧：简历表单 -->
    <el-col :span="17">
      <el-card>
        <!-- 文件上传区：PDF/TXT 提取文字填入简历原文 -->
        <el-upload
          drag
          :auto-upload="false"
          :show-file-list="false"
          accept=".txt,.md,.pdf"
          :on-change="handleFile"
          style="margin-bottom:16px"
        >
          <div v-loading="uploading" style="padding:8px">
            <div style="font-size:26px">📎</div>
            <div style="margin-top:4px">拖拽或点击上传简历文件（PDF / TXT）</div>
            <div style="color:#9096a6; font-size:12px; margin-top:2px">自动提取文字填入下方「简历原文」，Word 简历请先另存为 PDF</div>
          </div>
        </el-upload>

        <el-form label-width="80px">
          <el-row :gutter="16">
            <el-col :span="8"><el-form-item label="姓名"><el-input v-model="form.name" /></el-form-item></el-col>
            <el-col :span="16"><el-form-item label="学校"><el-input v-model="form.school" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="学历">
              <el-select v-model="form.edu" placeholder="选择学历" style="width:100%">
                <el-option v-for="e in ['大专','本科','硕士','博士']" :key="e" :label="e" :value="e" />
              </el-select>
            </el-form-item></el-col>
            <el-col :span="8"><el-form-item label="年龄"><el-input-number v-model="form.age" :min="16" :max="60" controls-position="right" style="width:100%" /></el-form-item></el-col>
            <el-col :span="8"><el-form-item label="技能标签"><el-input v-model="form.skills" placeholder="AI 解析后自动填写" /></el-form-item></el-col>
            <el-col :span="24"><el-form-item label="主要经历">
              <el-input v-model="form.experience" type="textarea" :rows="3" placeholder="AI 解析后自动填写，也可手动编辑" />
            </el-form-item></el-col>
            <el-col :span="24">
              <el-form-item label="简历原文">
                <el-input v-model="form.content" type="textarea" :rows="10"
                  placeholder="把你的完整简历内容粘贴到这里（AI 解析的数据来源）" />
              </el-form-item>
            </el-col>
          </el-row>
          <div style="text-align:right">
            <el-button type="primary" size="large" @click="save">保存简历</el-button>
          </div>
        </el-form>
      </el-card>
    </el-col>

    <!-- 右侧：AI 解析卡 -->
    <el-col :span="7">
      <el-card class="ai-card">
        <div class="ai-icon">✨</div>
        <div class="ai-title">AI 简历解析</div>
        <p class="ai-desc">上传 PDF/TXT 简历或直接粘贴原文并保存后，大模型会自动抽取<strong>学历、技能、经历</strong>填入结构化字段。HR 进行 AI 筛选时，正是用这些字段做匹配评分。</p>
        <el-steps direction="vertical" :active="form.id ? 2 : 0" style="margin:12px 0">
          <el-step title="填写并保存简历原文" />
          <el-step title="点击下方 AI 解析" />
          <el-step title="字段自动回填完成" />
        </el-steps>
        <el-button type="success" size="large" style="width:100%" :loading="parsing" @click="aiParse">
          {{ parsing ? 'AI 解析中…' : '开始 AI 解析' }}
        </el-button>
      </el-card>
    </el-col>
  </el-row>
</template>

<style scoped>
.ai-card { background: linear-gradient(170deg, #f0fbfd, #ffffff); }
.ai-icon { font-size: 34px; }
.ai-title { font-size: 17px; font-weight: 700; color: #1e2235; margin: 6px 0; }
.ai-desc { font-size: 13px; color: #5a6072; line-height: 1.8; }
</style>
