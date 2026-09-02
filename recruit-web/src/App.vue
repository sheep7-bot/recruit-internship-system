<template>
  <!-- 根组件只负责渲染：三端各自的布局结构在 layout/ 目录下 -->

  <!-- <router-view> 是 vue-router 的"页面出口"：
       当前 URL 匹配到哪个路由，这里就渲染哪个组件。
       比如地址是 /student/jobs 就渲染 JobList.vue，切到 /admin/stats 就换成 Stats.vue，
       页面切换全程不刷新浏览器（这就是"单页应用"SPA 的含义）。 -->
  <router-view />
</template>
