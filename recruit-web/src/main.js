// ============================================================
// 前端入口文件：整个 Vue 应用的"总装配车间"（index.html 里引入的就是它）
// ============================================================
//
// Vue 3 推荐用 <script setup> 写组件、用 createApp 创建应用。
// 一个 Vue 应用的启动流程就三步：创建应用 → 安装插件 → 挂载到页面。
import { createApp } from 'vue'

// Element Plus：基于 Vue 的 UI 组件库（饿了么开源），项目里所有按钮/表格/弹窗/表单
// 都是它的组件，比如 <el-button>、<el-table>、<el-dialog>
import ElementPlus from 'element-plus'
// 组件库的全局样式（没有它组件就没有外观）
import 'element-plus/dist/index.css'
// 配中文语言包：不配的话弹窗按钮会显示英文 OK/Cancel，配置后显示"确定/取消"
import zhCn from 'element-plus/es/locale/lang/zh-cn'
// Element Plus 的图标组件库（菜单、按钮里的小图标）
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

// 根组件：整个页面的骨架（App.vue）
import App from './App.vue'
// 路由：控制"URL 地址 ↔ 页面组件"的对应关系（单页应用的页面切换靠它）
import router from './router'
// 全局自定义样式（卡片圆角、表格表头底色等统一美化）
import './style.css'

// 1. 创建 Vue 应用实例，把 App.vue 作为根组件
const app = createApp(App)

// 2. 安装插件
app.use(ElementPlus, { locale: zhCn })  // 安装 Element Plus（带中文包）
app.use(router)                         // 安装路由（App 里的 <router-view> 才能工作）

// 3. 把全部图标注册成全局组件
// ElementPlusIconsVue 里导出了几百个图标组件，逐个 import 太啰嗦，
// 用 Object.entries 遍历循环注册，之后模板里就能直接用 <component :is="图标名" />
for (const [key, component] of Object.entries(ElementPlusIconsVue)) {
  app.component(key, component)
}

// 4. 挂载：把应用渲染到 index.html 里 id="app" 的那个 <div> 中
// 之后整个页面就是一个 Vue 应用在管理（单页应用 SPA：页面切换不重新加载整个网页）
app.mount('#app')
