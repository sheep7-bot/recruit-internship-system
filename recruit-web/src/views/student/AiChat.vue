<script setup>
// 学生端：AI 求职助手（流式对话）
// 走后端 /ai/chat/stream 的 SSE 流：大模型每生成一段就追加渲染（打字机效果）；
// 流式不可用时自动降级为一次性问答接口 /ai/chat
import { nextTick, ref } from 'vue'
import { ElMessage } from 'element-plus'
import request from '../../api/request'
import { token } from '../../api/auth'

const input = ref('')
const loading = ref(false)     // 正在请求（显示"思考中"动画）
const listRef = ref(null)
const messages = ref([
  { role: 'ai', text: '你好，我是 AI 求职助手 👋 可以问我：简历怎么写、面试怎么准备、怎么选岗位等问题' },
])
const quickAsks = ['实习面试要注意什么？', '简历应该怎么写？', '没有经验怎么找实习？']

async function scrollBottom() {
  await nextTick()
  listRef.value?.scrollTo?.({ top: 99999, behavior: 'smooth' })
}

// 流式问答：fetch 读取 SSE 流，逐段追加到消息里
async function sendStream(question) {
  const resp = await fetch('http://localhost:8081/api/ai/chat/stream', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json', token: token.value },
    body: JSON.stringify({ question }),
  })
  if (!resp.ok || !resp.body) throw new Error('stream unavailable')

  // 推入一条空的 AI 消息，之后边收边追加
  messages.value.push({ role: 'ai', text: '' })
  const aiMsg = messages.value[messages.value.length - 1]

  const reader = resp.body.getReader()
  const decoder = new TextDecoder()
  let buffer = ''
  while (true) {
    const { done, value } = await reader.read()
    if (done) break
    buffer += decoder.decode(value, { stream: true })
    // SSE 格式：每条事件是 "data:内容" + 换行，逐行解析
    const lines = buffer.split('\n')
    buffer = lines.pop()   // 最后一段可能不完整，留到下一轮
    for (const line of lines) {
      if (line.startsWith('data:')) {
        aiMsg.text += line.slice(5)
        scrollBottom()
      }
    }
  }
  // 回答为空说明流中途失败，给个兜底提示
  if (!aiMsg.text.trim()) aiMsg.text = 'AI 服务暂时不可用，请稍后再试'
}

// 一次性问答（降级用）
async function sendFallback(question) {
  const data = await request.post('/ai/chat', { question })
  messages.value.push({ role: 'ai', text: data.answer })
  scrollBottom()
}

async function send(question) {
  const q = (question || input.value).trim()
  if (!q || loading.value) return
  messages.value.push({ role: 'user', text: q })
  input.value = ''
  loading.value = true
  scrollBottom()
  try {
    await sendStream(q)
  } catch (e) {
    try {
      await sendFallback(q)
    } catch (e2) {
      messages.value.push({ role: 'ai', text: '网络异常，请检查后端服务是否启动' })
      scrollBottom()
    }
  } finally {
    loading.value = false
    scrollBottom()
  }
}
</script>

<template>
  <div class="page-title" style="margin-bottom:16px">AI 求职助手</div>
  <el-card :body-style="{ padding: 0 }">
    <!-- 消息区 -->
    <div ref="listRef" class="chat-list">
      <div v-for="(m, i) in messages" :key="i" class="chat-row" :class="m.role">
        <div class="avatar" :class="m.role">{{ m.role === 'user' ? '我' : 'AI' }}</div>
        <div class="bubble" :class="m.role">
          <!-- 思考中动画：三个跳动的点 -->
          <span v-if="loading && i === messages.length - 1 && m.role === 'ai' && !m.text" class="thinking">
            <span class="dot"></span><span class="dot"></span><span class="dot"></span>
          </span>
          <template v-else>{{ m.text }}<span v-if="m.text && i === messages.length - 1 && loading" class="cursor">▍</span></template>
        </div>
      </div>
    </div>
    <!-- 快捷问题 -->
    <div class="quick-asks">
      <el-tag v-for="q in quickAsks" :key="q" style="cursor:pointer" effect="plain" @click="send(q)">{{ q }}</el-tag>
    </div>
    <!-- 输入区 -->
    <div class="chat-input">
      <el-input v-model="input" placeholder="输入你的问题，回车发送" size="large" @keyup.enter="send()" />
      <el-button type="primary" size="large" :loading="loading" @click="send()">发送</el-button>
    </div>
  </el-card>
</template>

<style scoped>
.chat-list { height: 52vh; overflow: auto; padding: 20px 24px; background: #f7f9fc; border-radius: 12px 12px 0 0; }
.chat-row { display: flex; gap: 10px; margin-bottom: 16px; align-items: flex-start; }
.chat-row.user { flex-direction: row-reverse; }
.avatar {
  width: 36px; height: 36px; border-radius: 50%; flex-shrink: 0;
  display: flex; align-items: center; justify-content: center; font-size: 13px; font-weight: bold;
}
.avatar.ai { background: linear-gradient(135deg, #2563eb, #0891b2); color: #fff; }
.avatar.user { background: #e8edf5; color: #4a5568; }
.bubble { background: #fff; padding: 10px 14px; border-radius: 10px; max-width: 68%; line-height: 1.7; font-size: 14px; box-shadow: 0 1px 2px rgba(0,0,0,.05); min-height: 20px; }
.bubble.user { background: #2563eb; color: #fff; }
/* 思考中的跳动点 */
.thinking { display: inline-flex; gap: 5px; padding: 4px 0; }
.thinking .dot {
  width: 7px; height: 7px; border-radius: 50%; background: #94a3b8;
  animation: bounce 1.2s infinite ease-in-out;
}
.thinking .dot:nth-child(2) { animation-delay: .15s; }
.thinking .dot:nth-child(3) { animation-delay: .3s; }
@keyframes bounce {
  0%, 60%, 100% { transform: translateY(0); opacity: .5; }
  30% { transform: translateY(-5px); opacity: 1; }
}
/* 流式输出时的光标 */
.cursor { display: inline-block; color: #2563eb; animation: blink 1s infinite; margin-left: 1px; }
@keyframes blink { 50% { opacity: 0; } }
.quick-asks { display: flex; gap: 8px; padding: 10px 16px; border-top: 1px solid #f0f2f5; }
.chat-input { display: flex; gap: 10px; padding: 12px 16px; border-top: 1px solid #f0f2f5; }
</style>
