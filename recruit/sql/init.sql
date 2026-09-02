-- ============================================================
-- 实习招聘及智能分析系统 数据库初始化脚本
-- 数据库：recruit（MySQL 8）
-- 使用：mysql -uroot -p1234 < sql/init.sql
-- ============================================================

DROP DATABASE IF EXISTS recruit;
CREATE DATABASE recruit DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_general_ci;
USE recruit;

-- 用户表（学生/企业/管理员 三种角色共用一张表）
CREATE TABLE t_user (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  username VARCHAR(50) NOT NULL UNIQUE COMMENT '登录名',
  password VARCHAR(100) NOT NULL COMMENT '密码（BCrypt 加密存储，不是明文！）',
  role VARCHAR(20) NOT NULL COMMENT '角色：student/company/admin',
  name VARCHAR(50) COMMENT '姓名',
  phone VARCHAR(20) COMMENT '电话',
  status INT DEFAULT 1 COMMENT '账号状态：1正常 0禁用',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '用户表';

-- 企业表
CREATE TABLE t_company (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '关联企业账号 t_user.id',
  name VARCHAR(100) NOT NULL COMMENT '企业名称',
  industry VARCHAR(50) COMMENT '行业',
  scale VARCHAR(50) COMMENT '规模',
  contact VARCHAR(50) COMMENT '联系人',
  approved INT DEFAULT 0 COMMENT '入驻审核：0待审核 1通过 2驳回',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '企业表';

-- 学生简历表
CREATE TABLE t_resume (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  user_id BIGINT NOT NULL COMMENT '关联学生 t_user.id',
  name VARCHAR(50) COMMENT '姓名',
  school VARCHAR(100) COMMENT '学校',
  edu VARCHAR(50) COMMENT '学历',
  age INT COMMENT '年龄',
  skills VARCHAR(200) COMMENT '技能（逗号分隔，AI解析后回填）',
  experience VARCHAR(500) COMMENT '经历（AI解析后回填）',
  content TEXT COMMENT '简历全文（学生粘贴或上传的原文）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '学生简历表';

-- 岗位表
CREATE TABLE t_job (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  company_id BIGINT NOT NULL COMMENT '发布企业 t_company.id',
  title VARCHAR(100) NOT NULL COMMENT '岗位名称',
  city VARCHAR(50) COMMENT '城市',
  salary VARCHAR(50) COMMENT '薪资范围',
  type VARCHAR(20) COMMENT '类型：实习/校招',
  skills VARCHAR(200) COMMENT '技能要求（逗号分隔）',
  edu_req VARCHAR(50) COMMENT '学历要求',
  description VARCHAR(500) COMMENT '岗位描述',
  status INT DEFAULT 0 COMMENT '状态：0待审核 1发布中 2已下架/驳回',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT '岗位表';

-- 投递记录表
CREATE TABLE t_apply (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  student_id BIGINT NOT NULL COMMENT '学生 t_user.id',
  job_id BIGINT NOT NULL COMMENT '岗位 t_job.id',
  status VARCHAR(20) DEFAULT '已投递' COMMENT '投递状态：已投递/被查看/面试邀约',
  apply_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '投递时间'
) COMMENT '投递记录表';

-- AI 筛选评分表
CREATE TABLE t_ai_score (
  id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '主键',
  apply_id BIGINT NOT NULL COMMENT '投递记录 t_apply.id',
  score INT COMMENT 'AI 匹配分（0-100）',
  reason VARCHAR(500) COMMENT '评分理由（大模型生成）',
  create_time DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT 'AI 筛选评分表';

-- ============================================================
-- 种子数据（密码统一 1234，方便演示）
-- 注意：密码列存的是 BCrypt 哈希（$2a$10$ 开头），不是明文。
--       "1234" 是 BCryptPasswordEncoder 对这个哈希的原始密码，登录照常输 1234
-- ============================================================

-- 管理员 + 学生 + 企业账号（以下哈希均由 BCrypt 对明文 "1234" 加密生成）
INSERT INTO t_user (username, password, role, name, phone, status) VALUES
('admin',   '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'admin',   '系统管理员', '13800000000', 1),
('student', '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'student', '张小明',     '13800000001', 1),
('company', '$2a$10$dr/ZvxlynbY2TD.ZGsjm1OFcKJNYnEkqIkyTMVM4ne3D7jUazyHpm', 'company', '陈诚',       '13800000002', 1);

-- 企业信息（已通过审核）
INSERT INTO t_company (user_id, name, industry, scale, contact, approved) VALUES
(3, '华清远见科技', 'IT服务', '500-1000人', '陈诚', 1);

-- 学生简历
INSERT INTO t_resume (user_id, name, school, edu, age, skills, experience, content) VALUES
(2, '张小明', '昆明理工大学', '本科', 21, 'Java,Spring Boot,MySQL',
 '2025年在校期间完成学生管理系统课程设计，熟悉 Spring Boot 三层架构开发。',
 '昆明理工大学计算机专业本科在读，熟悉 Java、Spring Boot、MySQL，做过学生管理系统课程设计，学习能力强，每周可实习 4 天。');

-- 岗位（一个在招、一个待审核）
INSERT INTO t_job (company_id, title, city, salary, type, skills, edu_req, description, status) VALUES
(1, 'Java 后端开发实习生', '昆明', '3000-4500元/月', '实习', 'Java,Spring Boot,MySQL', '本科',
 '参与公司 Spring Boot 微服务模块开发，涉及数据库设计与接口开发，有导师带教。', 1),
(1, '前端开发实习生（Vue3）', '昆明', '2800-4000元/月', '实习', 'Vue3,JavaScript,Element Plus', '本科',
 '参与公司前端页面开发，使用 Vue3 + Element Plus 技术栈。', 0);

-- 学生投递在招岗位
INSERT INTO t_apply (student_id, job_id, status) VALUES
(2, 1, '已投递');
