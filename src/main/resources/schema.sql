USE eledangercheck;

CREATE TABLE IF NOT EXISTS user (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(20) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    real_name VARCHAR(50),
    status INT DEFAULT 1,
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_username (username)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS task (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_name VARCHAR(200) NOT NULL,
    task_type VARCHAR(50),
    task_desc TEXT,
    voltage_level VARCHAR(20),
    equipment_type VARCHAR(100),
    work_type VARCHAR(50),
    env_conditions VARCHAR(200),
    principal_id BIGINT,
    status VARCHAR(20) DEFAULT 'pending',
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_principal_id (principal_id),
    INDEX idx_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS task_parse (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    voltage_level VARCHAR(20),
    equipment_type VARCHAR(100),
    work_type VARCHAR(50),
    env_conditions VARCHAR(200),
    location VARCHAR(200),
    parse_time DATETIME,
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hazard (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT NOT NULL,
    hazard_name VARCHAR(200) NOT NULL,
    category VARCHAR(50),
    hazard_level VARCHAR(20) DEFAULT 'medium',
    description TEXT,
    source_case VARCHAR(200),
    location VARCHAR(200),
    similarity DOUBLE DEFAULT 0.0,
    create_time DATETIME,
    FOREIGN KEY (task_id) REFERENCES task(id) ON DELETE CASCADE,
    INDEX idx_task_id (task_id),
    INDEX idx_hazard_level (hazard_level)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS safety_measure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    hazard_id BIGINT NOT NULL,
    measure_name VARCHAR(200) NOT NULL,
    measure_desc TEXT,
    priority VARCHAR(10) DEFAULT 'P1',
    status VARCHAR(20) DEFAULT 'pending',
    create_time DATETIME,
    FOREIGN KEY (hazard_id) REFERENCES hazard(id) ON DELETE CASCADE,
    INDEX idx_hazard_id (hazard_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS disclosure (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    task_id BIGINT,
    title VARCHAR(200) NOT NULL,
    disclosure_no VARCHAR(50),
    disclosure_type VARCHAR(50),
    content TEXT,
    pdf_path VARCHAR(500),
    word_path VARCHAR(500),
    emergency_route VARCHAR(500),
    emergency_contact VARCHAR(100),
    disclosure_status VARCHAR(20) DEFAULT 'draft',
    sign_status VARCHAR(20) DEFAULT 'unsigned',
    create_time DATETIME,
    update_time DATETIME,
    INDEX idx_task_id (task_id),
    INDEX idx_disclosure_status (disclosure_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS disclosure_sign (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    disclosure_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    sign_image TEXT,
    sign_time DATETIME,
    FOREIGN KEY (disclosure_id) REFERENCES disclosure(id) ON DELETE CASCADE,
    FOREIGN KEY (user_id) REFERENCES user(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS accident_case (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    case_name VARCHAR(200) NOT NULL,
    accident_type VARCHAR(50),
    voltage_level VARCHAR(20),
    work_type VARCHAR(50),
    equipment_type VARCHAR(100),
    case_desc TEXT,
    lessons_learned TEXT,
    hazard_points TEXT,
    occur_time DATETIME,
    severity VARCHAR(20) DEFAULT 'medium',
    create_time DATETIME,
    INDEX idx_accident_type (accident_type),
    INDEX idx_work_type (work_type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS hazard_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(50) NOT NULL UNIQUE,
    dict_name VARCHAR(100) NOT NULL,
    dict_desc TEXT,
    level VARCHAR(20) DEFAULT 'medium',
    priority INT DEFAULT 1,
    create_time DATETIME,
    INDEX idx_dict_code (dict_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS measure_dict (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    dict_code VARCHAR(50) NOT NULL UNIQUE,
    dict_name VARCHAR(100) NOT NULL,
    dict_desc TEXT,
    hazard_code VARCHAR(50),
    priority INT DEFAULT 1,
    create_time DATETIME,
    FOREIGN KEY (hazard_code) REFERENCES hazard_dict(dict_code),
    INDEX idx_hazard_code (hazard_code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS evaluation (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    disclosure_id BIGINT NOT NULL,
    score INT,
    violation_count INT DEFAULT 0,
    accident_count INT DEFAULT 0,
    evaluation_desc TEXT,
    evaluation_time DATETIME,
    FOREIGN KEY (disclosure_id) REFERENCES disclosure(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

INSERT IGNORE INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES
('HAZARD_ELECTRIC', '触电风险', '接触带电设备或线路可能导致触电事故', 'high', 1),
('HAZARD_FALL', '高处坠落', '高空作业时防护措施不到位可能导致坠落', 'high', 2),
('HAZARD_MECHANICAL', '机械伤害', '机械设备操作不当可能导致伤害', 'medium', 3),
('HAZARD_MISTAKE', '误操作', '错误操作可能导致设备损坏或人员伤亡', 'high', 4),
('HAZARD_FIRE', '火灾风险', '电气设备过热或短路可能引发火灾', 'medium', 5),
('HAZARD_GAS', '气体中毒', 'SF6气体泄漏可能导致人员中毒', 'medium', 6),
('HAZARD_EXPLOSION', '爆炸风险', '设备故障或误操作可能引发爆炸', 'high', 7),
('HAZARD_COLLISION', '物体打击', '工具或物体坠落可能导致人员受伤', 'medium', 8);

INSERT IGNORE INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES
('MEASURE_POWER_OFF', '停电操作', '执行停电操作，断开相关开关', 'HAZARD_ELECTRIC', 1),
('MEASURE_TEST', '验电操作', '使用验电器确认设备已停电', 'HAZARD_ELECTRIC', 2),
('MEASURE_GROUND', '挂接地线', '在工作地段两端挂接地线', 'HAZARD_ELECTRIC', 3),
('MEASURE_FENCE', '设置围栏', '在作业区域周围设置安全围栏', 'HAZARD_FALL', 4),
('MEASURE_GUARD', '专人监护', '安排专人监护作业过程', 'HAZARD_MISTAKE', 5),
('MEASURE_LOCK', '上锁挂牌', '执行上锁挂牌制度，防止误操作', 'HAZARD_MISTAKE', 6),
('MEASURE_PPE', '佩戴防护用品', '正确佩戴绝缘手套、绝缘靴等防护用品', 'HAZARD_ELECTRIC', 7),
('MEASURE_LADDER', '梯子检查', '检查梯子是否牢固可靠', 'HAZARD_FALL', 8),
('MEASURE_FIRE_EXTINGUISHER', '配备灭火器', '在作业现场配备合适的灭火器', 'HAZARD_FIRE', 9),
('MEASURE_VENTILATION', '通风换气', '确保作业区域通风良好', 'HAZARD_GAS', 10),
('MEASURE_TOOL_CHECK', '工具检查', '检查工器具是否完好', 'HAZARD_MECHANICAL', 11),
('MEASURE_SAFETY_BELT', '系安全带', '高空作业必须系好安全带', 'HAZARD_FALL', 12);