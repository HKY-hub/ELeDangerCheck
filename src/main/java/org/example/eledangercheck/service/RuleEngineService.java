package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.entity.TaskParse;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RuleEngineService {

    public List<Hazard> identifyHazards(TaskParse taskParse) {
        List<Hazard> hazards = new ArrayList<>();

        String voltageLevel = taskParse.getVoltageLevel();
        String equipmentType = taskParse.getEquipmentType();
        String workType = taskParse.getWorkType();
        String envConditions = taskParse.getEnvConditions();
        String location = taskParse.getLocation();

        if (voltageLevel != null && !voltageLevel.isEmpty()) {
            if (voltageLevel.contains("10kV") || voltageLevel.contains("35kV") || 
                voltageLevel.contains("110kV") || voltageLevel.contains("220kV")) {
                hazards.add(createHazard("高压触电风险", "触电风险", "high", 
                    voltageLevel + "高压设备作业，存在触电事故风险", location));
            } else if (voltageLevel.contains("380V") || voltageLevel.contains("220V")) {
                hazards.add(createHazard("低压触电风险", "触电风险", "medium", 
                    "低压设备作业，存在触电事故风险", location));
            }
        }

        if (workType != null && workType.contains("带电")) {
            hazards.add(createHazard("带电作业触电风险", "触电风险", "high", 
                "带电作业存在直接触电风险，必须严格执行安全规程", location));
        }

        if (equipmentType != null) {
            if (equipmentType.contains("杆塔") || equipmentType.contains("铁塔")) {
                hazards.add(createHazard("高处坠落风险", "高处坠落", "high", 
                    "杆塔作业属于高空作业，存在高处坠落风险", location));
            }
            if (equipmentType.contains("梯子")) {
                hazards.add(createHazard("梯子作业坠落风险", "高处坠落", "medium", 
                    "梯子作业存在滑倒、坠落风险", location));
            }
            if (equipmentType.contains("风机") || equipmentType.contains("电机") || equipmentType.contains("泵")) {
                hazards.add(createHazard("转动设备伤害风险", "机械伤害", "high", 
                    "转动设备作业存在绞伤、卷入风险", location));
            }
        }

        if (workType != null) {
            if (workType.contains("起重") || workType.contains("吊装")) {
                hazards.add(createHazard("起重伤害风险", "机械伤害", "high", 
                    "起重作业存在重物坠落、挤压伤害风险", location));
            }
            if (workType.contains("切割") || workType.contains("打磨") || workType.contains("钻孔")) {
                hazards.add(createHazard("工具伤害风险", "机械伤害", "medium", 
                    "使用切割、打磨工具存在飞溅、割伤风险", location));
            }
            if (workType.contains("屋顶") || workType.contains("屋面")) {
                hazards.add(createHazard("高处坠落风险", "高处坠落", "high", 
                    "屋顶作业存在高处坠落风险", location));
            }
        }

        if (envConditions != null) {
            if (envConditions.contains("雷雨") || envConditions.contains("暴雨")) {
                hazards.add(createHazard("雷雨天气风险", "环境风险", "high", 
                    "雷雨天气作业存在雷击风险", location));
            }
            if (envConditions.contains("大风")) {
                hazards.add(createHazard("大风天气风险", "环境风险", "high", 
                    "大风天气高空作业存在坠落风险", location));
            }
            if (envConditions.contains("高温")) {
                hazards.add(createHazard("高温作业风险", "环境风险", "medium", 
                    "高温环境作业存在中暑风险", location));
            }
            if (envConditions.contains("夜间")) {
                hazards.add(createHazard("夜间作业风险", "环境风险", "medium", 
                    "夜间作业存在照明不足、视线不清风险", location));
            }
        }

        if (hazards.isEmpty()) {
            hazards.add(createHazard("其他风险", "其他风险", "low", 
                "未识别到具体危险点，请谨慎作业", location));
        }

        return hazards;
    }

    private Hazard createHazard(String hazardName, String category, String hazardLevel, 
                                String description, String location) {
        Hazard hazard = new Hazard();
        hazard.setHazardName(hazardName);
        hazard.setCategory(category);
        hazard.setHazardLevel(hazardLevel);
        hazard.setDescription(description);
        hazard.setLocation(location);
        hazard.setSimilarity(0.85);
        return hazard;
    }
}