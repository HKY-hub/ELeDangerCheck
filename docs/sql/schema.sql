CREATE DATABASE IF NOT EXISTS eledangercheck DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE eledangercheck;

DROP TABLE IF EXISTS evaluation;
DROP TABLE IF EXISTS measure_dict;
DROP TABLE IF EXISTS hazard_dict;
DROP TABLE IF EXISTS accident_case;
DROP TABLE IF EXISTS disclosure_sign;
DROP TABLE IF EXISTS disclosure;
DROP TABLE IF EXISTS safety_measure;
DROP TABLE IF EXISTS hazard;
DROP TABLE IF EXISTS task_parse;
DROP TABLE IF EXISTS task;
DROP TABLE IF EXISTS user;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE COMMENT '用户名',
    password VARCHAR(255) NOT NULL COMMENT '密码',
    email VARCHAR(100) COMMENT '邮箱',
    real_name VARCHAR(50) COMMENT '真实姓名',
    status INT DEFAULT 1 COMMENT '状态：0-禁用，1-启用',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL COMMENT '任务名称',
    task_type VARCHAR(50) COMMENT '任务类型',
    task_desc TEXT COMMENT '任务描述',
    voltage_level VARCHAR(20) COMMENT '电压等级',
    equipment_type VARCHAR(100) COMMENT '设备类型',
    work_type VARCHAR(50) COMMENT '作业类型',
    env_conditions VARCHAR(200) COMMENT '环境条件',
    principal_id BIGINT COMMENT '负责人ID',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待处理，in_progress-进行中，completed-已完成',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    INDEX idx_principal (principal_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务表';

CREATE TABLE IF NOT EXISTS task_parse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    voltage_level VARCHAR(20) COMMENT '电压等级',
    equipment_type VARCHAR(100) COMMENT '设备类型',
    work_type VARCHAR(50) COMMENT '作业类型',
    env_conditions VARCHAR(200) COMMENT '环境条件',
    location VARCHAR(200) COMMENT '作业位置',
    parse_time DATETIME COMMENT '解析时间',
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    INDEX idx_task (task_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='任务解析表';

CREATE TABLE IF NOT EXISTS hazard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    hazard_name VARCHAR(200) NOT NULL COMMENT '危险点名称',
    category VARCHAR(50) COMMENT '危险类别',
    hazard_level VARCHAR(20) DEFAULT 'medium' COMMENT '风险等级：high-高，medium-中，low-低',
    description TEXT COMMENT '危险点描述',
    source_case VARCHAR(200) COMMENT '案例来源',
    location VARCHAR(200) COMMENT '位置',
    similarity DOUBLE DEFAULT 0.0 COMMENT '相似度',
    create_time DATETIME COMMENT '创建时间',
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    INDEX idx_task (task_id),
    INDEX idx_level (hazard_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危险点表';

CREATE TABLE IF NOT EXISTS safety_measure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hazard_id BIGINT NOT NULL COMMENT '危险点ID',
    measure_code VARCHAR(50) COMMENT '措施代码',
    measure_name VARCHAR(200) NOT NULL COMMENT '措施名称',
    measure_desc TEXT COMMENT '措施描述',
    priority INT DEFAULT 1 COMMENT '优先级',
    status VARCHAR(20) DEFAULT 'pending' COMMENT '状态：pending-待执行，completed-已完成',
    create_time DATETIME COMMENT '创建时间',
    FOREIGN KEY (hazard_id) REFERENCES hazard(id) ON DELETE CASCADE,
    INDEX idx_hazard (hazard_id),
    INDEX idx_priority (priority)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='安全措施表';

CREATE TABLE IF NOT EXISTS disclosure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL COMMENT '任务ID',
    title VARCHAR(200) NOT NULL COMMENT '交底标题',
    disclosure_no VARCHAR(50) COMMENT '交底编号',
    disclosure_type VARCHAR(50) COMMENT '交底类型',
    content TEXT COMMENT '交底内容',
    pdf_path VARCHAR(500) COMMENT 'PDF路径',
    word_path VARCHAR(500) COMMENT 'Word路径',
    emergency_contact VARCHAR(100) COMMENT '紧急联系人',
    emergency_route VARCHAR(500) COMMENT '应急路线',
    disclosure_status VARCHAR(20) DEFAULT 'draft' COMMENT '交底状态：draft-草稿，issued-已下发，completed-已完成',
    sign_status VARCHAR(20) DEFAULT 'unsigned' COMMENT '签名状态：unsigned-未签名，signed-已签名',
    create_time DATETIME COMMENT '创建时间',
    update_time DATETIME COMMENT '更新时间',
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    INDEX idx_task (task_id),
    INDEX idx_status (disclosure_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交底表';

CREATE TABLE IF NOT EXISTS disclosure_sign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    disclosure_id BIGINT NOT NULL COMMENT '交底ID',
    user_id BIGINT NOT NULL COMMENT '用户ID',
    sign_image TEXT COMMENT '签名图片',
    sign_time DATETIME COMMENT '签名时间',
    FOREIGN KEY (disclosure_id) REFERENCES disclosure(id) ON DELETE CASCADE,
    INDEX idx_disclosure (disclosure_id),
    INDEX idx_user (user_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='交底签名表';

CREATE TABLE IF NOT EXISTS accident_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_name VARCHAR(200) NOT NULL COMMENT '案例名称',
    accident_type VARCHAR(50) COMMENT '事故类型',
    voltage_level VARCHAR(20) COMMENT '电压等级',
    work_type VARCHAR(50) COMMENT '作业类型',
    equipment_type VARCHAR(100) COMMENT '设备类型',
    case_desc TEXT COMMENT '案例描述',
    cause_analysis TEXT COMMENT '原因分析',
    lessons_learned TEXT COMMENT '经验教训',
    hazard_points TEXT COMMENT '危险点',
    occur_time DATETIME COMMENT '发生时间',
    severity VARCHAR(20) DEFAULT 'medium' COMMENT '严重程度',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_type (accident_type),
    INDEX idx_work_type (work_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='事故案例表';

CREATE TABLE IF NOT EXISTS hazard_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_desc TEXT COMMENT '字典描述',
    level VARCHAR(20) DEFAULT 'medium' COMMENT '风险等级',
    priority INT DEFAULT 1 COMMENT '优先级',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='危险源字典表';

CREATE TABLE IF NOT EXISTS measure_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(50) NOT NULL UNIQUE COMMENT '字典编码',
    dict_name VARCHAR(100) NOT NULL COMMENT '字典名称',
    dict_desc TEXT COMMENT '字典描述',
    hazard_code VARCHAR(50) COMMENT '关联危险源编码',
    priority INT DEFAULT 1 COMMENT '优先级',
    create_time DATETIME COMMENT '创建时间',
    INDEX idx_code (dict_code),
    INDEX idx_hazard_code (hazard_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='控制措施字典表';

CREATE TABLE IF NOT EXISTS evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    disclosure_id BIGINT NOT NULL COMMENT '交底ID',
    score INT COMMENT '评分',
    violation_count INT DEFAULT 0 COMMENT '违规项数',
    accident_count INT DEFAULT 0 COMMENT '事故数',
    evaluation_desc TEXT COMMENT '评估描述',
    evaluation_time DATETIME COMMENT '评估时间',
    FOREIGN KEY (disclosure_id) REFERENCES disclosure(id) ON DELETE CASCADE,
    INDEX idx_disclosure (disclosure_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='评估表';

INSERT INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_ELECTRIC', '触电风险', '接触带电设备或线路可能导致触电事故', 'high', 1);
INSERT INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_FALL', '高处坠落', '高空作业时防护措施不到位可能导致坠落', 'high', 2);
INSERT INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_MECHANICAL', '机械伤害', '机械设备操作不当可能导致伤害', 'medium', 3);
INSERT INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_MISTAKE', '误操作', '错误操作可能导致设备损坏或人员伤亡', 'high', 4);
INSERT INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_FIRE', '火灾风险', '电气设备过热或短路可能引发火灾', 'medium', 5);

INSERT INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_POWER_OFF', '停电操作', '执行停电操作，断开相关开关', 'HAZARD_ELECTRIC', 1);
INSERT INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_TEST', '验电操作', '使用验电器确认设备已停电', 'HAZARD_ELECTRIC', 2);
INSERT INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_GROUND', '挂接地线', '在工作地段两端挂接地线', 'HAZARD_ELECTRIC', 3);
INSERT INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_FENCE', '设置围栏', '在作业区域周围设置安全围栏', 'HAZARD_FALL', 4);
INSERT INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_SAFETY_BELT', '系安全带', '高空作业必须系好安全带', 'HAZARD_FALL', 5);

INSERT INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, case_desc, cause_analysis, lessons_learned, hazard_points, severity) VALUES 
('XX变电站10kV触电事故', '触电', '10kV', '检修', '开关', '2023年5月，XX变电站10kV开关检修作业时，工作人员误触带电部位导致触电死亡。', '未执行停电验电流程，安全措施不到位。', '必须严格执行停电-验电-接地流程，加强安全监护。', '触电;误操作', 'high');

INSERT INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, case_desc, cause_analysis, lessons_learned, hazard_points, severity) VALUES 
('XX线路杆塔坠落事故', '高处坠落', '35kV', '巡视', '杆塔', '2023年8月，XX线路杆塔巡视时，工作人员从杆塔上坠落，造成重伤。', '未系安全带，安全意识淡薄。', '高空作业必须系好安全带，定期安全培训。', '高处坠落', 'high');

INSERT INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, case_desc, cause_analysis, lessons_learned, hazard_points, severity) VALUES 
('XX配电站火灾事故', '火灾', '10kV', '维护', '变压器', '2023年12月，XX配电站变压器过热引发火灾，造成设备损坏。', '设备维护不到位，未及时发现异常。', '加强设备巡检，配备灭火器材。', '火灾', 'medium');