-- 职位模块 + 多角色表（依赖已有 users 表，见 schema-users.sql）
-- 管理员账号请在业务库中插入 users + user_roles(ADMIN)，或使用 local profile 的演示数据（见 application-local.yml 说明）。
-- MySQL 8+ / utf8mb4

-- 账号拥有的角色（注册时写入求职方或招聘方；管理员由运维插入或本地 seed）
CREATE TABLE IF NOT EXISTS user_roles (
    id       BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id  BIGINT       NOT NULL COMMENT '用户 id',
    role     VARCHAR(32)  NOT NULL COMMENT 'SEEKER / RECRUITER / ADMIN',
    CONSTRAINT fk_user_roles_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_user_roles_user_role (user_id, role)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户角色（多选）';

-- 职位类别（管理员维护）
CREATE TABLE IF NOT EXISTS job_categories (
    id          BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(128) NOT NULL COMMENT '类别名称',
    description VARCHAR(512) NULL COMMENT '说明',
    sort_order  INT          NOT NULL DEFAULT 0 COMMENT '排序，越小越靠前',
    created_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    UNIQUE KEY uk_job_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位类别';

-- 职位（招聘方发布；管理员可全量维护）
CREATE TABLE IF NOT EXISTS jobs (
    id                 BIGINT        NOT NULL AUTO_INCREMENT PRIMARY KEY,
    recruiter_user_id  BIGINT        NOT NULL COMMENT '发布人（招聘方用户）',
    category_id        BIGINT        NULL COMMENT '职位类别',
    title              VARCHAR(255)  NOT NULL COMMENT '职位标题',
    company_name       VARCHAR(255)  NOT NULL COMMENT '公司/团队名称',
    description        TEXT          NOT NULL COMMENT '职位描述 JD',
    salary_min         INT           NULL COMMENT '月薪下限（元），可空',
    salary_max         INT           NULL COMMENT '月薪上限（元），可空',
    city               VARCHAR(128)  NULL COMMENT '工作城市',
    work_type          VARCHAR(32)   NULL COMMENT 'FULL_TIME/PART_TIME/CONTRACT/INTERNSHIP 等',
    status             VARCHAR(32)   NOT NULL DEFAULT 'DRAFT' COMMENT 'DRAFT/PUBLISHED/WITHDRAWN',
    published_at       DATETIME(6)   NULL COMMENT '首次发布时间',
    created_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at         DATETIME(6)   NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_jobs_recruiter FOREIGN KEY (recruiter_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_jobs_category FOREIGN KEY (category_id) REFERENCES job_categories (id) ON DELETE SET NULL,
    KEY idx_jobs_status (status),
    KEY idx_jobs_recruiter (recruiter_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位信息';

-- 求职投递记录
CREATE TABLE IF NOT EXISTS job_application_records (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    job_id          BIGINT       NOT NULL,
    seeker_user_id  BIGINT       NOT NULL COMMENT '投递人（求职方）',
    status          VARCHAR(32)  NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED/VIEWED/REJECTED/ACCEPTED 等',
    note            TEXT         NULL COMMENT '备注/附言',
    applied_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_jar_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_jar_seeker FOREIGN KEY (seeker_user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_jar_job_seeker (job_id, seeker_user_id),
    KEY idx_jar_seeker (seeker_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='职位投递记录';

-- 求职偏好记录表
CREATE TABLE IF NOT EXISTS job_preferences (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    seeker_user_id  BIGINT       NOT NULL COMMENT '求职者用户',
    keyword         VARCHAR(255) NULL COMMENT '职位关键词',
    city            VARCHAR(128) NULL COMMENT '期望城市',
    salary_min      INT          NULL COMMENT '期望薪资下限',
    salary_max      INT          NULL COMMENT '期望薪资上限',
    work_type       VARCHAR(32)  NULL COMMENT '期望岗位类型',
    preference_count BIGINT      NOT NULL DEFAULT 0 COMMENT '筛选次数',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_job_preferences_seeker FOREIGN KEY (seeker_user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_job_preferences_search_signature (seeker_user_id, keyword, city, salary_min, salary_max, work_type),
    KEY idx_job_preferences_seeker (seeker_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='求职偏好记录';

-- 简历表
CREATE TABLE IF NOT EXISTS resumes (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id         BIGINT       NOT NULL COMMENT '简历所属用户',
    name            VARCHAR(128) NOT NULL COMMENT '简历名称',
    personal_info   TEXT         NULL COMMENT '个人信息',
    education       TEXT         NULL COMMENT '教育经历',
    job_status      VARCHAR(64)  NULL COMMENT '求职状态',
    work_experience TEXT         NULL COMMENT '工作经历',
    project_experience TEXT      NULL COMMENT '项目经历',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_resumes_user FOREIGN KEY (user_id) REFERENCES users (id) ON DELETE CASCADE,
    KEY idx_resumes_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='简历信息';

-- 对话会话表
CREATE TABLE IF NOT EXISTS chat_sessions (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    job_id          BIGINT       NOT NULL COMMENT '关联的职位',
    seeker_user_id  BIGINT       NOT NULL COMMENT '求职者',
    recruiter_user_id BIGINT     NOT NULL COMMENT '招聘者',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_session_job FOREIGN KEY (job_id) REFERENCES jobs (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_session_seeker FOREIGN KEY (seeker_user_id) REFERENCES users (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_session_recruiter FOREIGN KEY (recruiter_user_id) REFERENCES users (id) ON DELETE CASCADE,
    UNIQUE KEY uk_chat_session_job_seeker (job_id, seeker_user_id),
    KEY idx_chat_session_recruiter (recruiter_user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话会话';

-- 对话消息表
CREATE TABLE IF NOT EXISTS chat_messages (
    id              BIGINT       NOT NULL AUTO_INCREMENT PRIMARY KEY,
    session_id      BIGINT       NOT NULL COMMENT '关联的会话',
    sender_user_id  BIGINT       NOT NULL COMMENT '发送者',
    msg_type        VARCHAR(32)  NOT NULL DEFAULT 'TEXT' COMMENT 'TEXT, FILE, RESUME',
    content         TEXT         NOT NULL COMMENT '消息内容或文件URL',
    is_read         BOOLEAN      NOT NULL DEFAULT FALSE COMMENT '是否已读',
    created_at      DATETIME(6)  NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions (id) ON DELETE CASCADE,
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_user_id) REFERENCES users (id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='对话消息';
