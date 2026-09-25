package org.example.eledangercheck.controller;

import org.example.eledangercheck.entity.*;
import org.example.eledangercheck.service.AgentService;
import org.example.eledangercheck.service.AiAgentService;
import org.example.eledangercheck.service.UnifiedAiService;
import org.example.eledangercheck.service.AgentWeatherService;
import org.example.eledangercheck.service.DisclosureService;
import org.example.eledangercheck.service.HazardService;
import org.example.eledangercheck.service.SafetyMeasureService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/agent")
public class AgentController {

    private final AgentService agentService;
    private final AiAgentService aiAgentService;
    private final UnifiedAiService unifiedAiService;
    private final AgentWeatherService agentWeatherService;
    private final HazardService hazardService;
    private final SafetyMeasureService safetyMeasureService;
    private final DisclosureService disclosureService;

    public AgentController(AgentService agentService, AiAgentService aiAgentService,
                          UnifiedAiService unifiedAiService,
                          AgentWeatherService agentWeatherService,
                          HazardService hazardService, SafetyMeasureService safetyMeasureService,
                          DisclosureService disclosureService) {
        this.agentService = agentService;
        this.aiAgentService = aiAgentService;
        this.unifiedAiService = unifiedAiService;
        this.agentWeatherService = agentWeatherService;
        this.hazardService = hazardService;
        this.safetyMeasureService = safetyMeasureService;
        this.disclosureService = disclosureService;
    }

    @PostMapping("/parse")
    public ResponseEntity<Map<String, Object>> parseTask(@RequestBody Map<String, String> request) {
        String taskText = request.get("taskText");
        Map<String, Object> result = agentService.parseTask(taskText);
        return ResponseEntity.ok(result);
    }

    @PostMapping("/create-task")
    public ResponseEntity<Task> createTaskWithParse(@RequestBody Map<String, String> request) {
        String taskName = request.get("taskName");
        String taskDesc = request.get("taskDesc");
        String taskText = request.get("taskText");
        Task task = agentService.createTaskWithParse(taskName, taskDesc, taskText);
        return ResponseEntity.ok(task);
    }

    @PostMapping("/task/{taskId}/identify-hazards")
    public ResponseEntity<List<Hazard>> identifyHazards(@PathVariable Long taskId) {
        List<Hazard> hazards = agentService.identifyHazards(taskId);
        return ResponseEntity.ok(hazards);
    }

    @PostMapping("/hazard/{hazardId}/generate-measures")
    public ResponseEntity<List<SafetyMeasure>> generateMeasures(@PathVariable Long hazardId) {
        List<SafetyMeasure> measures = agentService.generateMeasures(hazardId);
        return ResponseEntity.ok(measures);
    }

    @PostMapping("/task/{taskId}/generate-disclosure")
    public ResponseEntity<Disclosure> generateDisclosureCard(@PathVariable Long taskId) {
        Disclosure disclosure = agentService.generateDisclosureCard(taskId);
        return ResponseEntity.ok(disclosure);
    }

    @PostMapping("/complete-flow")
    public ResponseEntity<Map<String, Object>> completeAgentFlow(@RequestBody Map<String, String> request) {
        String taskName = request.get("taskName");
        String taskDesc = request.get("taskDesc");
        String taskText = request.get("taskText");
        Map<String, Object> result = agentService.completeAgentFlow(taskName, taskDesc, taskText);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/task/{taskId}/hazards")
    public ResponseEntity<List<Hazard>> getHazardsByTask(@PathVariable Long taskId) {
        List<Hazard> hazards = hazardService.getHazardsByTaskId(taskId);
        return ResponseEntity.ok(hazards);
    }

    @GetMapping("/hazard/{hazardId}/measures")
    public ResponseEntity<List<SafetyMeasure>> getMeasuresByHazard(@PathVariable Long hazardId) {
        List<SafetyMeasure> measures = safetyMeasureService.getMeasuresByHazardId(hazardId);
        return ResponseEntity.ok(measures);
    }

    @GetMapping("/task/{taskId}/disclosure")
    public ResponseEntity<Disclosure> getDisclosureByTask(@PathVariable Long taskId) {
        try {
            Disclosure disclosure = disclosureService.getDisclosureByTaskId(taskId);
            return ResponseEntity.ok(disclosure);
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }

    // ========== 新的统一AI对话接口 ==========

    /**
     * 统一AI对话接口
     * 基于DeepSeek API实现
     * 输出格式统一，知识库约束，确定性回答
     */
    @PostMapping("/chat")
    public ResponseEntity<Map<String, Object>> chat(@RequestBody Map<String, String> request) {
        String message = request.get("message");
        String conversationId = request.get("conversationId");
        Map<String, Object> result = unifiedAiService.chat(message, conversationId);
        return ResponseEntity.ok(result);
    }

    /**
     * 获取当前模型信息
     */
    @GetMapping("/model")
    public ResponseEntity<Map<String, Object>> getModel() {
        Map<String, Object> result = Map.of(
            "mode", "api",
            "modes", Map.of(
                "api", "DeepSeek API"
            ),
            "model", "deepseek-v4-pro"
        );
        return ResponseEntity.ok(result);
    }

    @PostMapping("/ai-chat")
    public ResponseEntity<Map<String, Object>> aiChat(@RequestBody Map<String, String> request) {
        String taskText = request.get("taskText");
        String aiResponse = aiAgentService.completeFlow(taskText);
        
        Map<String, Object> result = agentService.completeAgentFlow(taskText, taskText, taskText);
        result.put("aiAnalysis", aiResponse);
        
        return ResponseEntity.ok(result);
    }

    // ========== 天气校验接口（AGENT联动） ==========

    /**
     * 搜索城市（用于选择作业地点）
     */
    @GetMapping("/cities/search")
    public ResponseEntity<List<WeatherCity>> searchCities(@RequestParam String keyword) {
        List<WeatherCity> cities = agentWeatherService.searchCities(keyword);
        return ResponseEntity.ok(cities);
    }

    /**
     * 检查作业天气是否适合
     * 恶劣天气时返回isSafe=false和拒绝原因
     */
    @PostMapping("/weather-check")
    public ResponseEntity<Map<String, Object>> checkWorkWeather(@RequestBody Map<String, String> request) {
        String cityCode = request.get("cityCode");
        String cityName = request.get("cityName");
        String workDate = request.get("workDate");
        Map<String, Object> result = agentWeatherService.checkWorkWeather(cityCode, cityName, workDate);
        return ResponseEntity.ok(result);
    }

    /**
     * 带天气校验的完整交底卡生成流程
     * 恶劣天气时直接拒绝生成
     */
    @PostMapping("/generate-with-weather")
    public ResponseEntity<Map<String, Object>> generateWithWeatherCheck(@RequestBody Map<String, String> request) {
        String cityCode = request.get("cityCode");
        String cityName = request.get("cityName");
        String workDate = request.get("workDate");
        String taskName = request.get("taskName");
        String taskDesc = request.get("taskDesc");
        String taskText = request.get("taskText");

        // 第一步：天气校验
        Map<String, Object> weatherCheck = agentWeatherService.checkWorkWeather(cityCode, cityName, workDate);
        Boolean isSafe = (Boolean) weatherCheck.get("isSafe");

        if (isSafe != null && !isSafe) {
            // 天气不安全，拒绝生成
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("weatherCheck", weatherCheck);
            result.put("rejected", true);
            result.put("rejectReason", weatherCheck.get("message"));
            result.put("disclosure", null);
            return ResponseEntity.ok(result);
        }

        // 第二步：天气安全，生成交底卡
        Map<String, Object> flowResult = agentService.completeAgentFlow(taskName, taskDesc, taskText);
        flowResult.put("weatherCheck", weatherCheck);
        flowResult.put("rejected", false);
        return ResponseEntity.ok(flowResult);
    }
}