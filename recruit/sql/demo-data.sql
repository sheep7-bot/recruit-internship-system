-- ============================================================
-- 多公司演示数据（增量脚本，不删除现有数据）
-- 用途：让系统里有 3 家公司、多名学生、跨公司投递，演示数据隔离
-- 使用：mysql -uroot -p1234 --default-character-set=utf8mb4 < sql/demo-data.sql
-- ============================================================
USE recruit;

-- ── 新增企业账号与档案 ──
INSERT INTO t_user (username, password, role, name, phone, status) VALUES
('company2', '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'company', '王经理', '13800000003', 1),
('company3', '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'company', '刘晓',   '13800000004', 1),
('zhangwei', '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'student', '张伟',   '13800000005', 1),
('lisi',     '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'student', '李思',   '13800000006', 1);

-- 云智科技：审核通过；星创互联：待审核（给管理员留一条待办）
INSERT INTO t_company (user_id, name, industry, scale, contact, approved)
SELECT u.id, '昆明云智科技', '软件研发', '100-500人', '王经理', 1 FROM t_user u WHERE u.username = 'company2';
INSERT INTO t_company (user_id, name, industry, scale, contact, approved)
SELECT u.id, '星创互联', '互联网', '50-150人', '刘晓', 0 FROM t_user u WHERE u.username = 'company3';

-- ── 新增学生简历 ──
INSERT INTO t_resume (user_id, name, school, edu, age, skills, experience, content)
SELECT u.id, '张伟', '云南大学', '本科', 22, 'Java,Spring Boot,MySQL,Redis',
 '做过校园论坛后端开发，熟悉 MySQL 索引优化，使用 Redis 做过热点缓存。',
 '云南大学计算机专业本科，熟悉 Java、Spring Boot、MySQL、Redis，有校园论坛项目经验，可立即到岗实习。'
FROM t_user u WHERE u.username = 'zhangwei';

INSERT INTO t_resume (user_id, name, school, edu, age, skills, experience, content)
SELECT u.id, '李思', '昆明理工大学', '本科', 21, '需求分析,原型设计,用户调研',
 '在社团负责活动策划，会用 Axure 画原型，做过 500 人问卷的用户调研项目。',
 '昆明理工大学工商管理专业本科，对产品岗位兴趣浓厚，会 Axure 原型设计和用户调研，每周可实习 4 天。'
FROM t_user u WHERE u.username = 'lisi';

-- ── 新增岗位 ──
INSERT INTO t_job (company_id, title, city, salary, type, skills, edu_req, description, status)
SELECT c.id, 'Java 开发工程师（校招）', '昆明', '6000-9000元/月', '校招', 'Java,Spring Boot,MySQL,Redis', '本科',
 '参与企业级 SaaS 平台后端开发，接触微服务与高并发场景，有导师带教。', 1
FROM t_company c WHERE c.name = '昆明云智科技';

INSERT INTO t_job (company_id, title, city, salary, type, skills, edu_req, description, status)
SELECT c.id, '产品经理实习生', '昆明', '2500-3500元/月', '实习', '需求分析,原型设计,用户调研', '本科',
 '协助产品经理完成需求调研、原型设计与竞品分析。', 1
FROM t_company c WHERE c.name = '昆明云智科技';

INSERT INTO t_job (company_id, title, city, salary, type, skills, edu_req, description, status)
SELECT c.id, 'Vue 前端实习生', '成都', '2500-4000元/月', '实习', 'Vue3,JavaScript,CSS', '大专',
 '参与公司 SaaS 产品前端页面开发，使用 Vue3 + Element Plus。', 0
FROM t_company c WHERE c.name = '星创互联';

-- ── 跨公司投递记录 ──
INSERT INTO t_apply (student_id, job_id, status)
SELECT u.id, j.id, '已投递' FROM t_user u, t_job j
WHERE u.username = 'zhangwei' AND j.title = 'Java 开发工程师（校招）';

INSERT INTO t_apply (student_id, job_id, status)
SELECT u.id, j.id, '被查看' FROM t_user u, t_job j
WHERE u.username = 'lisi' AND j.title = '产品经理实习生';

-- 李思投递华清的岗位：用来演示华清 HR 看到"其他学校/专业方向"的候选人被规则筛掉
INSERT INTO t_apply (student_id, job_id, status)
SELECT u.id, j.id, '已投递' FROM t_user u, t_job j
WHERE u.username = 'lisi' AND j.title = 'Java 后端开发实习生';
