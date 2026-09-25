package org.example.eledangercheck.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.config.DeepSeekConfig;
import org.example.eledangercheck.entity.AccidentCase;
import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.entity.HazardDict;
import org.example.eledangercheck.entity.MeasureDict;
import org.example.eledangercheck.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 统一AI对话服务
 * - 基于DeepSeek API实现
 * - 统一输出格式（Markdown + emoji）
 * - 知识库约束（数据库案例、危险源、安全措施）
 * - 确定性回答（temperature=0 + 固定知识库上下文）
 * - 非电力安全内容拦截
 */
@Service
public class UnifiedAiService {

    private static final Logger logger = LoggerFactory.getLogger(UnifiedAiService.class);

    private final AccidentCaseService accidentCaseService;
    private final HazardService hazardService;
    private final SafetyMeasureService safetyMeasureService;
    private final DictService dictService;
    private final DeepSeekConfig deepSeekConfig;
    private final RestTemplate restTemplate;

    // 统一的系统提示词
    private static final String SYSTEM_PROMPT_TEMPLATE = 
        "你是电力安全智能助手，专门回答电力作业安全相关问题。\n\n" +
        "【核心原则 - 必须严格遵守】\n" +
        "1. 只回答电力安全相关问题，不相关的直接说\"抱歉，我只能回答电力安全相关问题\"\n" +
        "2. 严格基于提供的知识库内容回答，不要编造不存在的案例或措施\n" +
        "3. 回答要专业、准确、符合电力行业规范\n" +
        "4. 始终使用中文回答\n\n" +
        "【输出格式 - 必须100%严格按照以下格式输出，不得更改顺序和标题】\n" +
        "## 📋 分析结论\n" +
        "（用1-2句话总结核心结论）\n\n" +
        "### ⚠️ 危险点辨识\n" +
        "（按风险等级从高到低列出，每条格式：- **风险名称（风险等级）**: 详细描述）\n\n" +
        "### 🛡️ 安全控制措施\n" +
        "（按优先级排序，编号列出，每条格式：1. **措施名称**：具体操作要求）\n\n" +
        "### 📚 参考案例\n" +
        "（如果知识库中有相关案例，列出案例名称和关键教训；没有就写\"暂无相关案例\"）\n\n" +
        "### 💡 注意事项\n" +
        "（3-5条关键提醒，用- ✅ 开头）\n\n" +
        "【知识库参考资料】\n" +
        "{{KNOWLEDGE_BASE}}\n\n" +
        "重要提醒：必须严格按照上面的输出格式回答，每个章节标题必须完全一致，包括emoji符号。";

    public UnifiedAiService(AccidentCaseService accidentCaseService,
                            HazardService hazardService,
                            SafetyMeasureService safetyMeasureService,
                            DictService dictService,
                            DeepSeekConfig deepSeekConfig,
                            RestTemplate restTemplate) {
        this.accidentCaseService = accidentCaseService;
        this.hazardService = hazardService;
        this.safetyMeasureService = safetyMeasureService;
        this.dictService = dictService;
        this.deepSeekConfig = deepSeekConfig;
        this.restTemplate = restTemplate;
    }

    /**
     * 主聊天接口
     */
    public Map<String, Object> chat(String userMessage, String conversationId) {
        Map<String, Object> result = new HashMap<>();

        // 第一步：检查是否为电力安全相关问题
        if (!isPowerSafetyRelated(userMessage)) {
            result.put("rejected", true);
            result.put("message", buildRejectMessage());
            return result;
        }

        // 第二步：检索知识库（确定性格式，保证每次相同问题检索结果一致）
        String knowledgeContext = buildKnowledgeContext(userMessage);

        // 第三步：调用AI模型
        String response = null;
        String usedModel = "API (DeepSeek)";

        try {
            response = callApiModel(userMessage, knowledgeContext);
            logger.info("使用API模型回答成功");
        } catch (Exception e) {
            logger.error("模型调用失败: {}", e.getMessage());
            result.put("error", "模型调用失败: " + e.getMessage());
            result.put("rejected", false);
            result.put("answer", null);
            result.put("model", usedModel);
            return result;
        }

        // 第四步：统一输出格式
        response = normalizeAnswer(response, userMessage, knowledgeContext);

        result.put("rejected", false);
        result.put("answer", response);
        result.put("model", usedModel);
        return result;
    }

    /**
     * 构建拒绝回答的消息
     */
    private String buildRejectMessage() {
        return "抱歉，我是电力安全智能助手，专门回答电力作业安全相关的问题。\n\n" +
                "我可以帮助您：\n" +
                "• 🔍 分析电力作业风险与危险点辨识\n" +
                "• 🛡️ 提供安全防护措施建议\n" +
                "• 📚 查询电力事故案例及经验教训\n" +
                "• 📋 生成安全交底卡内容\n" +
                "• 📖 解答电力安全规程问题\n\n" +
                "请您提出电力安全相关的问题，我会尽力为您专业解答。";
    }

    /**
     * 判断问题是否与电力安全相关
     */
    private boolean isPowerSafetyRelated(String message) {
        if (message == null || message.trim().isEmpty()) {
            return false;
        }
        String msg = message.toLowerCase();

        // 电力安全相关关键词（命中任意一个即认为相关）
        String[] powerSafetyKeywords = {
            // 电力核心词
            "电力", "电气", "电工", "高压", "低压", "千伏", "kv", "10kv", "35kv", "110kv", "220kv",
            "触电", "漏电", "短路", "接地", "绝缘", "停电", "送电", "倒闸", "带电", "验电",
            // 设备设施
            "变压器", "开关柜", "配电柜", "断路器", "隔离开关", "电缆", "线路", "母线",
            "变电站", "配电室", "杆塔", "登杆", "铁塔", "导线", "刀闸", "熔断器",
            // 作业类型
            "作业", "施工", "检修", "运维", "巡检", "试验", "安装", "拆除", "吊装",
            "高处", "登高", "坠落", "有限空间", "密闭空间",
            // 安全相关
            "安全", "风险", "危险", "隐患", "防护", "交底", "措施", "规程", "规范",
            "安全带", "安全帽", "绝缘手套", "绝缘靴", "接地线", "挂牌",
            // 事故应急
            "事故", "案例", "应急", "救援", "火灾", "爆炸", "中毒", "窒息",
            // 问候语
            "你好", "您好", "hi", "hello", "在吗", "能做什么", "功能"
        };

        for (String keyword : powerSafetyKeywords) {
            if (msg.contains(keyword.toLowerCase())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 构建知识库上下文（确定性排序，保证每次相同问题结果一致）
     */
    private String buildKnowledgeContext(String userMessage) {
        StringBuilder context = new StringBuilder();

        // 1. 检索相关事故案例（按相关度排序，取前3个）
        try {
            List<AccidentCase> cases = accidentCaseService.getAllAccidentCases();
            List<AccidentCase> relevantCases = filterAndSortCases(cases, userMessage, 3);
            if (!relevantCases.isEmpty()) {
                context.append("【事故案例库】\n");
                for (int i = 0; i < relevantCases.size(); i++) {
                    AccidentCase c = relevantCases.get(i);
                    context.append("案例" + (i+1) + ": " + c.getCaseName() + "\n");
                    context.append("  类型: " + c.getAccidentType() + " | 电压等级: " + c.getVoltageLevel() + " | 严重程度: " + c.getSeverity() + "\n");
                    context.append("  危险点: " + c.getHazardPoints() + "\n");
                    context.append("  原因分析: " + c.getCauseAnalysis() + "\n");
                    context.append("  经验教训: " + c.getLessonsLearned() + "\n\n");
                }
            }
        } catch (Exception e) {
            logger.warn("加载事故案例失败: {}", e.getMessage());
        }

        // 2. 检索相关危险源（优先从字典表取，按相关度排序，取前5个）
        try {
            List<HazardDict> hazardDicts = dictService.getAllHazardDicts();
            List<HazardDict> relevantHazardDicts = filterAndSortHazardDicts(hazardDicts, userMessage, 5);
            if (!relevantHazardDicts.isEmpty()) {
                context.append("【危险源库】\n");
                for (HazardDict h : relevantHazardDicts) {
                    context.append("- " + h.getDictName() + "（" + h.getLevel() + "风险）: " + h.getDictDesc() + "\n");
                }
                context.append("\n");
            }
        } catch (Exception e) {
            logger.warn("加载危险源字典失败: {}", e.getMessage());
        }

        // 3. 检索相关安全措施（优先从字典表取，按相关度排序，取前6个）
        try {
            List<MeasureDict> measureDicts = dictService.getAllMeasureDicts();
            List<MeasureDict> relevantMeasureDicts = filterAndSortMeasureDicts(measureDicts, userMessage, 6);
            if (!relevantMeasureDicts.isEmpty()) {
                context.append("【安全措施库】\n");
                for (MeasureDict m : relevantMeasureDicts) {
                    context.append("- " + m.getDictName() + ": " + m.getDictDesc() + "\n");
                }
                context.append("\n");
            }
        } catch (Exception e) {
            logger.warn("加载安全措施字典失败: {}", e.getMessage());
        }

        return context.toString();
    }

    // ========== 知识库检索排序（确定性算法，保证每次结果一致） ==========

    private List<AccidentCase> filterAndSortCases(List<AccidentCase> all, String query, int limit) {
        String q = query.toLowerCase();
        return all.stream()
                .map(c -> new AbstractMap.SimpleEntry<>(c, calcRelevanceScore(
                    c.getCaseName() + " " + c.getAccidentType() + " " + c.getWorkType() +
                    " " + c.getVoltageLevel() + " " + c.getHazardPoints() + " " + c.getLessonsLearned(), q)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> {
                    // 先按相关度降序，再按ID升序（保证确定性）
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Long.compare(a.getKey().getId(), b.getKey().getId());
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<Hazard> filterAndSortHazards(List<Hazard> all, String query, int limit) {
        String q = query.toLowerCase();
        return all.stream()
                .map(h -> new AbstractMap.SimpleEntry<>(h, calcRelevanceScore(
                    h.getHazardName() + " " + h.getDescription() + " " + h.getCategory(), q)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Long.compare(a.getKey().getId(), b.getKey().getId());
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<HazardDict> filterAndSortHazardDicts(List<HazardDict> all, String query, int limit) {
        String q = query.toLowerCase();
        return all.stream()
                .map(h -> new AbstractMap.SimpleEntry<>(h, calcRelevanceScore(
                    h.getDictName() + " " + h.getDictDesc() + " " + h.getDictCode(), q)))
                .filter(e -> e.getValue() >= 0)  // 字典表数据全部返回，按相关度排序
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Long.compare(a.getKey().getId(), b.getKey().getId());
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<SafetyMeasure> filterAndSortMeasures(List<SafetyMeasure> all, String query, int limit) {
        String q = query.toLowerCase();
        return all.stream()
                .map(m -> new AbstractMap.SimpleEntry<>(m, calcRelevanceScore(
                    m.getMeasureName() + " " + m.getMeasureDesc() + " " + m.getMeasureCode(), q)))
                .filter(e -> e.getValue() > 0)
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    return Long.compare(a.getKey().getId(), b.getKey().getId());
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<MeasureDict> filterAndSortMeasureDicts(List<MeasureDict> all, String query, int limit) {
        String q = query.toLowerCase();
        return all.stream()
                .map(m -> new AbstractMap.SimpleEntry<>(m, calcRelevanceScore(
                    m.getDictName() + " " + m.getDictDesc() + " " + m.getDictCode() + " " + m.getHazardCode(), q)))
                .filter(e -> e.getValue() >= 0)  // 字典表数据全部返回，按相关度排序
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) return cmp;
                    // 按优先级排序
                    int priA = a.getKey().getPriority() != null ? a.getKey().getPriority() : 999;
                    int priB = b.getKey().getPriority() != null ? b.getKey().getPriority() : 999;
                    int priCmp = Integer.compare(priA, priB);
                    if (priCmp != 0) return priCmp;
                    return Long.compare(a.getKey().getId(), b.getKey().getId());
                })
                .limit(limit)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    /**
     * 计算文本相关度得分（确定性算法）
     */
    private int calcRelevanceScore(String text, String query) {
        String t = text.toLowerCase();
        String q = query.toLowerCase();
        int score = 0;
        // 关键词匹配得分
        String[] words = q.split("[\\s,，。.！!？?；;]+");
        for (String word : words) {
            if (word.length() >= 2 && t.contains(word)) {
                score += word.length() * 2;
            }
        }
        return score;
    }

    // ========== API模型调用 ==========

    private String callApiModel(String userMessage, String knowledgeContext) {
        String apiKey = deepSeekConfig.getApiKey();
        String baseUrl = deepSeekConfig.getBaseUrl();

        if (apiKey == null || apiKey.isEmpty()) {
            throw new BusinessException("API Key未配置");
        }

        String url = baseUrl + "/chat/completions";

        JSONArray messages = buildUnifiedMessages(userMessage, knowledgeContext);

        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-v4-pro");
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.0);  // 确定性回答
        requestBody.put("max_tokens", 2000);
        requestBody.put("top_p", 1.0);
        requestBody.put("frequency_penalty", 0.0);
        requestBody.put("presence_penalty", 0.0);

        org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
        httpHeaders.set("Authorization", "Bearer " + apiKey);
        httpHeaders.set("Content-Type", "application/json");

        org.springframework.http.HttpEntity<String> entity =
                new org.springframework.http.HttpEntity<>(requestBody.toJSONString(), httpHeaders);

        String response = restTemplate.postForObject(url, entity, String.class);
        JSONObject responseJson = JSON.parseObject(response);

        if (responseJson.containsKey("choices")) {
            JSONArray choices = responseJson.getJSONArray("choices");
            if (!choices.isEmpty()) {
                JSONObject choice = choices.getJSONObject(0);
                JSONObject message = choice.getJSONObject("message");
                return message.getString("content");
            }
        }
        throw new BusinessException("API返回格式异常");
    }

    // ========== 统一消息构建 ==========

    private JSONArray buildUnifiedMessages(String userMessage, String knowledgeContext) {
        JSONArray messages = new JSONArray();

        // System prompt
        String systemPrompt = SYSTEM_PROMPT_TEMPLATE.replace("{{KNOWLEDGE_BASE}}",
                knowledgeContext.isEmpty() ? "（暂无相关知识库数据）" : knowledgeContext);

        JSONObject systemMsg = new JSONObject();
        systemMsg.put("role", "system");
        systemMsg.put("content", systemPrompt);
        messages.add(systemMsg);

        // 用户消息
        String userPrompt = "请严格按照上面规定的输出格式回答以下问题。\n\n" +
                "问题：" + userMessage + "\n\n" +
                "再次强调输出格式：\n" +
                "## 📋 分析结论\n" +
                "### ⚠️ 危险点辨识\n" +
                "### 🛡️ 安全控制措施\n" +
                "### 📚 参考案例\n" +
                "### 💡 注意事项\n" +
                "必须完全按照这个结构输出，章节标题不能改。";

        JSONObject userMsg = new JSONObject();
        userMsg.put("role", "user");
        userMsg.put("content", userPrompt);
        messages.add(userMsg);

        return messages;
    }

    // ========== 输出格式统一化 ==========

    /**
     * 标准化回答格式
     * 如果模型输出已经符合标准格式，则直接返回
     * 如果不符合，则提取关键信息并重新格式化为标准结构
     */
    private String normalizeAnswer(String answer, String userMessage, String knowledgeContext) {
        if (answer == null || answer.trim().isEmpty()) {
            return generateKnowledgeBasedAnswer(userMessage, knowledgeContext);
        }

        // 检查是否已经符合标准格式（包含所有5个章节标题）
        boolean hasConclusion = answer.contains("📋 分析结论") || answer.contains("分析结论");
        boolean hasHazards = answer.contains("⚠️ 危险点辨识") || answer.contains("危险点辨识") || answer.contains("危险点");
        boolean hasMeasures = answer.contains("🛡️ 安全控制措施") || answer.contains("安全控制措施") || answer.contains("控制措施");
        boolean hasCases = answer.contains("📚 参考案例") || answer.contains("参考案例");
        boolean hasNotes = answer.contains("💡 注意事项") || answer.contains("注意事项");

        // 如果已经有至少3个标准章节，认为格式基本符合，做轻微修正
        int matchCount = (hasConclusion?1:0) + (hasHazards?1:0) + (hasMeasures?1:0) + (hasCases?1:0) + (hasNotes?1:0);
        if (matchCount >= 3) {
            return fixFormatting(answer);
        }

        // 格式完全不符合，基于知识库 + 模型回答内容，重新生成标准格式
        return rebuildStandardFormat(answer, userMessage, knowledgeContext);
    }

    /**
     * 轻微修正格式（补齐缺失的章节标题emoji和格式）
     */
    private String fixFormatting(String answer) {
        String result = answer;
        // 统一章节标题格式
        result = result.replaceAll("##\\s*分析结论", "## 📋 分析结论");
        result = result.replaceAll("###\\s*危险点辨识", "### ⚠️ 危险点辨识");
        result = result.replaceAll("###\\s*安全控制措施", "### 🛡️ 安全控制措施");
        result = result.replaceAll("###\\s*参考案例", "### 📚 参考案例");
        result = result.replaceAll("###\\s*注意事项", "### 💡 注意事项");
        return result;
    }

    /**
     * 基于模型回答 + 知识库，重新构建标准格式的回答
     * 保证输出格式100%一致
     */
    private String rebuildStandardFormat(String modelAnswer, String userMessage, String knowledgeContext) {
        StringBuilder result = new StringBuilder();
        String msg = userMessage.toLowerCase();

        // 提取模型回答中的纯文本（去除markdown标记）
        String plainText = modelAnswer
                .replaceAll("#+\\s*", "")
                .replaceAll("\\*\\*", "")
                .replaceAll("-\\s+", "")
                .replaceAll("\\d+\\.\\s+", "")
                .replaceAll("\\|.*\\|", "")
                .replaceAll("---+", "")
                .trim();

        // ===== 分析结论 =====
        result.append("## 📋 分析结论\n\n");
        // 从模型回答中提取前2句话作为结论
        String[] sentences = plainText.split("[。！？.!?\n]");
        StringBuilder conclusion = new StringBuilder();
        int count = 0;
        for (String s : sentences) {
            String trimmed = s.trim();
            if (trimmed.length() > 5 && count < 2) {
                conclusion.append(trimmed).append("。");
                count++;
            }
        }
        if (conclusion.length() > 0) {
            result.append(conclusion.toString()).append("\n\n");
        } else {
            // 兜底结论
            if (msg.contains("停电")) {
                result.append("**停电作业**需严格执行停电、验电、接地、挂牌、闭锁的\"五步操作法\"，确保作业安全。\n\n");
            } else if (msg.contains("高处") || msg.contains("登杆") || msg.contains("高空")) {
                result.append("**高处作业**存在高坠风险，必须严格落实防坠措施，规范使用安全带。\n\n");
            } else if (msg.contains("带电")) {
                result.append("**带电作业**属于高风险作业，需由持证专业人员严格按规程操作。\n\n");
            } else {
                result.append("该作业场景存在多项安全风险，需严格落实安全防护措施，遵守电力安全工作规程。\n\n");
            }
        }

        // ===== 危险点辨识 =====
        result.append("### ⚠️ 危险点辨识\n\n");
        // 从知识库中提取危险源
        try {
            List<HazardDict> hazards = filterAndSortHazardDicts(dictService.getAllHazardDicts(), userMessage, 5);
            if (!hazards.isEmpty()) {
                for (int i = 0; i < hazards.size(); i++) {
                    HazardDict h = hazards.get(i);
                    result.append((i+1) + ". **" + h.getDictName() + "**（" + h.getLevel() + "风险）\n");
                    result.append("   " + h.getDictDesc() + "\n");
                }
            } else {
                result.append("1. **触电风险**（高风险）：接触带电设备或线路可能导致触电事故\n");
                result.append("2. **高处坠落**（高风险）：高空作业防护不到位可能导致坠落\n");
                result.append("3. **机械伤害**（中风险）：设备操作不当可能造成机械伤害\n");
            }
        } catch (Exception e) {
            result.append("1. **触电风险**（高风险）：接触带电设备可能导致触电\n");
            result.append("2. **高处坠落**（高风险）：高处作业需防坠落\n");
            result.append("3. **误操作风险**（高风险）：误操作可能引发事故\n");
        }
        result.append("\n");

        // ===== 安全控制措施 =====
        result.append("### 🛡️ 安全控制措施\n\n");
        try {
            List<MeasureDict> measures = filterAndSortMeasureDicts(dictService.getAllMeasureDicts(), userMessage, 6);
            if (!measures.isEmpty()) {
                for (int i = 0; i < measures.size(); i++) {
                    MeasureDict m = measures.get(i);
                    result.append((i+1) + ". **" + m.getDictName() + "**\n");
                    result.append("   " + m.getDictDesc() + "\n");
                }
            } else {
                result.append("1. **停电操作**：执行停电、验电、接地、挂牌、隔离五步法\n");
                result.append("2. **个人防护**：使用合格的绝缘工具和防护用品\n");
                result.append("3. **现场管理**：设置安全围栏和警示标志\n");
                result.append("4. **工作确认**：工作前确认所有电源已断开\n");
            }
        } catch (Exception e) {
            result.append("1. **停电操作**：严格执行停电验电程序\n");
            result.append("2. **个人防护**：佩戴安全防护用品\n");
            result.append("3. **现场监护**：设置专人安全监护\n");
        }
        result.append("\n");

        // ===== 参考案例 =====
        result.append("### 📚 参考案例\n\n");
        try {
            List<AccidentCase> cases = filterAndSortCases(accidentCaseService.getAllAccidentCases(), userMessage, 2);
            if (!cases.isEmpty()) {
                for (int i = 0; i < cases.size(); i++) {
                    AccidentCase c = cases.get(i);
                    result.append((i+1) + ". **" + c.getCaseName() + "**\n");
                    result.append("   - 类型：" + c.getAccidentType() + " | 电压：" + c.getVoltageLevel() + " | 程度：" + c.getSeverity() + "\n");
                    result.append("   - 教训：" + truncateText(c.getLessonsLearned(), 60) + "\n");
                }
            } else {
                result.append("暂无相关案例\n");
            }
        } catch (Exception e) {
            result.append("暂无相关案例\n");
        }
        result.append("\n");

        // ===== 注意事项 =====
        result.append("### 💡 注意事项\n\n");
        result.append("- ✅ 作业前必须进行**安全技术交底**，确保全员知晓风险\n");
        result.append("- ✅ 特种作业人员必须**持证上岗**，严禁无证作业\n");
        result.append("- ✅ 作业现场设置**专人监护**，严禁擅自离岗\n");
        result.append("- ✅ 发现异常立即**停止作业**，撤离现场并上报\n");
        result.append("- ✅ 严格执行**两票三制**，严禁违章指挥和违章作业\n");

        result.append("\n---\n");
        result.append("*📌 以上分析基于电力安全知识库及AI生成，仅供参考。实际作业请严格遵守《电力安全工作规程》及现场专项方案。*");

        return result.toString();
    }

    // ========== 知识库增强的规则引擎（降级兜底，确定性回答） ==========

    /**
     * 基于数据库知识库生成回答（确定性算法，保证每次相同问题答案完全一致）
     */
    private String generateKnowledgeBasedAnswer(String userMessage, String knowledgeContext) {
        StringBuilder result = new StringBuilder();
        String msg = userMessage.toLowerCase();

        // 从知识库中提取相关数据
        List<AccidentCase> relevantCases = new ArrayList<>();
        List<HazardDict> relevantHazardDicts = new ArrayList<>();
        List<MeasureDict> relevantMeasureDicts = new ArrayList<>();

        try {
            relevantCases = filterAndSortCases(accidentCaseService.getAllAccidentCases(), userMessage, 2);
            relevantHazardDicts = filterAndSortHazardDicts(dictService.getAllHazardDicts(), userMessage, 5);
            relevantMeasureDicts = filterAndSortMeasureDicts(dictService.getAllMeasureDicts(), userMessage, 6);
        } catch (Exception e) {
            logger.warn("知识库加载失败，使用通用回答: {}", e.getMessage());
        }

        // 如果知识库没有匹配到任何内容，使用通用回答
        if (relevantHazardDicts.isEmpty() && relevantMeasureDicts.isEmpty() && relevantCases.isEmpty()) {
            return generateGeneralAnswer(userMessage);
        }

        // ===== 按统一格式生成回答 =====

        result.append("## 📋 分析结论\n\n");
        result.append("根据电力安全规程和知识库分析，");
        if (msg.contains("停电")) {
            result.append("**停电作业**需严格执行停电、验电、接地、挂牌、闭锁的\"五步操作法\"，");
        } else if (msg.contains("高处") || msg.contains("登杆") || msg.contains("杆塔") || msg.contains("高空")) {
            result.append("**高处作业**存在高坠风险，必须落实防坠措施，");
        } else if (msg.contains("带电")) {
            result.append("**带电作业**属于高风险作业，需由持证专业人员按规程操作，");
        } else if (msg.contains("误操作") || msg.contains("倒闸")) {
            result.append("**倒闸操作**必须严格执行操作票制度，严防误操作，");
        } else {
            result.append("该作业场景存在多项安全风险，");
        }
        result.append("需严格落实以下安全措施。\n\n");

        // 危险点辨识
        result.append("### ⚠️ 危险点辨识\n\n");
        if (!relevantHazardDicts.isEmpty()) {
            for (int i = 0; i < Math.min(relevantHazardDicts.size(), 5); i++) {
                HazardDict h = relevantHazardDicts.get(i);
                result.append((i+1) + ". **" + h.getDictName() + "**（" + h.getLevel() + "风险）\n");
                result.append("   " + h.getDictDesc() + "\n");
            }
        } else {
            result.append("1. **触电风险**：接触带电部位可能导致触电伤亡\n");
            result.append("2. **高处坠落风险**：高处作业防护不到位可能导致坠落\n");
            result.append("3. **机械伤害风险**：设备操作不当可能造成机械伤害\n");
        }
        result.append("\n");

        // 安全控制措施
        result.append("### 🛡️ 安全控制措施\n\n");
        if (!relevantMeasureDicts.isEmpty()) {
            for (int i = 0; i < Math.min(relevantMeasureDicts.size(), 6); i++) {
                MeasureDict m = relevantMeasureDicts.get(i);
                result.append((i+1) + ". **" + m.getDictName() + "**\n");
                result.append("   " + m.getDictDesc() + "\n");
            }
        } else {
            result.append("1. **停电操作**：执行停电、验电、接地、挂牌、隔离五步法\n");
            result.append("2. **个人防护**：使用合格的绝缘工具和防护用品\n");
            result.append("3. **现场管理**：设置安全围栏和警示标志\n");
            result.append("4. **工作确认**：工作前确认所有电源已断开\n");
        }
        result.append("\n");

        // 参考案例
        if (!relevantCases.isEmpty()) {
            result.append("### 📚 参考案例\n\n");
            for (int i = 0; i < relevantCases.size(); i++) {
                AccidentCase c = relevantCases.get(i);
                result.append((i+1) + ". **" + c.getCaseName() + "**\n");
                result.append("   - 类型：" + c.getAccidentType() + " | 电压：" + c.getVoltageLevel() + " | 程度：" + c.getSeverity() + "\n");
                result.append("   - 教训：" + truncateText(c.getLessonsLearned(), 80) + "\n");
            }
            result.append("\n");
        }

        // 注意事项
        result.append("### 💡 注意事项\n\n");
        result.append("- ✅ 作业前必须进行**安全技术交底**，确保全员知晓风险\n");
        result.append("- ✅ 特种作业人员必须**持证上岗**，严禁无证作业\n");
        result.append("- ✅ 作业现场设置**专人监护**，严禁擅自离岗\n");
        result.append("- ✅ 发现异常立即**停止作业**，撤离现场并上报\n");
        result.append("- ✅ 严格执行**两票三制**，严禁违章指挥和违章作业\n");

        result.append("\n---\n");
        result.append("*📌 以上分析基于电力安全知识库生成，仅供参考。实际作业请严格遵守《电力安全工作规程》及现场专项方案。*");

        return result.toString();
    }

    /**
     * 通用安全回答（当知识库没有匹配内容时使用）
     */
    private String generateGeneralAnswer(String userMessage) {
        return "## 📋 电力安全分析\n\n" +
                "### ⚠️ 主要危险点\n\n" +
                "1. **触电风险**：接触带电设备或线路可能导致触电事故\n" +
                "2. **高处坠落风险**：高空作业防护不到位可能导致坠落伤害\n" +
                "3. **机械伤害风险**：设备操作不当可能造成机械伤害\n" +
                "4. **火灾风险**：电气设备故障可能引发火灾\n\n" +
                "### 🛡️ 安全控制措施\n\n" +
                "1. **停电作业**：严格执行停电、验电、接地、挂牌、闭锁\"五步操作法\"\n" +
                "2. **个人防护**：正确佩戴安全帽、安全带，使用合格绝缘工具\n" +
                "3. **现场管理**：设置安全围栏和警示标志，无关人员严禁入内\n" +
                "4. **工作确认**：工作前确认设备状态，履行工作票手续\n" +
                "5. **监护制度**：作业现场设专人监护，及时纠正违章行为\n\n" +
                "### 💡 注意事项\n\n" +
                "- ✅ 作业前必须进行安全技术交底\n" +
                "- ✅ 特种作业人员必须持证上岗\n" +
                "- ✅ 严格执行两票三制\n" +
                "- ✅ 发现异常立即停止作业\n\n" +
                "---\n" +
                "*📌 以上为通用安全建议，具体作业请结合现场实际制定专项方案。如需更精准的分析，请提供更详细的作业信息。*";
    }

    /**
     * 截断文本
     */
    private String truncateText(String text, int maxLen) {
        if (text == null) return "";
        if (text.length() <= maxLen) return text;
        return text.substring(0, maxLen) + "...";
    }
}
