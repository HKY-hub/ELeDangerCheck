package org.example.eledangercheck.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Component
public class DatabaseInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DatabaseInitializer.class);

    @Autowired
    private DataSource dataSource;

    @PostConstruct
    public void init() {
        Connection conn = null;
        try {
            conn = DataSourceUtils.getConnection(dataSource);
            
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS user (id BIGINT AUTO_INCREMENT PRIMARY KEY, username VARCHAR(20) NOT NULL UNIQUE, password VARCHAR(255) NOT NULL, email VARCHAR(100), real_name VARCHAR(50), phone VARCHAR(20), role VARCHAR(20) DEFAULT 'operator', department VARCHAR(100), status INT DEFAULT 1, create_time DATETIME, update_time DATETIME, INDEX idx_username (username)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS task (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_name VARCHAR(200) NOT NULL, task_type VARCHAR(50), task_desc TEXT, voltage_level VARCHAR(20), equipment_type VARCHAR(100), work_type VARCHAR(50), env_conditions VARCHAR(200), principal_id BIGINT, status VARCHAR(20) DEFAULT 'pending', create_time DATETIME, update_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS task_parse (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT NOT NULL, original_text TEXT, voltage_level VARCHAR(20), equipment_type VARCHAR(100), work_type VARCHAR(50), env_conditions VARCHAR(200), location VARCHAR(200), parse_result TEXT, create_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS hazard (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT NOT NULL, hazard_name VARCHAR(200) NOT NULL, category VARCHAR(50), hazard_level VARCHAR(20) DEFAULT 'medium', description TEXT, source_case VARCHAR(200), location VARCHAR(200), similarity DOUBLE, create_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS safety_measure (id BIGINT AUTO_INCREMENT PRIMARY KEY, hazard_id BIGINT NOT NULL, measure_code VARCHAR(50), measure_name VARCHAR(200) NOT NULL, measure_desc TEXT, priority INT DEFAULT 1, status VARCHAR(20) DEFAULT 'pending', create_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS disclosure (id BIGINT AUTO_INCREMENT PRIMARY KEY, task_id BIGINT, title VARCHAR(200) NOT NULL, disclosure_no VARCHAR(50), disclosure_type VARCHAR(50), content TEXT, pdf_path VARCHAR(500), word_path VARCHAR(500), emergency_route VARCHAR(500), emergency_contact VARCHAR(100), disclosure_status VARCHAR(20) DEFAULT 'draft', sign_status VARCHAR(20) DEFAULT 'unsigned', issuer_signature TEXT, receiver_signature TEXT, disclosure_time DATETIME, confirm_time DATETIME, create_time DATETIME, update_time DATETIME, INDEX idx_task_id (task_id), INDEX idx_disclosure_status (disclosure_status)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            // 修复disclosure表task_id字段：从NOT NULL改为允许NULL（兼容新建交底无任务的场景）
            try {
                executeSQL(conn, "ALTER TABLE disclosure MODIFY COLUMN task_id BIGINT NULL");
                logger.info("disclosure表task_id字段已更新为允许NULL");
            } catch (Exception e) {
                logger.debug("disclosure表task_id字段调整跳过（可能已是最新状态）: {}", e.getMessage());
            }

            // 补充disclosure表缺失字段（兼容schema-mysql.sql创建的表）
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN pdf_path VARCHAR(500) DEFAULT NULL");
                logger.info("disclosure表新增pdf_path字段");
            } catch (Exception e) {
                logger.debug("disclosure表pdf_path字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN word_path VARCHAR(500) DEFAULT NULL");
                logger.info("disclosure表新增word_path字段");
            } catch (Exception e) {
                logger.debug("disclosure表word_path字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN issuer_signature TEXT DEFAULT NULL");
                logger.info("disclosure表新增issuer_signature字段");
            } catch (Exception e) {
                logger.debug("disclosure表issuer_signature字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN receiver_signature TEXT DEFAULT NULL");
                logger.info("disclosure表新增receiver_signature字段");
            } catch (Exception e) {
                logger.debug("disclosure表receiver_signature字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN disclosure_time DATETIME DEFAULT NULL");
                logger.info("disclosure表新增disclosure_time字段");
            } catch (Exception e) {
                logger.debug("disclosure表disclosure_time字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN confirm_time DATETIME DEFAULT NULL");
                logger.info("disclosure表新增confirm_time字段");
            } catch (Exception e) {
                logger.debug("disclosure表confirm_time字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN update_time DATETIME DEFAULT NULL");
                logger.info("disclosure表新增update_time字段");
            } catch (Exception e) {
                logger.debug("disclosure表update_time字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE disclosure ADD COLUMN sign_status VARCHAR(20) DEFAULT 'unsigned'");
                logger.info("disclosure表新增sign_status字段");
            } catch (Exception e) {
                logger.debug("disclosure表sign_status字段跳过（可能已存在）: {}", e.getMessage());
            }

            // 修复hazard_dict表字段名：从description改为dict_desc（与实体类一致）
            try {
                executeSQL(conn, "ALTER TABLE hazard_dict CHANGE COLUMN description dict_desc TEXT DEFAULT NULL");
                logger.info("hazard_dict表description字段已重命名为dict_desc");
            } catch (Exception e) {
                logger.debug("hazard_dict表dict_desc字段跳过（可能已是最新状态）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE hazard_dict ADD COLUMN priority INT DEFAULT 1");
                logger.info("hazard_dict表新增priority字段");
            } catch (Exception e) {
                logger.debug("hazard_dict表priority字段跳过（可能已存在）: {}", e.getMessage());
            }

            // 修复measure_dict表字段名：从description改为dict_desc
            try {
                executeSQL(conn, "ALTER TABLE measure_dict CHANGE COLUMN description dict_desc TEXT DEFAULT NULL");
                logger.info("measure_dict表description字段已重命名为dict_desc");
            } catch (Exception e) {
                logger.debug("measure_dict表dict_desc字段跳过（可能已是最新状态）: {}", e.getMessage());
            }

            // 补充hazard表缺失字段
            try {
                executeSQL(conn, "ALTER TABLE hazard ADD COLUMN similarity DOUBLE DEFAULT NULL");
                logger.info("hazard表新增similarity字段");
            } catch (Exception e) {
                logger.debug("hazard表similarity字段跳过（可能已存在）: {}", e.getMessage());
            }

            // 补充safety_measure表缺失字段
            try {
                executeSQL(conn, "ALTER TABLE safety_measure ADD COLUMN measure_code VARCHAR(50) DEFAULT NULL");
                logger.info("safety_measure表新增measure_code字段");
            } catch (Exception e) {
                logger.debug("safety_measure表measure_code字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE safety_measure MODIFY COLUMN priority INT DEFAULT 1");
                logger.info("safety_measure表priority字段类型已更新为INT");
            } catch (Exception e) {
                logger.debug("safety_measure表priority字段跳过（可能已是最新状态）: {}", e.getMessage());
            }

            // 补充task_parse表缺失字段
            try {
                executeSQL(conn, "ALTER TABLE task_parse ADD COLUMN original_text TEXT DEFAULT NULL");
                logger.info("task_parse表新增original_text字段");
            } catch (Exception e) {
                logger.debug("task_parse表original_text字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE task_parse ADD COLUMN parse_result TEXT DEFAULT NULL");
                logger.info("task_parse表新增parse_result字段");
            } catch (Exception e) {
                logger.debug("task_parse表parse_result字段跳过（可能已存在）: {}", e.getMessage());
            }

            // 扩大task表字段长度，避免数据超长报错
            try {
                executeSQL(conn, "ALTER TABLE task MODIFY COLUMN equipment_type VARCHAR(500) DEFAULT NULL");
                logger.info("task表equipment_type字段已扩大为VARCHAR(500)");
            } catch (Exception e) {
                logger.debug("task表equipment_type字段跳过（可能已是最新）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE task MODIFY COLUMN work_type VARCHAR(200) DEFAULT NULL");
                logger.info("task表work_type字段已扩大为VARCHAR(200)");
            } catch (Exception e) {
                logger.debug("task表work_type字段跳过（可能已是最新）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE task MODIFY COLUMN env_conditions VARCHAR(200) DEFAULT NULL");
                logger.info("task表env_conditions字段已扩大为VARCHAR(200)");
            } catch (Exception e) {
                logger.debug("task表env_conditions字段跳过（可能已是最新）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE task MODIFY COLUMN voltage_level VARCHAR(50) DEFAULT NULL");
                logger.info("task表voltage_level字段已扩大为VARCHAR(50)");
            } catch (Exception e) {
                logger.debug("task表voltage_level字段跳过（可能已是最新）: {}", e.getMessage());
            }

            // 扩大task_parse表字段长度
            try {
                executeSQL(conn, "ALTER TABLE task_parse MODIFY COLUMN equipment_type VARCHAR(500) DEFAULT NULL");
                logger.info("task_parse表equipment_type字段已扩大为VARCHAR(500)");
            } catch (Exception e) {
                logger.debug("task_parse表equipment_type字段跳过（可能已是最新）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE task_parse MODIFY COLUMN work_type VARCHAR(200) DEFAULT NULL");
                logger.info("task_parse表work_type字段已扩大为VARCHAR(200)");
            } catch (Exception e) {
                logger.debug("task_parse表work_type字段跳过（可能已是最新）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE task_parse MODIFY COLUMN location VARCHAR(500) DEFAULT NULL");
                logger.info("task_parse表location字段已扩大为VARCHAR(500)");
            } catch (Exception e) {
                logger.debug("task_parse表location字段跳过（可能已是最新）: {}", e.getMessage());
            }

            // 补充accident_case表缺失字段
            try {
                executeSQL(conn, "ALTER TABLE accident_case ADD COLUMN cause_analysis TEXT DEFAULT NULL");
                logger.info("accident_case表新增cause_analysis字段");
            } catch (Exception e) {
                logger.debug("accident_case表cause_analysis字段跳过（可能已存在）: {}", e.getMessage());
            }

            // 补充user表缺失字段
            try {
                executeSQL(conn, "ALTER TABLE user ADD COLUMN phone VARCHAR(20) DEFAULT NULL");
                logger.info("user表新增phone字段");
            } catch (Exception e) {
                logger.debug("user表phone字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE user ADD COLUMN role VARCHAR(20) DEFAULT 'operator'");
                logger.info("user表新增role字段");
            } catch (Exception e) {
                logger.debug("user表role字段跳过（可能已存在）: {}", e.getMessage());
            }
            try {
                executeSQL(conn, "ALTER TABLE user ADD COLUMN department VARCHAR(100) DEFAULT NULL");
                logger.info("user表新增department字段");
            } catch (Exception e) {
                logger.debug("user表department字段跳过（可能已存在）: {}", e.getMessage());
            }

            executeSQL(conn, "CREATE TABLE IF NOT EXISTS disclosure_sign (id BIGINT AUTO_INCREMENT PRIMARY KEY, disclosure_id BIGINT NOT NULL, user_id BIGINT NOT NULL, sign_image TEXT, sign_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS accident_case (id BIGINT AUTO_INCREMENT PRIMARY KEY, case_name VARCHAR(200) NOT NULL, accident_type VARCHAR(50), voltage_level VARCHAR(20), work_type VARCHAR(50), equipment_type VARCHAR(100), case_desc TEXT, cause_analysis TEXT, lessons_learned TEXT, hazard_points TEXT, occur_time DATETIME, severity VARCHAR(20) DEFAULT 'medium', create_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS hazard_dict (id BIGINT AUTO_INCREMENT PRIMARY KEY, dict_code VARCHAR(50) NOT NULL UNIQUE, dict_name VARCHAR(100) NOT NULL, dict_desc TEXT, level VARCHAR(20) DEFAULT 'medium', priority INT DEFAULT 1, create_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS measure_dict (id BIGINT AUTO_INCREMENT PRIMARY KEY, dict_code VARCHAR(50) NOT NULL UNIQUE, dict_name VARCHAR(100) NOT NULL, dict_desc TEXT, hazard_code VARCHAR(50), priority INT DEFAULT 1, create_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS evaluation (id BIGINT AUTO_INCREMENT PRIMARY KEY, disclosure_id BIGINT NOT NULL, score INT, violation_count INT DEFAULT 0, accident_count INT DEFAULT 0, evaluation_desc TEXT, evaluation_time DATETIME) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            executeSQL(conn, "CREATE TABLE IF NOT EXISTS weather_city (id BIGINT AUTO_INCREMENT PRIMARY KEY, city_code VARCHAR(20) NOT NULL UNIQUE, city_name VARCHAR(50) NOT NULL, province VARCHAR(50), pinyin VARCHAR(100), INDEX idx_city_name (city_name)) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
            
            // 清理重复/错误的宜昌城市编码（保留正确的101200901）
            executeSQL(conn, "DELETE FROM weather_city WHERE city_name = '宜昌' AND city_code != '101200901'");
            
            executeSQL(conn, "INSERT IGNORE INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_ELECTRIC', '触电风险', '接触带电设备或线路可能导致触电事故', 'high', 1)");
            executeSQL(conn, "INSERT IGNORE INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_FALL', '高处坠落', '高空作业时防护措施不到位可能导致坠落', 'high', 2)");
            executeSQL(conn, "INSERT IGNORE INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_MECHANICAL', '机械伤害', '机械设备操作不当可能导致伤害', 'medium', 3)");
            executeSQL(conn, "INSERT IGNORE INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_MISTAKE', '误操作', '错误操作可能导致设备损坏或人员伤亡', 'high', 4)");
            executeSQL(conn, "INSERT IGNORE INTO hazard_dict (dict_code, dict_name, dict_desc, level, priority) VALUES ('HAZARD_FIRE', '火灾风险', '电气设备过热或短路可能引发火灾', 'medium', 5)");
            
            executeSQL(conn, "INSERT IGNORE INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_POWER_OFF', '停电操作', '执行停电操作，断开相关开关', 'HAZARD_ELECTRIC', 1)");
            executeSQL(conn, "INSERT IGNORE INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_TEST', '验电操作', '使用验电器确认设备已停电', 'HAZARD_ELECTRIC', 2)");
            executeSQL(conn, "INSERT IGNORE INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_GROUND', '挂接地线', '在工作地段两端挂接地线', 'HAZARD_ELECTRIC', 3)");
            executeSQL(conn, "INSERT IGNORE INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_FENCE', '设置围栏', '在作业区域周围设置安全围栏', 'HAZARD_FALL', 4)");
            executeSQL(conn, "INSERT IGNORE INTO measure_dict (dict_code, dict_name, dict_desc, hazard_code, priority) VALUES ('MEASURE_SAFETY_BELT', '系安全带', '高空作业必须系好安全带', 'HAZARD_FALL', 5)");
            
            executeSQL(conn, "INSERT IGNORE INTO user (username, password, email, real_name, phone, role, department, status, create_time) VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', 'admin@example.com', '管理员', '13800138000', 'admin', '运维部', 1, NOW())");
            executeSQL(conn, "INSERT IGNORE INTO user (username, password, email, real_name, phone, role, department, status, create_time) VALUES ('111', '$2a$10$N9qo8uLOickgx2ZMRZoMye.IjzqAKL9xL5jvMFVdNJHvGCgTq/VEq', '111@example.com', '用户111', '13800138001', 'operator', '运维部', 1, NOW())");

            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101010100', '北京', '北京')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101020100', '上海', '上海')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101280101', '广州', '广东')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101280601', '深圳', '广东')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101210101', '杭州', '浙江')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101190101', '南京', '江苏')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101200101', '武汉', '湖北')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101200901', '宜昌', '湖北')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101200201', '襄阳', '湖北')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101200801', '荆州', '湖北')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101200501', '黄冈', '湖北')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101270101', '成都', '四川')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101110101', '西安', '陕西')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101040100', '重庆', '重庆')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101220101', '合肥', '安徽')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101030100', '天津', '天津')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101180101', '郑州', '河南')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101250101', '长沙', '湖南')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101120201', '青岛', '山东')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101230201', '厦门', '福建')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101070201', '大连', '辽宁')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101230101', '福州', '福建')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101120101', '济南', '山东')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101070101', '沈阳', '辽宁')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101290101', '昆明', '云南')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101050101', '哈尔滨', '黑龙江')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101090101', '石家庄', '河北')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101060101', '长春', '吉林')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101240101', '南昌', '江西')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101300101', '南宁', '广西')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101260101', '贵阳', '贵州')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101100101', '太原', '山西')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101160101', '兰州', '甘肃')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101310101', '海口', '海南')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101130101', '乌鲁木齐', '新疆')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101170101', '银川', '宁夏')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101150101', '西宁', '青海')");
            executeSQL(conn, "INSERT IGNORE INTO weather_city (city_code, city_name, province) VALUES ('101140101', '拉萨', '西藏')");

            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站10kV开关柜触电事故', '触电', '10kV', '检修', '开关柜', '较大', '2023年5月12日，某供电公司检修班在对10kV开关柜进行预防性试验时，工作负责人未确认设备是否完全停电，即安排作业人员进入柜内作业。作业人员右手触及带电母线，造成触电重伤，经抢救脱离生命危险。', '工作负责人未执行停电验电程序，未挂接地线即开始工作；作业人员安全意识淡薄，未对工作负责人的违章指挥提出异议；现场安全监护不到位，监护人未履行职责。', '严格执行两票三制，作业前必须完成停电、验电、挂接地线三项措施；加强工作负责人安全责任教育，严禁违章指挥；强化作业人员自我保护意识，有权拒绝违章指挥。', '未停电作业;未验电;未挂接地线;违章指挥;监护不到位', '2023-05-12 09:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某线路工程高处坠落事故', '高处坠落', '110kV', '施工', '线路', '重大', '2022年8月25日，某送变电工程公司在110kV线路架线施工中，作业人员在30米高的铁塔上安装附件时，未系安全带，不慎从塔上坠落，当场死亡。事故造成直接经济损失180万元。', '作业人员严重违反安全规程，高处作业未系安全带；现场安全员未及时制止违章行为；施工前安全技术交底流于形式，未针对高处作业风险进行重点强调。', '高处作业必须100%系好安全带，安全带应高挂低用；加强现场安全监督，对违章行为零容忍；认真开展安全技术交底，确保每个作业人员知晓风险点和防控措施。', '未系安全带;高处作业;安全监督不到位;交底流于形式', '2022-08-25 14:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电厂误操作导致全厂停电事故', '误操作', '220kV', '运维', '断路器', '重大', '2021年11月8日，某发电厂运行人员在进行220kV母线倒闸操作时，误将运行中的断路器拉开，导致220kV I段母线失电，全厂机组停运，造成电网大面积停电，影响用户12万户。', '运行人员操作前未认真核对设备名称和编号，操作票填写错误；监护人未认真履行监护职责，未及时发现并纠正错误操作；防误闭锁装置管理不善，存在解锁操作现象。', '严格执行操作票制度，操作前必须核对设备名称、编号、位置；加强操作监护，监护人必须全程监护，不得参与操作；加强防误闭锁装置管理，严禁随意解锁。', '误操作;操作票错误;监护不到位;防误闭锁失效', '2021-11-08 16:45:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站变压器火灾事故', '火灾', '110kV', '运维', '变压器', '较大', '2020年6月18日，某110kV变电站主变压器因绕组绝缘老化发生短路，引发火灾。消防人员经过3小时扑救将火扑灭，变压器完全烧毁，造成直接经济损失500余万元，无人员伤亡。', '变压器长期过载运行，绝缘老化加速；日常巡检不到位，未及时发现油温异常升高；变压器消防设施不完善，火灾初期未能有效控制。', '加强设备状态监测，定期开展油色谱分析和绝缘试验；严禁设备长期过载运行，发现异常及时处理；完善变电站消防设施，定期开展消防演练。', '绝缘老化;过载运行;巡检不到位;消防设施不完善', '2020-06-18 02:15:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配电工程倒杆伤人事故', '坍塌', '10kV', '施工', '线路', '较大', '2023年3月7日，某供电公司在进行10kV线路改造施工时，新立的12米水泥电杆因基础未夯实就上架线，导致电杆倾倒，砸中下方作业人员，造成1人死亡、2人重伤。', '施工单位为赶工期，在电杆基础未夯实的情况下即安排架线作业；现场负责人未进行安全检查就盲目指挥；作业人员自我保护意识差，在杆下危险区域停留。', '严格执行施工工艺标准，电杆组立后必须夯实基础并经检查合格方可上架线；加强施工现场安全管理，严禁违章指挥；作业人员应保持安全距离，严禁在杆下危险区域逗留。', '基础未夯实;违章指挥;危险区域停留;赶工期', '2023-03-07 11:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某检修工地物体打击事故', '物体打击', '35kV', '检修', '变压器', '一般', '2022年4月15日，某变电检修公司在35kV变电站检修主变压器时，高处作业人员使用的扳手不慎坠落，击中下方地面作业人员头部，造成头皮裂伤，缝合8针。', '高处作业人员未使用工具袋，工具随意放置；下方作业人员未正确佩戴安全帽；现场未设置安全警戒区域，上下交叉作业未采取隔离措施。', '高处作业必须使用工具袋，较大工具应系保险绳；作业人员必须正确佩戴安全帽；交叉作业应设置安全隔离层，下方设置警戒区。', '工具坠落;未戴安全帽;交叉作业无隔离;无警戒区', '2022-04-15 10:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电缆隧道机械伤害事故', '机械伤害', '10kV', '施工', '电缆', '较大', '2021年9月3日，某电缆公司在进行10kV电缆敷设作业时，作业人员在电缆输送机运行过程中，用手去整理跑偏的电缆，手套被卷入输送机滚轮，造成右手3根手指被绞断。', '作业人员违反操作规程，在设备运行时用手接触转动部位；设备安全防护装置缺失，滚轮处未安装防护罩；现场安全管理不严，违章作业未被及时制止。', '严格遵守设备操作规程，设备运行时严禁接触转动部位；所有转动机械必须安装可靠的安全防护装置；加强作业人员安全教育，提高安全防范意识。', '违章作业;防护装置缺失;转动机械;管理不严', '2021-09-03 15:40:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某500kV变电站误登带电构架事故', '触电', '500kV', '检修', '其他', '特别重大', '2020年12月22日，某超高压公司检修人员在500kV变电站进行设备检修时，工作负责人未核对设备位置，误将作业人员带到带电构架附近，作业人员攀爬时与带电体安全距离不足，引发电弧灼伤，造成2人死亡、1人重伤。', '工作负责人责任心不强，未认真核对工作地点；作业人员未确认设备是否带电即盲目攀爬；现场安全措施不完善，未在带电设备周围设置明显的警示标识。', '严格执行工作票制度，工作负责人必须对工作地点的正确性负责；作业前必须核对待作业设备状态，确认无电后方可工作；完善现场安全警示标识，加强作业全过程监护。', '误登带电设备;未核对位置;警示标识缺失;监护不力', '2020-12-22 08:50:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配电台区低压触电死亡事故', '触电', '低压', '运维', '其他', '较大', '2023年7月19日，某供电所运维人员在处理低压台区漏电故障时，未断开电源即进行故障排查，用手触摸计量箱外壳时触电，经抢救无效死亡。', '运维人员安全意识淡薄，未停电就进行故障处理；未使用验电笔检测设备是否带电；单人作业，无监护人员。', '低压作业同样必须执行停电验电程序；严禁单人进行电气作业；配备合格的验电工具并正确使用。', '未停电;未验电;单人作业;安全意识淡薄', '2023-07-19 13:25:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某输电线路巡检高处坠落事故', '高处坠落', '220kV', '巡检', '线路', '较大', '2022年2月14日，某输电运检中心巡检人员在220kV线路巡检登塔过程中，因脚扣滑脱，从15米高处坠落，造成腰椎骨折、下肢瘫痪。', '巡检人员登塔前未检查脚扣安全性能；冬季塔上有霜冻，脚扣摩擦力不足；未按规定使用防坠安全器。', '登塔前必须检查登高工具的安全性能；恶劣天气条件下禁止登塔作业；高处作业必须使用防坠安全器等双重防护。', '脚扣滑脱;未检查工器具;恶劣天气;防坠措施缺失', '2022-02-14 09:00:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站隔离开关误操作事故', '误操作', '110kV', '运维', '隔离开关', '较大', '2021年5月27日，某变电站值班人员在进行倒闸操作时，误将带负荷的110kV隔离开关拉开，产生强烈电弧，造成隔离开关烧毁，母线停电，未造成人员伤亡。', '操作人未严格执行操作票制度，跳项操作；监护人未认真监护，未及时发现操作错误；隔离开关与断路器之间的防误闭锁功能失效。', '严格执行操作票制度，严禁跳项、漏项操作；加强操作监护，做到逐项唱票、逐项复诵、逐项操作；定期检查防误闭锁装置，确保功能完好。', '带负荷拉刀闸;跳项操作;监护不到位;防误闭锁失效', '2021-05-27 11:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电缆沟火灾事故', '火灾', '35kV', '运维', '电缆', '较大', '2020年10月9日，某35kV变电站电缆沟内因电缆中间接头故障引发火灾，火势沿电缆蔓延，烧毁电缆200余米，造成3条出线停运，直接经济损失200余万元。', '电缆中间接头制作质量不合格，长期运行后绝缘击穿短路；电缆沟未按规定设置防火封堵；火灾自动报警系统未及时预警。', '严格控制电缆接头制作质量，加强工艺管控；按规范设置电缆防火封堵和火灾报警系统；定期开展电缆红外测温，及时发现隐患。', '电缆接头故障;防火封堵缺失;报警失效;绝缘击穿', '2020-10-09 03:45:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某基建工地脚手架坍塌事故', '坍塌', '低压', '施工', '其他', '重大', '2023年1月16日，某变电站基建工地，施工人员在搭设12米高的脚手架时，因脚手架基础不牢、连墙件设置不足，导致脚手架整体坍塌，造成3人死亡、4人受伤。', '脚手架搭设方案未经审批，搭设不符合规范要求；脚手架基础未做硬化处理，连墙件设置数量不足；施工单位未进行验收即投入使用。', '脚手架搭设必须编制专项方案并经审批；严格按规范搭设，确保基础牢固、连墙件可靠；搭设完成后必须经验收合格方可使用。', '脚手架坍塌;基础不牢;连墙件不足;未验收', '2023-01-16 16:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电气试验触电事故', '触电', '10kV', '试验', '开关柜', '较大', '2022年6月30日，某电力试验公司在对10kV开关柜进行耐压试验时，试验人员在未放电的情况下，用手接触试验接线端子，造成触电，被弹开后摔倒致脑部受伤。', '试验人员违反试验规程，试验结束后未对被试设备充分放电；未使用绝缘工具进行拆线操作；现场监护人未尽到监护责任。', '高压试验结束后必须对被试设备充分放电；拆线作业必须使用绝缘工具；加强试验全过程安全监护。', '未放电;试验违章;未用绝缘工具;监护不到位', '2022-06-30 14:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变压器检修物体打击事故', '物体打击', '220kV', '检修', '变压器', '一般', '2021年3月12日，某检修公司在220kV变电站主变检修现场，吊车在吊放散热器时，因捆绑不牢，散热器滑落，砸中地面作业人员脚部，造成脚趾骨折。', '吊装作业捆绑不牢，未设溜绳；作业人员在吊装物下方停留；现场指挥人员指挥不当。', '吊装作业必须捆绑牢固，设置溜绳；严禁在吊装物下方停留或通行；吊装作业应由持证人员指挥。', '捆绑不牢;吊装物下方停留;无证指挥;无溜绳', '2021-03-12 09:45:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某断路器检修机械伤害事故', '机械伤害', '110kV', '检修', '断路器', '一般', '2023年9月5日，某变电检修人员在检修110kV断路器机构时，未将弹簧储能释放，在检查过程中机构突然动作，将作业人员左手夹伤，造成手指骨折。', '作业人员未按规程释放弹簧储能；未采取防止机构突然动作的措施；对断路器机构危险性认识不足。', '断路器检修前必须释放弹簧能量并采取可靠的制动措施；加强作业人员对设备结构和危险性的培训；检修过程中设专人监护。', '弹簧未释放;机构误动;无制动措施;认识不足', '2023-09-05 10:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配电室火灾事故', '火灾', '10kV', '运维', '开关柜', '较大', '2020年8月21日，某企业配电室10kV开关柜因触头接触不良、过热引燃绝缘件发生火灾，造成配电室全部设备烧毁，全厂停产3天，直接经济损失800余万元。', '开关柜触头压力不足，长期过热未被发现；配电室未安装火灾自动报警和灭火装置；日常巡检不到位，未开展红外测温。', '加强设备状态监测，定期开展红外测温；配电室应按规定安装消防设施；加强日常巡检，及时发现设备缺陷。', '触头过热;巡检不到位;消防设施缺失;接触不良', '2020-08-21 23:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某线路施工误碰带电线路事故', '触电', '10kV', '施工', '线路', '重大', '2022年11月7日，某农网改造施工队在新建10kV线路时，施工人员在立杆过程中，吊车吊臂与上方平行的10kV带电线路安全距离不足，导致线路放电，造成3人触电，其中2人死亡、1人重伤。', '施工前未进行现场勘察，未查明周边带电线路情况；未编制专项施工方案；吊车作业未设专人监护，未保持足够安全距离。', '施工前必须进行现场勘察，摸清周边带电设备情况；临近带电体作业必须编制专项方案并设专人监护；确保施工机械与带电体保持足够安全距离。', '临近带电作业;未勘察;无监护;安全距离不足', '2022-11-07 14:50:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站高处坠落事故', '高处坠落', '35kV', '检修', '隔离开关', '较大', '2021年7月14日，某供电公司检修人员在35kV变电站检修隔离开关时，站在架构横梁上作业，未系安全带，脚下打滑从5米高处坠落，造成骨盆骨折、颅内出血。', '作业人员高处作业未系安全带；作业位置无操作平台，作业条件恶劣；现场监护人未及时制止违章行为。', '高处作业必须系好安全带；高处作业应搭设操作平台或使用登高作业车；监护人应认真履行监护职责，及时制止违章。', '未系安全带;无操作平台;监护不到位;高处作业', '2021-07-14 11:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某调度误操作导致大面积停电事故', '误操作', '500kV', '运维', '其他', '特别重大', '2023年4月18日，某电网调度中心调度员在进行500kV线路转检修操作时，误将运行中的500kV母联断路器断开，导致500kVII段母线失电，2座220kV变电站全停，影响用户35万户，停电时间4小时。', '调度员操作前未认真核对电网运行方式；操作票审核不严，存在严重错误；调度操作防误系统功能不完善。', '严格执行调度操作票制度，加强操作票审核；完善调度操作防误系统；加强调度员技能培训和责任心教育。', '调度误操作;操作票错误;审核不严;防误失效', '2023-04-18 19:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电缆试验触电事故', '触电', '35kV', '试验', '电缆', '较大', '2020年5月2日，某电缆公司试验人员在对35kV电缆进行直流耐压试验时，电缆另一端未设监护人，也未设置安全围栏，一村民路过时触及电缆终端头，触电身亡。', '试验前未对电缆两端进行安全确认；电缆另一端未设监护人、未设围栏和警示标志；试验现场安全措施不到位。', '电缆试验时两端都必须设专人监护；试验区域设置安全围栏和警示标志；试验前确认所有人员撤离试验区。', '试验无监护;无警示标识;未设围栏;安全措施不全', '2020-05-02 15:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某输电线路倒塔事故', '坍塌', '220kV', '运维', '线路', '重大', '2022年1月23日，某地区遭遇罕见暴雪冰冻灾害，220kV某线路铁塔因覆冰严重超过设计标准，发生倒塔事故，造成线路停运，未造成人员伤亡，直接经济损失3000余万元。', '极端天气导致铁塔覆冰远超设计值；线路运行维护单位对灾害预警响应不及时；未采取融冰等应急措施。', '加强极端天气预警和应急响应；重要线路应加装覆冰监测和融冰装置；完善电网防灾减灾标准。', '覆冰倒塔;极端天气;预警响应慢;无融冰措施', '2022-01-23 06:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电动机检修机械伤害事故', '机械伤害', '低压', '检修', '其他', '一般', '2023年8月17日，某企业电工在检修低压电动机时，未切断电源、未挂警示牌，用手盘动电动机时，电动机突然启动，将作业人员右手卷入皮带轮，造成皮肤挫裂伤。', '检修前未停电、未验电、未挂警示牌；未采取防止电机突然启动的措施；作业人员安全意识淡薄。', '电气设备检修必须执行停电、验电、挂牌、上锁制度；检修前断开电源并采取可靠的防启动措施；加强作业人员安全教育。', '未停电;未挂牌;突然启动;安全意识差', '2023-08-17 16:45:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某开关柜爆炸事故', '其他', '10kV', '运维', '开关柜', '较大', '2021年12月4日，某10kV配电室开关柜在运行中发生内部短路燃弧爆炸，开关柜门板被炸飞，冲击波将配电室窗户玻璃震碎，造成2名运行人员轻度灼伤，直接经济损失150万元。', '开关柜内部绝缘爬距不足，长期运行后绝缘老化；开关柜未安装压力释放装置；运行人员巡检不到位。', '选用具有合格防燃弧能力的开关柜；开关柜应安装压力释放通道；加强设备状态检测和巡检。', '绝缘击穿;燃弧爆炸;压力释放失效;巡检不到位', '2021-12-04 21:15:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站技改施工高处坠落事故', '高处坠落', '110kV', '施工', '其他', '较大', '2020年9月28日，某电力工程公司在110kV变电站技改施工中，作业人员在安装站用变母排时，从6米高的脚手架上踩空坠落，因安全带系挂不牢，安全带脱钩，造成腰椎压缩性骨折。', '作业人员安全带系挂不规范，低挂高用；脚手架脚手板未满铺，存在空隙；现场安全检查不到位。', '高处作业安全带必须高挂低用，系挂牢固；脚手架脚手板必须满铺、绑扎牢固；加强高处作业安全检查。', '安全带系挂不牢;脚手板未满铺;低挂高用;检查不到位', '2020-09-28 10:00:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配网作业反送电触电事故', '触电', '10kV', '检修', '线路', '重大', '2023年2月14日，某供电公司在进行10kV线路检修时，已按规定停电并挂接地线，但某用户自备发电机违规向电网反送电，导致线路带电，作业人员接触导线时触电，造成1人死亡、1人重伤。', '用户自备电源管理混乱，未装设可靠的闭锁装置；施工前未对所有可能来电的方向进行确认；对用户侧安全管理不到位。', '加强用户自备电源管理，必须装设可靠的防反送电闭锁装置；线路作业前应断开所有可能来电的开关并验电挂接；加强用户安全用电检查。', '反送电;用户自备电源;闭锁失效;管理不到位', '2023-02-14 09:15:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某GIS设备安装物体打击事故', '物体打击', '220kV', '施工', '其他', '一般', '2022年5月9日，某安装公司在220kV变电站GIS设备安装现场，作业人员在紧固法兰螺栓时，扳手打滑脱手，飞出的扳手砸中下方作业人员肩部，造成锁骨骨折。', '高处作业使用工具未系保险绳；交叉作业未设置安全隔离；下方作业人员未在安全区域作业。', '高处作业工具必须系保险绳；交叉作业应设置硬隔离；下方设置警戒区，禁止无关人员进入。', '工具脱手;未系保险绳;交叉作业;无隔离', '2022-05-09 14:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某互感器试验误操作事故', '误操作', '110kV', '试验', '其他', '一般', '2021年8月26日，某试验所在对110kV电流互感器进行试验时，试验接线错误，将试验电压加至运行中的二次回路，造成保护装置误动，线路跳闸。', '试验人员未认真核对接线，误碰运行回路；未采取防止误碰运行设备的措施；工作负责人监护不到位。', '试验前必须认真核对接线，确认无误后方可加压；在运行设备附近试验应采取隔离措施；加强试验工作监护。', '接线错误;误碰运行设备;监护不到位;保护误动', '2021-08-26 11:50:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变压器油务工作火灾事故', '火灾', '220kV', '检修', '变压器', '较大', '2023年6月11日，某检修公司在进行220kV主变压器油务处理时，滤油机附近的废油桶因静电引发火灾，火势蔓延至变压器本体，造成变压器部分烧毁，直接经济损失600万元。', '油务作业区域未清理易燃易爆物品；未采取防静电措施；现场消防器材配备不足。', '油务作业区域严禁烟火，清理易燃易爆物品；采取可靠的防静电接地措施；配备足够的消防器材。', '静电起火;易燃易爆物;消防器材不足;无防静电措施', '2023-06-11 15:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某输电线路铁塔腐蚀倒塔事故', '坍塌', '110kV', '运维', '线路', '较大', '2020年11月30日，某110kV输电线路铁塔因长期受工业废气腐蚀，塔材严重锈蚀，在大风天气下发生倒塔，造成线路停运，未造成人员伤亡，直接经济损失500万元。', '线路途经工业区，铁塔腐蚀严重；运行单位未定期检测铁塔锈蚀情况；未及时进行防腐处理。', '对腐蚀环境中的线路应缩短检测周期，定期检测塔材锈蚀情况；及时进行防腐处理或更换；加强线路特殊区段的运维管理。', '铁塔腐蚀;检测不到位;未及时处理;大风倒塔', '2020-11-30 20:15:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某低压配电室触电事故', '触电', '低压', '检修', '开关柜', '一般', '2022年3月22日，某物业电工在低压配电室更换空气开关时，图省事未停电，带电作业时不慎触及带电端子，造成左手电灼伤，治疗后痊愈。', '电工安全意识淡薄，明知有危险仍冒险带电作业；未使用绝缘防护用具；单人作业无监护。', '低压电气作业应停电进行，严禁无保护带电作业；带电作业必须使用合格的绝缘工具并设专人监护；加强电工安全教育和技能培训。', '带电作业;未用绝缘用具;单人作业;安全意识差', '2022-03-22 14:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站断路器机械伤害事故', '机械伤害', '500kV', '检修', '断路器', '较大', '2023年10月8日，某超高压公司检修人员在500kV断路器检修中，使用液压扳手紧固螺栓时，扳手卡涩反弹，击中作业人员面部，造成鼻梁骨折、牙齿脱落2颗。', '液压扳手使用方法不当；作业人员未佩戴防护面罩；工具使用前未检查完好性。', '使用液压工具应掌握正确操作方法；作业人员应佩戴防护面罩等防护用品；工具使用前检查完好性。', '工具反弹;未戴防护面罩;操作不当;工具未检查', '2023-10-08 09:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电缆施工挖断运行电缆事故', '误操作', '10kV', '施工', '电缆', '较大', '2021年4月19日，某市政工程公司在道路施工时，未向供电部门查询地下电缆分布，擅自使用挖掘机开挖，将一条10kV运行电缆挖断，造成大面积停电，未造成人员伤亡。', '施工前未进行地下管线探测和交底；未经审批擅自开挖；现场无专人监护。', '施工前必须查询地下管线分布并进行现场交底；开挖前履行审批手续；机械开挖设专人监护，人工开挖探沟。', '挖断电缆;未探测;未审批;无监护', '2021-04-19 10:45:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某风电项目高处坠落事故', '高处坠落', '其他', '施工', '其他', '重大', '2020年7月15日，某风电场安装项目中，作业人员在80米高的风机塔筒内攀爬时，防坠器突然失效，从50米高处坠落，当场死亡。', '防坠器未定期检验，已损坏失效；作业人员攀爬前未检查防坠器；安全工器具管理混乱。', '防坠器等安全工器具必须定期检验，合格后方可使用；作业前必须检查安全工器具完好性；建立安全工器具台账并定期检查。', '防坠器失效;未检查工器具;定期检验缺失;管理混乱', '2020-07-15 13:25:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站蓄电池室火灾事故', '火灾', '低压', '运维', '其他', '一般', '2022年9月6日，某变电站蓄电池室因蓄电池短路引发火灾，烧毁蓄电池组3组，造成直流系统短时中断，未造成人员伤亡，直接经济损失80万元。', '蓄电池长期运行，内阻增大发热；蓄电池室通风不良，温度过高；未安装火灾报警和灭火装置。', '加强蓄电池日常维护，定期检测内阻和温度；蓄电池室保持良好通风；安装火灾报警和自动灭火装置。', '蓄电池短路;通风不良;温度过高;消防设施缺失', '2022-09-06 05:40:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配网施工倒杆事故', '坍塌', '10kV', '施工', '线路', '较大', '2023年5月30日，某农网升级改造工程中，施工队在拆除旧电杆时，未采取临时拉线固定措施，用挖掘机直接推倒电杆，电杆倒向一侧砸中路边低压线路，造成线路断线停电，幸未伤人。', '拆除作业未制定施工方案；未采取临时拉线等安全措施；用挖掘机推倒电杆的方法严重违章。', '拆除电杆必须制定拆除方案，采取可靠的安全措施；严禁用挖掘机直接推倒电杆；拆除作业设专人指挥，设置警戒区。', '违章拆除;无临时拉线;无方案;无警戒区', '2023-05-30 15:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某开关柜检修感应电触电事故', '触电', '35kV', '检修', '开关柜', '一般', '2021年1月27日，某检修人员在35kV开关柜检修时，虽然已将开关转检修状态，但相邻间隔带电，检修人员接触开关柜外壳时因感应电麻电，本能后退时头部撞到横梁，造成头部外伤。', '开关柜感应电防护措施不到位；检修人员未采取防感应电措施；对感应电危害认识不足。', '在邻近带电设备的间隔作业应采取防感应电措施；作业人员使用个人保安线；加强感应电防护知识培训。', '感应电;未装接地线;个人保安线;认识不足', '2021-01-27 11:00:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变压器安装起重伤害事故', '物体打击', '110kV', '施工', '变压器', '较大', '2020年12月8日，某安装公司在吊装110kV变压器本体时，吊车支腿下陷，吊车侧翻，变压器从2米高处坠落，造成设备严重损坏，2名地面人员受轻伤。', '吊车支腿未垫枕木，地面承载力不足；吊装前未检查支腿稳固性；起吊重量接近吊车额定载荷。', '吊车支腿必须垫枕木，确保地面承载力满足要求；吊装前检查支腿稳固性；严禁超载吊装，风力过大时停止吊装。', '支腿下陷;未垫枕木;超载吊装;地面承载力不足', '2020-12-08 14:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配电室误入带电间隔事故', '触电', '10kV', '检修', '开关柜', '重大', '2022年7月21日，某供电公司检修班在10kV配电室进行开关柜检修时，工作负责人未核对设备编号，误将作业人员带入带电间隔，作业人员打开柜门后电弧灼伤，造成1人死亡、1人重伤。', '工作负责人未认真核对设备名称和编号；现场安全措施不完善，未在带电间隔设置警示标识；作业人员未确认设备状态即工作。', '严格执行工作票制度，工作负责人必须对工作地点正确性负责；带电间隔必须设置明显的警示标识和遮栏；作业前必须确认设备状态。', '误入带电间隔;未核对编号;警示缺失;状态未确认', '2022-07-21 08:50:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某输电线路覆冰舞动断线事故', '其他', '500kV', '运维', '线路', '较大', '2023年1月15日，某地区出现冻雨天气，500kV某线路因覆冰舞动导致相间短路，线路跳闸，重合不成功，造成一条500kV通道中断，未造成人员伤亡。', '极端冻雨天气导致线路严重覆冰；线路舞动防护措施不足；舞动监测预警系统不完善。', '加强极端天气监测预警；易舞段加装相间间隔棒等防舞装置；完善线路舞动监测系统。', '覆冰舞动;相间短路;防舞措施不足;预警不完善', '2023-01-15 04:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电力隧道施工坍塌事故', '坍塌', '其他', '施工', '其他', '重大', '2021年6月17日，某电缆隧道工程施工中，因隧道侧壁支护不及时，发生土方坍塌，将3名作业人员埋压，造成2人死亡、1人受伤，直接经济损失400余万元。', '隧道施工未按方案及时进行支护；地质条件变化未采取加强措施；现场监测不到位，未及时发现坍塌征兆。', '隧道施工必须按方案及时支护，严禁超挖；地质条件变化时应调整支护方案；加强施工监测，发现隐患及时撤离。', '土方坍塌;支护不及时;监测不到位;地质变化', '2021-06-17 16:25:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某低压带电作业电弧灼伤事故', '触电', '低压', '检修', '其他', '一般', '2020年4月10日，某供电所低压运维人员在进行低压带电接火作业时，未使用绝缘工具，操作中造成相间短路，产生电弧将作业人员双手灼伤。', '低压带电作业未使用合格的绝缘工具；作业人员未穿戴绝缘防护用品；作业方法不当。', '低压带电作业必须使用合格的绝缘工具；作业人员应穿戴绝缘防护用品；严格按低压带电作业规程操作。', '相间短路;电弧灼伤;未用绝缘工具;防护不足', '2020-04-10 10:15:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站继电保护误整定事故', '误操作', '220kV', '试验', '其他', '较大', '2022年10月23日，某供电公司继电保护人员在对220kV线路保护进行定值调整时，误将电流定值调小，导致线路正常负荷时保护误动跳闸，造成用户停电2小时。', '保护人员定值计算错误；定值调整后未进行复核；保护定值管理制度执行不严。', '保护定值必须双人计算、双人复核；定值调整后应进行模拟试验验证；严格执行保护定值管理制度。', '误整定;定值错误;未复核;制度执行不严', '2022-10-23 15:40:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某箱式变电站火灾事故', '火灾', '10kV', '运维', '其他', '一般', '2023年8月2日，某小区箱式变电站因低压电容柜补偿电容器鼓肚爆炸引发火灾，烧毁箱变内低压设备，造成小区停电5小时，未造成人员伤亡。', '电容器长期运行老化鼓肚未及时发现更换；电容器保护配置不完善；日常巡检不到位。', '定期检查电容器外观，发现鼓肚、渗油及时更换；完善电容器保护配置；加强箱式变电站日常巡检。', '电容器爆炸;老化未换;保护不完善;巡检不到位', '2023-08-02 20:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某输电线异物短路事故', '其他', '110kV', '运维', '线路', '一般', '2021年2月14日，大年初一，某110kV线路因附近村民燃放孔明灯，孔明灯飘落挂在线路上造成相间短路，线路跳闸重合成功，未造成长时间停电。', '线路沿线群众安全意识淡薄；电力设施保护宣传不到位；外力破坏防控难度大。', '加强电力设施保护宣传，提高群众安全意识；重要时段加强线路特巡；推广应用异物在线监测装置。', '外力破坏;孔明灯;相间短路;宣传不到位', '2021-02-14 21:50:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某检修现场工器具高空坠落事故', '物体打击', '10kV', '检修', '线路', '一般', '2020年10月5日，某配电检修班在10kV线路检修作业时，杆上作业人员放置在横担上的螺丝刀被风吹落，击中杆下地面人员头部，安全帽被砸裂，人员受轻微伤。', '高处作业工具未放置在工具袋内；杆下人员未保持安全距离；现场未设置警戒区。', '高处作业所有工具必须放在工具袋内，严禁随意放置；杆下人员应保持安全距离；作业区域设置警戒区。', '工具坠落;未放工具袋;无警戒区;安全距离不足', '2020-10-05 09:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站直流系统接地误操作事故', '误操作', '35kV', '检修', '其他', '一般', '2022年12月11日，某变电站运行人员在查找直流接地故障时，误将运行中的保护电源断开，造成主变差动保护退出运行，幸好未发生故障，未造成严重后果。', '运行人员直流接地查找方法不当；未使用专用仪器查找；操作前未考虑对保护的影响。', '直流接地查找应使用专用仪器，严禁盲目拉路；查找前应退出可能误动的保护；加强运行人员技能培训。', '误断保护电源;查找方法不当;未用专用仪器;技能不足', '2022-12-11 14:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某光伏电站设备火灾事故', '火灾', '其他', '运维', '其他', '较大', '2023年3月25日，某光伏电站逆变器因IGBT模块故障引发火灾，烧毁逆变器2台及附近光伏组件，造成直接经济损失300余万元，未造成人员伤亡。', '逆变器散热不良，模块长期过热运行；未安装自动灭火装置；日常巡检未发现温度异常。', '逆变器室应安装温度监测和自动灭火装置；加强设备温度监测和巡检；定期对逆变器进行维护保养。', '逆变器故障;散热不良;消防设施缺失;巡检不到位', '2023-03-25 13:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配电线路杆塔基础滑坡事故', '坍塌', '35kV', '运维', '线路', '较大', '2021年9月2日，某地区遭遇强降雨，35kV某线路杆塔因位于山坡上，基础周围土体滑坡，导致杆塔倾斜，线路停运，未造成人员伤亡。', '杆塔选址不当，位于易滑坡地段；未采取护坡加固措施；雨季特巡不到位。', '山区线路应避开易滑坡地段，无法避开时应采取护坡加固措施；雨季加强特殊区段特巡；建立地质灾害预警机制。', '山体滑坡;基础不稳;选址不当;特巡不到位', '2021-09-02 22:15:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某带电作业工器具不合格触电事故', '触电', '10kV', '检修', '线路', '较大', '2022年5月18日，某带电作业班在进行10kV线路带电作业时，使用的绝缘操作杆因绝缘层有裂纹未被发现，作业中发生绝缘击穿，造成作业人员触电，从12米高处坠落受重伤。', '绝缘工器具未按周期试验，存在缺陷；作业前未对工器具进行外观检查；带电作业防护措施不到位。', '绝缘工器具必须按周期进行电气试验，合格后方可使用；作业前必须对工器具进行外观检查；带电作业必须有后备保护措施。', '绝缘击穿;工器具不合格;未试验;未检查', '2022-05-18 10:45:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站扩建施工机械碰带电设备事故', '触电', '220kV', '施工', '其他', '重大', '2020年11月12日，某220kV变电站扩建工程中，吊车在吊装设备时，吊臂与带电的220kV母线安全距离不足，导致母线放电，造成母线跳闸停电，吊车烧毁，司机受重伤。', '临近带电设备作业未编制专项安全方案；未设专职安全监护人；吊车司机对带电体安全距离认识不足。', '临近带电体作业必须编制专项方案并经审批；设专职监护人进行全过程监护；对施工人员进行专项安全交底。', '临近带电作业;无专项方案;无监护;安全距离不足', '2020-11-12 14:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电缆头制作高处坠落事故', '高处坠落', '10kV', '施工', '电缆', '一般', '2023年7月4日，某电缆公司作业人员在6米高的电缆终端杆上制作电缆头时，脚下踩的脚扣突然松脱，幸好安全带系在横担上，人员悬挂在空中，受惊吓导致心脏病发作送医。', '脚扣使用前未检查，存在缺陷；作业人员身体状况不适合高处作业；班前会未进行安全确认。', '登高工器具使用前必须检查完好性；高处作业人员应定期体检，不适合者不得登高；班前会进行安全确认和身体状况问询。', '脚扣松脱;未检查工器具;身体不适;未确认', '2023-07-04 09:00:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变压器有载分接开关误操作事故', '误操作', '110kV', '检修', '变压器', '较大', '2021年8月30日，某变电站检修人员在对主变有载分接开关进行检修调试时，误操作导致分接开关在两个档位之间，造成变压器内部短路，变压器差动保护动作跳闸。', '检修人员对有载分接开关结构不熟悉；调试前未认真阅读说明书；现场无技术人员指导。', '特殊设备检修应安排熟悉设备的人员进行；检修前认真研究图纸和说明书；关键作业应有技术人员现场指导。', '误操作;结构不熟悉;无技术指导;调试错误', '2021-08-30 15:10:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电力设备库房火灾事故', '火灾', '其他', '其他', '其他', '较大', '2022年3月9日，某供电公司设备库房因电气线路老化短路引发火灾，烧毁库存设备、材料等，造成直接经济损失200余万元，未造成人员伤亡。', '库房电气线路敷设不规范，老化未更换；库房未安装火灾自动报警系统；消防安全管理不到位。', '库房电气线路应规范敷设，定期检查更换；按规定安装消防设施和火灾报警系统；加强消防安全管理，定期开展消防检查。', '电气火灾;线路老化;消防设施缺失;管理不到位', '2022-03-09 03:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某输电线路山火跳闸事故', '其他', '500kV', '运维', '线路', '较大', '2023年4月5日，清明节，某500kV线路沿线村民上坟烧纸引发山火，山火蔓延至线路下方，导致线路因绝缘降低跳闸重合不成功，造成线路停运6小时。', '线路通道管理不到位，未及时清理通道内可燃物；森林防火期特巡不到位；与地方政府联动机制不完善。', '加强线路通道治理，及时清理通道内可燃物；森林防火期加强特巡和宣传；建立与地方政府的联动防火机制。', '山火跳闸;通道管理不到位;特巡不足;联动机制缺', '2023-04-05 14:50:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某配电抢修触电事故', '触电', '10kV', '运维', '线路', '较大', '2020年12月25日，某供电所抢修班在处理10kV线路故障时，因夜间抢修能见度差，作业人员误登带电杆塔，造成触电，从10米高处坠落，全身多处骨折。', '夜间抢修照明不足；作业前未核对线路名称和杆号；登杆前未验电。', '夜间抢修必须配备充足的照明设备；登杆前必须核对线路名称、杆号并验电；恶劣条件下抢修应加强安全监护。', '误登杆塔;夜间作业;照明不足;未验电', '2020-12-25 21:30:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站GIS设备漏气事故', '其他', '220kV', '运维', '其他', '一般', '2021年7月19日，某220kV变电站GIS设备气室压力持续下降，运行人员巡检时发现漏气，检修人员处理过程中，因SF6气体大量泄漏，造成2名作业人员中毒送医。', 'GIS设备密封老化导致漏气；检修人员未佩戴正压式呼吸器；SF6气体防护知识不足。', '定期检测GIS设备气室压力，发现漏气及时处理；SF6设备检修必须佩戴正压式呼吸器；加强SF6气体防护知识培训。', 'SF6泄漏;未戴呼吸器;防护不足;知识欠缺', '2021-07-19 10:00:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某农村低压线路倒杆触电事故', '触电', '低压', '运维', '线路', '较大', '2022年8月10日，某农村低压线路因受大风影响，电杆倾斜倒地，导线断落地面，一村民路过时跨步电压触电，经抢救无效死亡。', '低压电杆年久失修，基础不牢；线路运维单位巡视不到位；农村电网改造不彻底。', '加强农村低压电网运维管理，定期开展巡视检查；加快老旧线路改造；加强农村安全用电宣传。', '倒杆断线;跨步电压;运维不到位;线路老化', '2022-08-10 18:20:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某变电站自动化系统误动事故', '误操作', '110kV', '试验', '其他', '一般', '2023年11月3日，某自动化厂家人员在对变电站自动化系统进行升级时，误将测试数据下发到运行设备，导致110kV出线断路器误跳闸，重合成功，未造成用户停电。', '厂家人员在运行系统上工作未办理工作票；未采取测试数据与运行系统隔离措施；运行单位监护不到位。', '在运行设备上工作必须办理工作票；测试数据应与运行系统可靠隔离；运行单位应派专人全程监护。', '误下发命令;无工作票;未隔离;监护不到位', '2023-11-03 16:40:00', NOW())");
            executeSQL(conn, "INSERT IGNORE INTO accident_case (case_name, accident_type, voltage_level, work_type, equipment_type, severity, case_desc, cause_analysis, lessons_learned, hazard_points, occur_time, create_time) VALUES ('某电力施工交通事故', '其他', '其他', '施工', '其他', '较大', '2021年5月24日，某电力工程公司施工车辆在运送施工人员途中，因车速过快、雨天路滑，车辆失控侧翻，造成2人死亡、3人受伤。', '驾驶员超速行驶，雨天未减速；车辆安全检查不到位；施工单位交通安全管理不严。', '加强驾驶员安全教育，严禁超速、疲劳驾驶；定期对车辆进行安全检查；恶劣天气严格控制车辆出行。', '交通事故;超速行驶;雨天路滑;管理不严', '2021-05-24 08:30:00', NOW())");
            
            logger.info("Database tables initialized successfully");
        } catch (Exception e) {
            logger.error("Database initialization failed: {}", e.getMessage(), e);
        } finally {
            if (conn != null) {
                DataSourceUtils.releaseConnection(conn, dataSource);
            }
        }
    }

    private void executeSQL(Connection conn, String sql) {
        try (Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
        } catch (Exception e) {
            logger.error("SQL execution failed: {}...", sql.substring(0, Math.min(50, sql.length())), e);
        }
    }
}