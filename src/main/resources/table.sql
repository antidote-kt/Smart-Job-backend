-- SmartJob数据库表结构

-- 用户表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '用户ID',
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password VARCHAR(64) NOT NULL COMMENT '密码(MD5加密)',
    nickname VARCHAR(50) COMMENT '昵称',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-正常 0-禁用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='用户表';
-- 创建索引
CREATE INDEX idx_username ON user(username);
CREATE INDEX idx_email ON user(email);
CREATE INDEX idx_status ON user(status);

-- 面试表
CREATE TABLE interview (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '面试ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '面试标题',
    position VARCHAR(100) NOT NULL COMMENT '应聘岗位',
    company VARCHAR(100) COMMENT '公司名称',
    interview_type TINYINT DEFAULT 1 COMMENT '面试类型: 1-技术面试 2-HR面试 3-综合面试',
    difficulty_level TINYINT DEFAULT 2 COMMENT '难度等级: 1-简单 2-中等 3-困难',
    status TINYINT DEFAULT 1 COMMENT '状态: 1-已创建 2-进行中 3-已完成 0-已取消',
    total_questions INT DEFAULT 0 COMMENT '总题目数',
    answered_questions INT DEFAULT 0 COMMENT '已回答题目数',
    overall_score DOUBLE(5,2) COMMENT '综合得分(由AI评估)',
    start_time DATETIME COMMENT '开始时间',
    end_time DATETIME COMMENT '结束时间',
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间'
) COMMENT='面试表';

-- 面试题目表
CREATE TABLE interview_question (
    id BIGINT PRIMARY KEY AUTO_INCREMENT COMMENT '题目ID',
    interview_id BIGINT NOT NULL COMMENT '面试ID',
    question_text TEXT NOT NULL COMMENT '题目内容(由AI生成)',
    user_answer TEXT COMMENT '用户答案',
    answer_time DATETIME COMMENT '回答时间',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间'
) COMMENT='面试题目表';

-- 创建索引
CREATE INDEX idx_interview_user_id ON interview(user_id);
CREATE INDEX idx_interview_status ON interview(status);
CREATE INDEX idx_interview_position ON interview(position);
CREATE INDEX idx_question_interview_id ON interview_question(interview_id);

-- 岗位模板表
CREATE TABLE job_requirements (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '岗位名称',
    category VARCHAR(50) NOT NULL COMMENT '岗位分类',
    level VARCHAR(20) NOT NULL COMMENT '岗位级别：初级、中级、高级',
    description TEXT COMMENT '岗位描述',
    requirements JSON COMMENT '岗位要求JSON格式',
    skills JSON COMMENT '技能要求JSON格式',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    is_active TINYINT DEFAULT 1
) COMMENT='岗位模板表';

-- 创建岗位相关索引
CREATE INDEX idx_job_category ON job_requirements(category);
CREATE INDEX idx_job_level ON job_requirements(level);
CREATE INDEX idx_job_active ON job_requirements(is_active);

-- 答案评估表
CREATE TABLE answer_evaluations (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    qa_record_id BIGINT NOT NULL COMMENT '问答记录ID',
    professional_score DOUBLE(5,2) COMMENT '专业性评分',
    logic_score DOUBLE(5,2) COMMENT '逻辑性评分',
    completeness_score DOUBLE(5,2) COMMENT '完整性评分',
    overall_score DOUBLE(5,2) COMMENT '综合评分',
    ai_feedback TEXT COMMENT 'AI详细反馈',
    evaluation_time DATETIME DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='答案评估表';

-- 面试报告表
CREATE TABLE interview_reports (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '面试会话ID',
    overall_score DOUBLE(5,2) COMMENT '综合得分',
    professional_score DOUBLE(5,2) COMMENT '专业性平均分',
    logic_score DOUBLE(5,2) COMMENT '逻辑性平均分',
    completeness_score DOUBLE(5,2) COMMENT '完整性平均分',
    performance_analysis TEXT COMMENT '表现分析',
    skill_assessment TEXT COMMENT '技能评估',
    improvement_suggestions TEXT COMMENT '总体改进建议',
    strong_points TEXT COMMENT '优势',
    weak_points TEXT COMMENT '劣势',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) COMMENT='面试报告表';

-- 创建评估相关索引
CREATE INDEX idx_evaluation_qa_record ON answer_evaluations(qa_record_id);
CREATE INDEX idx_evaluation_time ON answer_evaluations(evaluation_time);
CREATE INDEX idx_report_session ON interview_reports(session_id);