<script setup>
// ============================================================
// 登录 + 注册页（游客进入系统的第一个页面）
// ============================================================
// Vue 单文件组件（.vue 文件）的三段式结构，本项目的页面都长这样：
//   <script setup>  逻辑区：数据、事件处理函数（setup 语法糖：顶层的变量/函数模板直接可用）
//   <template>      模板区：页面的 HTML 结构（Vue 的插值 {{ }} 和指令 v-if/v-for/@click 写在这里）
//   <style scoped>  样式区：本组件专属的 CSS（scoped 表示只作用于当前组件，不污染别的页面）
//
// 本页用到的核心知识点：
//   ref()        响应式变量：.value 变化时页面自动更新（如 mode 控制登录/注册页签切换）
//   reactive()   响应式对象：把整个表单包起来，输入框 v-model 双向绑定到它的属性上
//   v-model      双向绑定：输入框里打字 → form.username 自动变；代码里改它 → 输入框内容也变
//   @click       事件绑定：点击按钮时调用对应函数
import { reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import request from '../api/request'
import { setLogin } from '../api/auth'

// useRouter：拿到路由器实例，可以编程式跳转页面（router.push('/xxx')）
const router = useRouter()
const mode = ref('login')   // 当前页签：login 登录 / register 注册

// 登录表单数据（reactive 对象：直接 form.username 读写，不用 .value）
const loginForm = reactive({ username: '', password: '' })
// 注册表单数据：企业注册时要额外填 companyName/industry/scale
const regForm = reactive({
  role: 'student', username: '', password: '', name: '', phone: '',
  companyName: '', industry: '', scale: '',
})

async function doLogin() {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入用户名和密码')
  // 调后端登录接口（await 等待响应，request 封装里 code!=0 会自动弹错并抛异常，
  // 所以这里不需要 try-catch——失败时下面代码不会执行）
  const data = await request.post('/auth/login', loginForm)
  // 关键：写入响应式 store（内存+sessionStorage），三端布局的菜单/用户名立刻生效
  setLogin(data.token, data.user)
  ElMessage.success('登录成功')
  // 按角色跳转到对应端的首页——系统自动识别身份，用户不用选"我是学生还是企业"
  const home = { student: '/student/jobs', company: '/company/jobs', admin: '/admin/audit-company' }
  router.push(home[data.user.role])
}

async function doRegister() {
  if (!regForm.username || !regForm.password) return ElMessage.warning('请输入用户名和密码')
  if (regForm.role === 'company' && !regForm.companyName) return ElMessage.warning('请填写企业名称')
  await request.post('/auth/register', regForm)
  ElMessage.success(regForm.role === 'company' ? '注册成功，等待管理员审核入驻后登录' : '注册成功，请登录')
  mode.value = 'login'
  loginForm.username = regForm.username
}
</script>

<template>
  <div class="login-page">
    <div class="login-card">
      <!-- 左侧品牌区 -->
      <div class="brand-side">
        <div class="brand-logo">实</div>
        <h1>实习招聘及<br/>智能分析系统</h1>
        <p>AI 简历解析 · 人岗匹配评分 · 智能推荐</p>
        <div class="brand-points">
          <div><el-icon><User /></el-icon>学生端：AI 解析简历 · 智能推荐 · 求职问答</div>
          <div><el-icon><Briefcase /></el-icon>企业端：AI 双通道筛选 · 候选人评分排序</div>
          <div><el-icon><MagicStick /></el-icon>大模型深度嵌入招聘全流程</div>
        </div>
      </div>

      <!-- 右侧表单区 -->
      <div class="form-side">
        <el-tabs v-model="mode" stretch>
          <el-tab-pane label="登录" name="login">
            <el-form @keyup.enter="doLogin">
              <el-form-item><el-input v-model="loginForm.username" placeholder="用户名" size="large" /></el-form-item>
              <el-form-item><el-input v-model="loginForm.password" type="password" placeholder="密码" size="large" show-password /></el-form-item>
              <el-button type="primary" size="large" style="width:100%" @click="doLogin">登 录</el-button>
            </el-form>
          </el-tab-pane>

          <el-tab-pane label="注册" name="register">
            <el-form label-width="70px">
              <el-form-item label="身份">
                <el-radio-group v-model="regForm.role">
                  <el-radio value="student">学生</el-radio>
                  <el-radio value="company">企业</el-radio>
                </el-radio-group>
              </el-form-item>
              <el-form-item label="用户名"><el-input v-model="regForm.username" /></el-form-item>
              <el-form-item label="密码"><el-input v-model="regForm.password" type="password" show-password /></el-form-item>
              <el-form-item label="姓名"><el-input v-model="regForm.name" /></el-form-item>
              <template v-if="regForm.role === 'company'">
                <el-form-item label="企业名称"><el-input v-model="regForm.companyName" /></el-form-item>
                <el-form-item label="行业"><el-input v-model="regForm.industry" /></el-form-item>
                <el-form-item label="规模"><el-input v-model="regForm.scale" placeholder="如 100-500人" /></el-form-item>
                <el-alert type="warning" :closable="false" style="margin-bottom:8px"
                  title="企业注册后需等待管理员审核入驻" />
              </template>
              <el-button type="primary" style="width:100%" @click="doRegister">注 册</el-button>
            </el-form>
          </el-tab-pane>
        </el-tabs>
      </div>
    </div>
  </div>
</template>

<style scoped>
.login-page {
  min-height: 100vh; display: flex; align-items: center; justify-content: center;
  background: linear-gradient(135deg, #162235 0%, #1d3a5f 55%, #0e7490 100%);
}
.login-card {
  width: 860px; min-height: 480px; border-radius: 20px; overflow: hidden;
  display: flex; box-shadow: 0 24px 64px rgba(0, 0, 0, .35);
}
.brand-side {
  width: 380px; padding: 48px 36px; color: #fff;
  background: linear-gradient(160deg, #1b6b7a, #162235);
  display: flex; flex-direction: column;
}
.brand-logo {
  width: 52px; height: 52px; border-radius: 14px; background: #37dcf2; color: #162235;
  font-size: 26px; font-weight: bold; display: flex; align-items: center; justify-content: center;
  margin-bottom: 24px;
}
.brand-side h1 { font-size: 26px; line-height: 1.4; margin: 0 0 12px; }
.brand-side p { color: #9fc3cf; font-size: 13px; margin: 0 0 40px; }
.brand-points { display: flex; flex-direction: column; gap: 16px; margin-top: auto; }
.brand-points div { display: flex; align-items: center; gap: 10px; font-size: 13px; color: #d7e6ec; }
.form-side { flex: 1; padding: 40px 44px; background: #fff; }
</style>
