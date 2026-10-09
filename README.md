# 职途（实习招聘及智能分析系统）

面向高校与企业的实习招聘平台：学生投递简历，企业发布岗位并用 AI 筛选候选人，管理员审核企业入驻与岗位。集成 Spring AI + 通义千问，实现简历解析、人岗匹配评分、智能推荐与求职问答。

## 三端功能

- **学生端**：岗位搜索与投递、简历管理（AI 解析回填）、AI 求职助手、智能岗位推荐
- **企业端**：岗位发布与管理、投递筛选（AI 双通道：规则过滤 + 大模型评分排序）、面试邀约
- **管理端**：企业入驻审核、岗位审核、用户管理、数据统计（ECharts）

## AI 能力

- **简历解析**：从简历原文抽取学历、技能、经历并回填结构化字段
- **双通道筛选**：规则先按学历、技能做硬性过滤（不消耗大模型）；通过的候选人才交由大模型打 0–100 分并给出理由，按分排序
- **智能推荐**：大模型根据简历从在招岗位中选出最匹配的 3 个
- **智能问答**：求职主题问答助手，SSE 流式输出

## 技术栈

- 后端：Spring Boot 4.1 · Spring AI（通义千问 qwen-plus）· MyBatis-Plus · MySQL · Redis
- 前端：Vue3 · Vite · Element Plus · ECharts
- 接口文档：Knife4j

## 目录结构

```
├── recruit/       # Spring Boot 后端
├── recruit-web/   # Vue3 前端
└── uml/           # 设计建模图集（用例 / 类 / ER / 时序图）
```

## 快速开始

1. 初始化数据库（脚本会重建 `recruit` 库并写入演示数据）：

   ```bash
   mysql -uroot -p < recruit/sql/init.sql
   ```

2. 配置环境变量 `DASHSCOPE_API_KEY`（或在 `recruit/src/main/resources/application.yml` 中填写）
3. 启动后端（端口 8081）：

   ```bash
   cd recruit
   mvnw spring-boot:run
   ```

4. 启动前端（端口 5173）：

   ```bash
   cd recruit-web
   npm install
   npm run dev
   ```

5. 接口文档：http://localhost:8081/doc.html

更详细的接口说明与演示账号见 `使用指南.md`。
