package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.*;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.*;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class AgentService {

    private final TaskMapper taskMapper;
    private final TaskParseMapper taskParseMapper;
    private final HazardMapper hazardMapper;
    private final HazardDictMapper hazardDictMapper;
    private final MeasureDictMapper measureDictMapper;
    private final SafetyMeasureMapper safetyMeasureMapper;
    private final DisclosureMapper disclosureMapper;
    private final AccidentCaseMapper accidentCaseMapper;

    private static final Pattern VOLTAGE_PATTERN = Pattern.compile("(\\d+)(?:\\.?\\d*)\\s*(kV|KV|伏|千伏)");
    private static final Pattern LOW_VOLTAGE_PATTERN = Pattern.compile("(低压|380V|220V|0\\.4kV|400V)");
    private static final Pattern EQUIPMENT_PATTERN = Pattern.compile("(线路|电缆|变压器|开关|绝缘子|杆塔|母线|电容器|电抗器|避雷器|互感器|配电柜|开关柜|控制柜|配电箱)");
    private static final Pattern WORK_TYPE_PATTERN = Pattern.compile("(检修|试验|抢修|安装|拆除|更换|维护|巡视)");
    private static final Pattern LOCATION_PATTERN = Pattern.compile("(\\d+#杆|\\d+号杆|\\d+塔|\\d+号塔|\\d+站|\\d+号站|\\w+线路|配电室|配电房|设备间)");

    private static final Map<String, List<String>> HAZARD_RULES = new HashMap<>();
    private static final Map<String, List<String>> HAZARD_MEASURES = new HashMap<>();

    static {
        HAZARD_RULES.put("触电", Arrays.asList("kV", "千伏", "带电", "停电", "验电", "地线", "低压", "380V", "220V", "配电柜", "开关柜", "配电箱"));
        HAZARD_RULES.put("高坠", Arrays.asList("杆", "塔", "高空", "攀登", "绝缘子"));
        HAZARD_RULES.put("机械伤害", Arrays.asList("起重", "吊装", "施工机械", "工具", "搬运", "更换"));
        HAZARD_RULES.put("误操作", Arrays.asList("操作", "合闸", "拉闸", "倒闸", "开关"));
        HAZARD_RULES.put("物体打击", Arrays.asList("工具", "材料", "高空作业"));
        HAZARD_RULES.put("火灾", Arrays.asList("电缆", "变压器", "油浸", "配电柜", "开关柜", "接线"));
        HAZARD_RULES.put("爆炸", Arrays.asList("电容器", "电抗器", "SF6"));
        HAZARD_RULES.put("窒息", Arrays.asList("电缆沟", "隧道", "密闭空间"));

        HAZARD_MEASURES.put("触电", Arrays.asList("停电", "验电", "挂地线", "设围栏", "专人监护", "绝缘工具"));
        HAZARD_MEASURES.put("高坠", Arrays.asList("系安全带", "戴安全帽", "设防护网", "梯子稳固", "禁止抛投"));
        HAZARD_MEASURES.put("机械伤害", Arrays.asList("设备接地", "防护罩完好", "专人指挥", "禁止超载"));
        HAZARD_MEASURES.put("误操作", Arrays.asList("操作票制度", "双人监护", "唱票复诵", "核对设备"));
        HAZARD_MEASURES.put("物体打击", Arrays.asList("戴安全帽", "工具系绳", "下方禁人", "设置警戒"));
        HAZARD_MEASURES.put("火灾", Arrays.asList("配备灭火器", "清除易燃物", "禁止明火", "防火监护"));
        HAZARD_MEASURES.put("爆炸", Arrays.asList("泄压通风", "禁止火源", "防爆工具", "安全距离"));
        HAZARD_MEASURES.put("窒息", Arrays.asList("通风换气", "气体检测", "专人监护", "救援设备"));
    }

    public AgentService(TaskMapper taskMapper, TaskParseMapper taskParseMapper,
                        HazardMapper hazardMapper, HazardDictMapper hazardDictMapper,
                        MeasureDictMapper measureDictMapper, SafetyMeasureMapper safetyMeasureMapper,
                        DisclosureMapper disclosureMapper, AccidentCaseMapper accidentCaseMapper) {
        this.taskMapper = taskMapper;
        this.taskParseMapper = taskParseMapper;
        this.hazardMapper = hazardMapper;
        this.hazardDictMapper = hazardDictMapper;
        this.measureDictMapper = measureDictMapper;
        this.safetyMeasureMapper = safetyMeasureMapper;
        this.disclosureMapper = disclosureMapper;
        this.accidentCaseMapper = accidentCaseMapper;
    }

    public Map<String, Object> parseTask(String taskText) {
        Map<String, Object> result = new HashMap<>();
        result.put("originalText", taskText);

        String voltageLevel = extractVoltage(taskText);
        result.put("voltageLevel", voltageLevel);

        String equipmentType = extractEquipment(taskText);
        result.put("equipmentType", equipmentType);

        String workType = extractWorkType(taskText);
        result.put("workType", workType);

        String location = extractLocation(taskText);
        result.put("location", location);

        String envConditions = inferEnvironment(taskText);
        result.put("envConditions", envConditions);

        return result;
    }

    private String extractVoltage(String text) {
        Matcher matcher = VOLTAGE_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group(1) + "kV";
        }
        Matcher lowVoltageMatcher = LOW_VOLTAGE_PATTERN.matcher(text);
        if (lowVoltageMatcher.find()) {
            return "低压";
        }
        return "未知";
    }

    private String extractEquipment(String text) {
        Matcher matcher = EQUIPMENT_PATTERN.matcher(text);
        List<String> equipments = new ArrayList<>();
        while (matcher.find()) {
            equipments.add(matcher.group(1));
        }
        return equipments.isEmpty() ? "未知" : String.join("/", equipments);
    }

    private String extractWorkType(String text) {
        Matcher matcher = WORK_TYPE_PATTERN.matcher(text);
        List<String> types = new ArrayList<>();
        while (matcher.find()) {
            types.add(matcher.group(1));
        }
        return types.isEmpty() ? "未知" : String.join("/", types);
    }

    private String extractLocation(String text) {
        Matcher matcher = LOCATION_PATTERN.matcher(text);
        List<String> locations = new ArrayList<>();
        while (matcher.find()) {
            locations.add(matcher.group(1));
        }
        return locations.isEmpty() ? "未知" : String.join("/", locations);
    }

    private String inferEnvironment(String text) {
        List<String> conditions = new ArrayList<>();
        if (text.contains("雨天") || text.contains("下雨") || text.contains("暴雨")) {
            conditions.add("雨天");
        }
        if (text.contains("大风") || text.contains("阵风")) {
            conditions.add("大风");
        }
        if (text.contains("夜间") || text.contains("晚上")) {
            conditions.add("夜间");
        }
        if (text.contains("高温") || text.contains("炎热")) {
            conditions.add("高温");
        }
        if (text.contains("寒冷") || text.contains("低温")) {
            conditions.add("寒冷");
        }
        return conditions.isEmpty() ? "正常" : String.join("/", conditions);
    }

    public Task createTaskWithParse(String taskName, String taskDesc, String taskText) {
        Map<String, Object> parseResult = parseTask(taskText);

        Task task = new Task();
        // 截断超长字段，避免数据库报错
        task.setTaskName(truncate(taskName, 200));
        task.setTaskDesc(taskDesc); // TEXT类型，不限长度
        task.setVoltageLevel(truncate((String) parseResult.get("voltageLevel"), 20));
        task.setEquipmentType(truncate((String) parseResult.get("equipmentType"), 100));
        task.setWorkType(truncate((String) parseResult.get("workType"), 50));
        task.setEnvConditions(truncate((String) parseResult.get("envConditions"), 50));
        task.setStatus("pending");
        task.setCreateTime(LocalDateTime.now());
        taskMapper.insert(task);

        TaskParse taskParse = new TaskParse();
        taskParse.setTaskId(task.getId());
        taskParse.setOriginalText(truncate(taskText, 5000));
        taskParse.setVoltageLevel(truncate((String) parseResult.get("voltageLevel"), 20));
        taskParse.setEquipmentType(truncate((String) parseResult.get("equipmentType"), 100));
        taskParse.setWorkType(truncate((String) parseResult.get("workType"), 50));
        taskParse.setEnvConditions(truncate((String) parseResult.get("envConditions"), 50));
        taskParse.setLocation(truncate((String) parseResult.get("location"), 200));
        taskParse.setParseResult(truncate(parseResult.toString(), 5000));
        taskParse.setCreateTime(LocalDateTime.now());
        taskParseMapper.insert(taskParse);

        return task;
    }

    /**
     * 截断字符串，防止数据库字段超长
     */
    private String truncate(String value, int maxLength) {
        if (value == null) return null;
        if (value.length() <= maxLength) return value;
        return value.substring(0, maxLength);
    }

    public List<Hazard> identifyHazards(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        String taskText = task.getTaskName() + " " + (task.getTaskDesc() != null ? task.getTaskDesc() : "");
        List<Hazard> hazards = new ArrayList<>();

        for (Map.Entry<String, List<String>> entry : HAZARD_RULES.entrySet()) {
            String hazardName = entry.getKey();
            List<String> keywords = entry.getValue();

            for (String keyword : keywords) {
                if (taskText.contains(keyword)) {
                    Hazard hazard = new Hazard();
                    hazard.setTaskId(taskId);
                    hazard.setHazardName(hazardName);
                    hazard.setCategory("自动识别");
                    hazard.setHazardLevel(determineRiskLevel(hazardName, task.getVoltageLevel()));
                    hazard.setLocation(task.getTaskName());
                    hazard.setDescription("识别到危险点：" + hazardName + "，关联关键词：" + keyword);
                    hazard.setCreateTime(LocalDateTime.now());
                    hazards.add(hazard);
                    break;
                }
            }
        }

        List<AccidentCase> similarCases = findSimilarCases(task);
        for (AccidentCase accidentCase : similarCases) {
            if (accidentCase.getHazardPoints() != null) {
                String[] points = accidentCase.getHazardPoints().split(";");
                for (String point : points) {
                    String pointTrimmed = point.trim();
                    boolean exists = hazards.stream().anyMatch(h -> h.getHazardName().equals(pointTrimmed));
                    if (!exists) {
                        Hazard hazard = new Hazard();
                        hazard.setTaskId(taskId);
                        hazard.setHazardName(pointTrimmed);
                        hazard.setCategory("案例匹配");
                        hazard.setHazardLevel(accidentCase.getSeverity());
                        hazard.setLocation(task.getTaskName());
                        hazard.setDescription("来自事故案例：" + accidentCase.getCaseName());
                        hazard.setSourceCase(accidentCase.getCaseName());
                        hazard.setSimilarity(0.8);
                        hazard.setCreateTime(LocalDateTime.now());
                        hazards.add(hazard);
                    }
                }
            }
        }

        hazards.sort((a, b) -> {
            int levelCompare = getLevelPriority(b.getHazardLevel()) - getLevelPriority(a.getHazardLevel());
            return levelCompare != 0 ? levelCompare : a.getHazardName().compareTo(b.getHazardName());
        });

        for (Hazard hazard : hazards) {
            hazardMapper.insert(hazard);
        }

        return hazards;
    }

    private String determineRiskLevel(String hazardName, String voltageLevel) {
        if (hazardName.equals("触电") && voltageLevel != null && !voltageLevel.equals("未知")) {
            if (voltageLevel.contains("低压") || voltageLevel.contains("380V") || voltageLevel.contains("220V") || voltageLevel.contains("0.4kV")) {
                return "high";
            }
            int voltage = 0;
            try {
                voltage = Integer.parseInt(voltageLevel.replace("kV", ""));
            } catch (Exception e) {
            }
            if (voltage >= 10) return "high";
            if (voltage > 0) return "medium";
            return "high";
        }
        return switch (hazardName) {
            case "高坠", "爆炸", "火灾" -> "high";
            case "机械伤害", "窒息" -> "medium";
            default -> "low";
        };
    }

    private int getLevelPriority(String level) {
        return switch (level) {
            case "high", "重大" -> 3;
            case "medium", "严重" -> 2;
            case "low", "一般" -> 1;
            default -> 0;
        };
    }

    private List<AccidentCase> findSimilarCases(Task task) {
        List<AccidentCase> allCases = accidentCaseMapper.selectList(null);
        return allCases.stream()
                .filter(c -> {
                    int matchCount = 0;
                    if (task.getWorkType() != null && c.getWorkType() != null &&
                            task.getWorkType().contains(c.getWorkType())) {
                        matchCount++;
                    }
                    if (task.getEquipmentType() != null && c.getEquipmentType() != null &&
                            task.getEquipmentType().contains(c.getEquipmentType())) {
                        matchCount++;
                    }
                    if (task.getVoltageLevel() != null && c.getVoltageLevel() != null &&
                            task.getVoltageLevel().equals(c.getVoltageLevel())) {
                        matchCount++;
                    }
                    return matchCount >= 1;
                })
                .limit(3)
                .collect(Collectors.toList());
    }

    public List<SafetyMeasure> generateMeasures(Long hazardId) {
        Hazard hazard = hazardMapper.selectById(hazardId);
        if (hazard == null) {
            throw new BusinessException("危险源不存在");
        }

        List<SafetyMeasure> measures = new ArrayList<>();
        List<String> measureNames = HAZARD_MEASURES.getOrDefault(hazard.getHazardName(), new ArrayList<>());

        for (int i = 0; i < measureNames.size(); i++) {
            SafetyMeasure measure = new SafetyMeasure();
            measure.setHazardId(hazardId);
            measure.setMeasureCode("MEAS" + String.format("%03d", i + 1));
            measure.setMeasureName(measureNames.get(i));
            measure.setMeasureDesc("针对危险点[" + hazard.getHazardName() + "]的控制措施");
            measure.setPriority(i + 1);
            measure.setStatus("pending");
            safetyMeasureMapper.insert(measure);
            measures.add(measure);
        }

        List<MeasureDict> dictMeasures = measureDictMapper.selectByHazardCode(hazard.getHazardName());
        for (MeasureDict dict : dictMeasures) {
            boolean exists = measures.stream().anyMatch(m -> m.getMeasureName().equals(dict.getDictName()));
            if (!exists) {
                SafetyMeasure measure = new SafetyMeasure();
                measure.setHazardId(hazardId);
                measure.setMeasureCode(dict.getDictCode());
                measure.setMeasureName(dict.getDictName());
                measure.setMeasureDesc(dict.getDictDesc());
                measure.setPriority(dict.getPriority() != null ? dict.getPriority() : measures.size() + 1);
                measure.setStatus("pending");
                safetyMeasureMapper.insert(measure);
                measures.add(measure);
            }
        }

        measures.sort(Comparator.comparingInt(SafetyMeasure::getPriority));
        return measures;
    }

    public Disclosure generateDisclosureCard(Long taskId) {
        Task task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new BusinessException("任务不存在");
        }

        List<Hazard> hazards = hazardMapper.selectByTaskId(taskId);
        List<SafetyMeasure> allMeasures = new ArrayList<>();
        for (Hazard hazard : hazards) {
            allMeasures.addAll(safetyMeasureMapper.selectByHazardId(hazard.getId()));
        }

        StringBuilder content = new StringBuilder();
        content.append("【安全交底卡】\n\n");
        content.append("一、作业任务\n");
        content.append("任务名称：").append(task.getTaskName()).append("\n");
        content.append("任务描述：").append(task.getTaskDesc() != null ? task.getTaskDesc() : "-").append("\n");
        content.append("电压等级：").append(task.getVoltageLevel()).append("\n");
        content.append("设备类型：").append(task.getEquipmentType()).append("\n");
        content.append("作业类型：").append(task.getWorkType()).append("\n");
        content.append("环境条件：").append(task.getEnvConditions()).append("\n\n");

        content.append("二、危险点识别\n");
        if (hazards.isEmpty()) {
            content.append("暂无危险点\n");
        } else {
            for (int i = 0; i < hazards.size(); i++) {
                Hazard h = hazards.get(i);
                content.append((i + 1)).append(". ").append(h.getHazardName());
                content.append("（风险等级：").append(getLevelText(h.getHazardLevel())).append("）\n");
                content.append("   描述：").append(h.getDescription()).append("\n");
            }
        }
        content.append("\n");

        content.append("三、控制措施\n");
        if (allMeasures.isEmpty()) {
            content.append("暂无控制措施\n");
        } else {
            for (int i = 0; i < allMeasures.size(); i++) {
                SafetyMeasure m = allMeasures.get(i);
                content.append((i + 1)).append(". ").append(m.getMeasureName());
                content.append("（优先级：").append(m.getPriority()).append("）\n");
                content.append("   描述：").append(m.getMeasureDesc()).append("\n");
            }
        }
        content.append("\n");

        content.append("四、应急信息\n");
        content.append("紧急联系人：待填写\n");
        content.append("应急逃生路线：待填写\n");
        content.append("救援电话：120 / 119\n\n");

        content.append("五、交底确认\n");
        content.append("作业负责人：___________\n");
        content.append("作业人员：___________\n");
        content.append("交底时间：___________\n");

        Disclosure disclosure = new Disclosure();
        disclosure.setTaskId(taskId);
        disclosure.setTitle("安全交底卡 - " + task.getTaskName());
        disclosure.setDisclosureNo("JD" + System.currentTimeMillis());
        disclosure.setDisclosureType("作业前交底");
        disclosure.setContent(content.toString());
        disclosure.setEmergencyContact("待确认");
        disclosure.setEmergencyRoute("待规划");
        disclosure.setDisclosureStatus("draft");
        disclosure.setCreateTime(LocalDateTime.now());
        disclosureMapper.insert(disclosure);

        return disclosure;
    }

    private String getLevelText(String level) {
        if ("high".equals(level)) {
            return "高风险";
        } else if ("medium".equals(level)) {
            return "中风险";
        } else if ("low".equals(level)) {
            return "低风险";
        } else if ("重大".equals(level) || "严重".equals(level) || "一般".equals(level)) {
            return level;
        }
        return level;
    }

    public Map<String, Object> completeAgentFlow(String taskName, String taskDesc, String taskText) {
        Map<String, Object> result = new HashMap<>();

        Task task = createTaskWithParse(taskName, taskDesc, taskText);
        result.put("task", task);

        List<Hazard> hazards = identifyHazards(task.getId());
        result.put("hazards", hazards);

        List<SafetyMeasure> measures = new ArrayList<>();
        for (Hazard hazard : hazards) {
            measures.addAll(generateMeasures(hazard.getId()));
        }
        result.put("measures", measures);

        Disclosure disclosure = generateDisclosureCard(task.getId());
        result.put("disclosure", disclosure);

        return result;
    }
}