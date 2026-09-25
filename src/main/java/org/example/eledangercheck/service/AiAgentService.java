package org.example.eledangercheck.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.config.DeepSeekConfig;
import org.example.eledangercheck.config.SimulationConfig;
import org.example.eledangercheck.entity.AccidentCase;
import org.example.eledangercheck.entity.Disclosure;
import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.entity.Task;
import org.example.eledangercheck.exception.BusinessException;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AiAgentService {

    private final AgentService agentService;
    private final SimulationConfig simulationConfig;
    private final DeepSeekConfig deepSeekConfig;
    private final AccidentCaseService accidentCaseService;
    private final RestTemplate restTemplate;

    private final Map<String, List<JSONObject>> conversationContexts = new HashMap<>();

    public AiAgentService(AgentService agentService, SimulationConfig simulationConfig,
                          DeepSeekConfig deepSeekConfig, AccidentCaseService accidentCaseService,
                          RestTemplate restTemplate) {
        this.agentService = agentService;
        this.simulationConfig = simulationConfig;
        this.deepSeekConfig = deepSeekConfig;
        this.accidentCaseService = accidentCaseService;
        this.restTemplate = restTemplate;
    }

    public String completeFlow(String taskText) {
        return completeFlow(taskText, null);
    }

    public String completeFlow(String taskText, String conversationId) {
        if (deepSeekConfig.getApiKey() == null || 
            deepSeekConfig.getApiKey().isEmpty() || 
            deepSeekConfig.getApiKey().contains("your_deepseek_api_key")) {
            return analyzeTaskWithoutAI(taskText);
        }

        try {
            List<AccidentCase> relevantCases = retrieveRelevantCases(taskText);
            return callDeepSeekAPI(taskText, conversationId, relevantCases);
        } catch (Exception e) {
            return "⚠️ AI调用失败，切换到本地规则分析：\n" + analyzeTaskWithoutAI(taskText);
        }
    }

    private List<AccidentCase> retrieveRelevantCases(String taskText) {
        List<AccidentCase> allCases = accidentCaseService.getAllAccidentCases();
        if (allCases.isEmpty()) {
            return new ArrayList<>();
        }

        return allCases.stream()
                .filter(caseItem -> isCaseRelevant(caseItem, taskText))
                .limit(3)
                .collect(Collectors.toList());
    }

    private boolean isCaseRelevant(AccidentCase caseItem, String taskText) {
        String taskLower = taskText.toLowerCase();
        String caseLower = (caseItem.getCaseName() + " " + 
                           caseItem.getAccidentType() + " " + 
                           caseItem.getWorkType() + " " + 
                           caseItem.getEquipmentType() + " " +
                           caseItem.getVoltageLevel()).toLowerCase();

        int matchCount = 0;
        
        if (caseItem.getVoltageLevel() != null && taskLower.contains(caseItem.getVoltageLevel().toLowerCase())) {
            matchCount++;
        }
        if (caseItem.getWorkType() != null && taskLower.contains(caseItem.getWorkType().toLowerCase())) {
            matchCount++;
        }
        if (caseItem.getEquipmentType() != null && taskLower.contains(caseItem.getEquipmentType().toLowerCase())) {
            matchCount++;
        }
        if (caseItem.getAccidentType() != null && taskLower.contains(caseItem.getAccidentType().toLowerCase())) {
            matchCount++;
        }

        return matchCount >= 1;
    }

    private String formatCaseContext(List<AccidentCase> cases) {
        if (cases.isEmpty()) {
            return "";
        }
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n\n参考事故案例：\n");
        for (int i = 0; i < cases.size(); i++) {
            AccidentCase c = cases.get(i);
            sb.append((i + 1)).append(". 案例名称：").append(c.getCaseName()).append("\n");
            sb.append("   事故类型：").append(c.getAccidentType()).append("\n");
            sb.append("   电压等级：").append(c.getVoltageLevel()).append("\n");
            sb.append("   设备类型：").append(c.getEquipmentType()).append("\n");
            sb.append("   危险点：").append(c.getHazardPoints()).append("\n");
            sb.append("   经验教训：").append(c.getLessonsLearned()).append("\n\n");
        }
        return sb.toString();
    }

    private String callDeepSeekAPI(String taskText, String conversationId, List<AccidentCase> relevantCases) {
        String url = deepSeekConfig.getBaseUrl() + "/chat/completions";
        
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", "deepseek-v4-pro");
        
        JSONArray messages = new JSONArray();
        
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", "你是一个专业的电力作业安全专家。请根据用户提供的电力作业任务描述，完成以下分析：\n" +
                "1. 解析作业任务：识别电压等级、设备类型、作业类型、作业位置\n" +
                "2. 识别危险点：列出所有可能存在的安全风险\n" +
                "3. 生成控制措施：针对每个危险点提供具体的安全措施\n" +
                "4. 生成安全交底卡内容：汇总所有信息\n" +
                "请参考提供的事故案例，在分析中引用相关经验教训。\n" +
                "请使用清晰的中文格式输出，使用emoji增强可读性。");
        messages.add(systemMessage);

        if (conversationId != null && conversationContexts.containsKey(conversationId)) {
            for (JSONObject contextMsg : conversationContexts.get(conversationId)) {
                messages.add(contextMsg);
            }
        }
        
        String userContent = "请分析以下电力作业任务：\n" + taskText + formatCaseContext(relevantCases);
        
        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", userContent);
        messages.add(userMessage);
        
        requestBody.put("messages", messages);
        requestBody.put("temperature", 0.7);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + deepSeekConfig.getApiKey());
        headers.put("Content-Type", "application/json");

        try {
            org.springframework.http.HttpHeaders httpHeaders = new org.springframework.http.HttpHeaders();
            httpHeaders.setAll(headers);
            org.springframework.http.HttpEntity<String> entity = new org.springframework.http.HttpEntity<>(requestBody.toJSONString(), httpHeaders);
            
            String response = restTemplate.postForObject(url, entity, String.class);
            JSONObject responseJson = JSON.parseObject(response);
            
            if (responseJson.containsKey("choices")) {
                JSONArray choices = responseJson.getJSONArray("choices");
                if (!choices.isEmpty()) {
                    JSONObject choice = choices.getJSONObject(0);
                    JSONObject message = choice.getJSONObject("message");
                    String content = message.getString("content");

                    if (conversationId != null) {
                        List<JSONObject> context = conversationContexts.computeIfAbsent(conversationId, k -> new ArrayList<>());
                        context.add(userMessage);
                        context.add(message);
                        if (context.size() > 20) {
                            context.subList(0, context.size() - 20).clear();
                        }
                    }

                    return content;
                }
            }
            return "AI返回结果格式异常";
        } catch (Exception e) {
            throw new BusinessException("调用DeepSeek API失败: " + e.getMessage());
        }
    }

    public void clearConversation(String conversationId) {
        conversationContexts.remove(conversationId);
    }

    private String analyzeTaskWithoutAI(String taskText) {
        Map<String, Object> parseResult = agentService.parseTask(taskText);
        String voltageLevel = (String) parseResult.get("voltageLevel");
        String equipmentType = (String) parseResult.get("equipmentType");
        String workType = (String) parseResult.get("workType");
        String location = (String) parseResult.get("location");
        String envConditions = (String) parseResult.get("envConditions");

        List<String> hazardNames = identifyHazardsByRules(voltageLevel, equipmentType, workType, envConditions, location);
        
        StringBuilder result = new StringBuilder();
        result.append("🎯 作业任务解析：\n");
        result.append("电压等级：").append(voltageLevel).append("\n");
        result.append("设备类型：").append(equipmentType).append("\n");
        result.append("作业类型：").append(workType).append("\n");
        result.append("位置：").append(location).append("\n\n");
        
        result.append("⚠️ 危险点识别：\n");
        for (int i = 0; i < hazardNames.size(); i++) {
            result.append((i + 1)).append(". ").append(hazardNames.get(i)).append("\n");
        }
        
        result.append("\n🛡️ 控制措施：\n");
        for (String hazard : hazardNames) {
            List<String> measures = getMeasuresForHazard(hazard);
            for (int i = 0; i < measures.size(); i++) {
                result.append("- ").append(measures.get(i)).append("\n");
            }
        }
        
        return result.toString();
    }

    private List<String> identifyHazardsByRules(String voltageLevel, String equipmentType, 
                                                 String workType, String envConditions, String location) {
        List<String> hazards = new ArrayList<>();
        
        if (voltageLevel != null && (voltageLevel.contains("低压") || voltageLevel.contains("380V") || 
            voltageLevel.contains("220V") || voltageLevel.contains("0.4kV") || voltageLevel.contains("400V"))) {
            hazards.add("触电风险");
        }
        
        if (voltageLevel != null && (voltageLevel.contains("10kV") || voltageLevel.contains("35kV") || 
            voltageLevel.contains("110kV") || voltageLevel.contains("220kV"))) {
            hazards.add("触电风险");
        }
        
        if (workType != null && (workType.contains("高处") || workType.contains("杆塔") || 
            workType.contains("登杆") || workType.contains("攀登"))) {
            hazards.add("高处坠落风险");
        }
        
        if (equipmentType != null && (equipmentType.contains("开关") || equipmentType.contains("刀闸") || 
            equipmentType.contains("断路器") || equipmentType.contains("配电柜") || 
            equipmentType.contains("开关柜") || equipmentType.contains("控制柜"))) {
            hazards.add("机械伤害风险");
        }
        
        if (equipmentType != null && (equipmentType.contains("电缆") || equipmentType.contains("线路") || 
            equipmentType.contains("母线"))) {
            hazards.add("火灾风险");
        }
        
        if (envConditions != null && (envConditions.contains("潮湿") || envConditions.contains("雨天") || 
            envConditions.contains("大雾") || envConditions.contains("雷雨") || envConditions.contains("大风"))) {
            hazards.add("环境风险");
        }
        
        if (workType != null && workType.contains("带电")) {
            hazards.add("带电作业风险");
        }
        
        if (workType != null && (workType.contains("吊装") || workType.contains("起重") || workType.contains("搬运"))) {
            hazards.add("起重伤害风险");
        }
        
        if (workType != null && (workType.contains("焊接") || workType.contains("切割"))) {
            hazards.add("灼烫风险");
        }
        
        if (workType != null && (workType.contains("挖掘") || workType.contains("开挖"))) {
            hazards.add("坍塌风险");
        }
        
        if (location != null && (location.contains("电缆沟") || location.contains("地下室") || 
            location.contains("密闭空间") || location.contains("有限空间"))) {
            hazards.add("中毒窒息风险");
        }
        
        if (equipmentType != null && (equipmentType.contains("变压器") || equipmentType.contains("互感器"))) {
            hazards.add("爆炸风险");
        }
        
        if (hazards.isEmpty()) {
            hazards.add("触电风险");
            hazards.add("高处坠落风险");
            hazards.add("机械伤害风险");
        }
        
        return hazards.stream().distinct().collect(Collectors.toList());
    }

    private List<String> getMeasuresForHazard(String hazardName) {
        List<String> measures = new ArrayList<>();
        
        if (hazardName.contains("触电")) {
            measures.add("执行停电、验电、接地、挂牌、隔离五步法");
            measures.add("使用合格的绝缘工具");
            measures.add("穿戴绝缘鞋和绝缘手套");
            measures.add("设置安全围栏和警示标志");
            measures.add("工作前确认所有电源已断开");
            measures.add("对电容设备进行充分放电");
        }
        
        if (hazardName.contains("高处坠落")) {
            measures.add("正确佩戴安全带并系挂牢固");
            measures.add("使用合格的登高工具");
            measures.add("设置防坠装置和生命绳");
            measures.add("作业下方设置安全网");
            measures.add("专人监护，禁止单人作业");
            measures.add("严禁酒后或疲劳登高作业");
        }
        
        if (hazardName.contains("机械伤害")) {
            measures.add("设备停电后再进行操作");
            measures.add("佩戴防护手套和护目镜");
            measures.add("设置防护屏障");
            measures.add("使用专用工具，禁止违规操作");
            measures.add("设备转动部件加装防护罩");
            measures.add("作业前检查工具完好性");
        }
        
        if (hazardName.contains("火灾")) {
            measures.add("作业现场配备灭火器");
            measures.add("禁止在易燃区域使用明火");
            measures.add("电缆切割使用专用工具");
            measures.add("接线压接牢固，避免虚接发热");
            measures.add("定期检查电气设备温度");
            measures.add("作业后清理现场，消除隐患");
        }
        
        if (hazardName.contains("环境")) {
            measures.add("恶劣天气停止户外作业");
            measures.add("加强绝缘防护措施");
            measures.add("增加监护人员");
            measures.add("做好防滑防坠措施");
            measures.add("雷电天气禁止登杆作业");
            measures.add("大风天气停止高空作业");
        }
        
        if (hazardName.contains("带电")) {
            measures.add("使用绝缘工具");
            measures.add("保持安全距离");
            measures.add("由具备带电作业资格人员操作");
            measures.add("制定完善的安全措施方案");
            measures.add("全程专人监护");
            measures.add("配备绝缘遮蔽用具");
        }
        
        if (hazardName.contains("起重")) {
            measures.add("检查起重设备完好");
            measures.add("划定作业区域，禁止无关人员进入");
            measures.add("专人指挥，信号统一");
            measures.add("禁止超载吊装");
            measures.add("捆绑牢固，重心稳定");
            measures.add("吊装区域设置警示标志");
        }
        
        if (hazardName.contains("灼烫")) {
            measures.add("佩戴防护面罩和防护手套");
            measures.add("作业区域设置防火毯");
            measures.add("配备灭火器材");
            measures.add("禁止在易燃物附近焊接");
            measures.add("焊接后确认无火灾隐患");
            measures.add("高温工件放置在安全位置");
        }
        
        if (hazardName.contains("坍塌")) {
            measures.add("作业前检查边坡稳定性");
            measures.add("设置支护措施");
            measures.add("配备监测设备");
            measures.add("禁止在坑边堆放重物");
            measures.add("制定应急救援预案");
            measures.add("专人监测，发现异常立即撤离");
        }
        
        if (hazardName.contains("中毒")) {
            measures.add("进入前进行气体检测");
            measures.add("配备通风设备");
            measures.add("佩戴防毒面具");
            measures.add("外部专人监护");
            measures.add("制定应急预案");
            measures.add("设置紧急出口");
        }
        
        if (hazardName.contains("爆炸")) {
            measures.add("严格执行动火作业许可制度");
            measures.add("清理作业区域易燃易爆物品");
            measures.add("配备防爆工具");
            measures.add("监测可燃气体浓度");
            measures.add("设置防爆区域警示");
            measures.add("制定防爆应急预案");
        }
        
        return measures;
    }
}