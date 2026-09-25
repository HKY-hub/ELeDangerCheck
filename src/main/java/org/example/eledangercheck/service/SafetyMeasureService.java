package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.SafetyMeasure;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.SafetyMeasureMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SafetyMeasureService {

    private final SafetyMeasureMapper safetyMeasureMapper;

    public SafetyMeasureService(SafetyMeasureMapper safetyMeasureMapper) {
        this.safetyMeasureMapper = safetyMeasureMapper;
    }

    public SafetyMeasure createSafetyMeasure(SafetyMeasure measure) {
        safetyMeasureMapper.insert(measure);
        return measure;
    }

    public SafetyMeasure getSafetyMeasureById(Long id) {
        return safetyMeasureMapper.selectById(id);
    }

    public List<SafetyMeasure> getMeasuresByHazardId(Long hazardId) {
        return safetyMeasureMapper.selectByHazardId(hazardId);
    }

    public List<SafetyMeasure> getMeasuresByTaskId(Long taskId) {
        return safetyMeasureMapper.selectByTaskId(taskId);
    }

    public List<SafetyMeasure> getAllMeasures() {
        return safetyMeasureMapper.selectList(null);
    }

    public SafetyMeasure updateSafetyMeasure(Long id, SafetyMeasure measureDetails) {
        SafetyMeasure measure = getSafetyMeasureById(id);
        if (measure == null) {
            throw new BusinessException("安全措施不存在");
        }
        if (measureDetails.getMeasureName() != null) {
            measure.setMeasureName(measureDetails.getMeasureName());
        }
        if (measureDetails.getMeasureDesc() != null) {
            measure.setMeasureDesc(measureDetails.getMeasureDesc());
        }
        if (measureDetails.getPriority() != null) {
            measure.setPriority(measureDetails.getPriority());
        }
        if (measureDetails.getStatus() != null) {
            measure.setStatus(measureDetails.getStatus());
        }
        safetyMeasureMapper.updateById(measure);
        return measure;
    }

    public void deleteSafetyMeasure(Long id) {
        if (safetyMeasureMapper.selectById(id) == null) {
            throw new BusinessException("安全措施不存在");
        }
        safetyMeasureMapper.deleteById(id);
    }
}