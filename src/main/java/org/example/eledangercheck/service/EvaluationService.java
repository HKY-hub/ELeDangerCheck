package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.Evaluation;
import org.example.eledangercheck.entity.Disclosure;
import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.EvaluationMapper;
import org.example.eledangercheck.mapper.DisclosureMapper;
import org.example.eledangercheck.mapper.HazardMapper;
import org.example.eledangercheck.mapper.SafetyMeasureMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class EvaluationService {

    private final EvaluationMapper evaluationMapper;
    private final DisclosureMapper disclosureMapper;
    private final HazardMapper hazardMapper;
    private final SafetyMeasureMapper safetyMeasureMapper;

    public EvaluationService(EvaluationMapper evaluationMapper, DisclosureMapper disclosureMapper,
                             HazardMapper hazardMapper, SafetyMeasureMapper safetyMeasureMapper) {
        this.evaluationMapper = evaluationMapper;
        this.disclosureMapper = disclosureMapper;
        this.hazardMapper = hazardMapper;
        this.safetyMeasureMapper = safetyMeasureMapper;
    }

    public Evaluation evaluate(Long disclosureId) {
        Disclosure disclosure = disclosureMapper.selectById(disclosureId);
        if (disclosure == null) {
            throw new BusinessException("交底记录不存在");
        }

        Long taskId = disclosure.getTaskId();
        List<Hazard> hazards = hazardMapper.selectByTaskId(taskId);
        List<SafetyMeasure> measures = new java.util.ArrayList<>();
        for (Hazard hazard : hazards) {
            measures.addAll(safetyMeasureMapper.selectByHazardId(hazard.getId()));
        }

        int score = calculateScore(hazards, measures, disclosure);
        int violationCount = countViolations(hazards, measures);
        String evaluationDesc = generateEvaluationDesc(score, hazards.size(), measures.size(), violationCount);

        Evaluation evaluation = new Evaluation();
        evaluation.setDisclosureId(disclosureId);
        evaluation.setScore(score);
        evaluation.setViolationCount(violationCount);
        evaluation.setEvaluationDesc(evaluationDesc);
        evaluation.setCreateTime(LocalDateTime.now());
        evaluationMapper.insert(evaluation);

        return evaluation;
    }

    private int calculateScore(List<Hazard> hazards, List<SafetyMeasure> measures, Disclosure disclosure) {
        int score = 100;

        if (hazards.isEmpty()) {
            score -= 30;
        }

        long highRiskCount = hazards.stream().filter(h -> "high".equals(h.getHazardLevel())).count();
        long mediumRiskCount = hazards.stream().filter(h -> "medium".equals(h.getHazardLevel())).count();

        if (highRiskCount > 0 && measures.size() < highRiskCount * 3) {
            score -= 20;
        }

        if (mediumRiskCount > 0 && measures.size() < mediumRiskCount * 2) {
            score -= 10;
        }

        if (disclosure.getEmergencyContact() == null || disclosure.getEmergencyContact().contains("待")) {
            score -= 10;
        }

        if (disclosure.getEmergencyRoute() == null || disclosure.getEmergencyRoute().contains("待")) {
            score -= 10;
        }

        return Math.max(0, score);
    }

    private int countViolations(List<Hazard> hazards, List<SafetyMeasure> measures) {
        int violations = 0;

        if (hazards.isEmpty()) {
            violations++;
        }

        long highRiskCount = hazards.stream().filter(h -> "high".equals(h.getHazardLevel())).count();
        if (highRiskCount > 0 && measures.isEmpty()) {
            violations++;
        }

        if (highRiskCount > 0 && measures.size() < highRiskCount) {
            violations++;
        }

        return violations;
    }

    private String generateEvaluationDesc(int score, int hazardCount, int measureCount, int violationCount) {
        StringBuilder desc = new StringBuilder();

        if (score >= 90) {
            desc.append("【优秀】交底质量评估优秀");
        } else if (score >= 70) {
            desc.append("【良好】交底质量评估良好");
        } else if (score >= 60) {
            desc.append("【合格】交底质量评估合格");
        } else {
            desc.append("【不合格】交底质量评估不合格，需要整改");
        }

        desc.append("\n\n评估详情：");
        desc.append("\n- 危险点识别数量：").append(hazardCount);
        desc.append("\n- 控制措施数量：").append(measureCount);
        desc.append("\n- 违规项数量：").append(violationCount);

        if (violationCount > 0) {
            desc.append("\n\n整改建议：");
            if (hazardCount == 0) {
                desc.append("\n  1. 需要重新识别危险点");
            }
            if (measureCount == 0) {
                desc.append("\n  2. 需要为每个危险点制定控制措施");
            }
            if (measureCount > 0 && hazardCount > 0) {
                desc.append("\n  3. 建议增加控制措施数量");
            }
        }

        return desc.toString();
    }

    public Evaluation getEvaluationByDisclosureId(Long disclosureId) {
        List<Evaluation> evaluations = evaluationMapper.selectByDisclosureId(disclosureId);
        return evaluations.isEmpty() ? null : evaluations.get(0);
    }

    public List<Evaluation> getAllEvaluations() {
        return evaluationMapper.selectList(null);
    }
}