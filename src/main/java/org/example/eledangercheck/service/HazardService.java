package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.Hazard;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.HazardMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class HazardService {

    private final HazardMapper hazardMapper;

    public HazardService(HazardMapper hazardMapper) {
        this.hazardMapper = hazardMapper;
    }

    public Hazard createHazard(Hazard hazard) {
        hazard.setCreateTime(LocalDateTime.now());
        hazardMapper.insert(hazard);
        return hazard;
    }

    public Hazard getHazardById(Long id) {
        Hazard hazard = hazardMapper.selectById(id);
        if (hazard == null) {
            throw new BusinessException("危险源不存在");
        }
        return hazard;
    }

    public List<Hazard> getHazardsByTaskId(Long taskId) {
        return hazardMapper.selectByTaskId(taskId);
    }

    public List<Hazard> getHazardsByLevel(String level) {
        return hazardMapper.selectByHazardLevel(level);
    }

    public List<Hazard> getAllHazards() {
        return hazardMapper.selectList(null);
    }

    public Hazard updateHazard(Long id, Hazard hazardDetails) {
        Hazard hazard = getHazardById(id);
        if (hazardDetails.getHazardName() != null) {
            hazard.setHazardName(hazardDetails.getHazardName());
        }
        if (hazardDetails.getCategory() != null) {
            hazard.setCategory(hazardDetails.getCategory());
        }
        if (hazardDetails.getHazardLevel() != null) {
            hazard.setHazardLevel(hazardDetails.getHazardLevel());
        }
        if (hazardDetails.getLocation() != null) {
            hazard.setLocation(hazardDetails.getLocation());
        }
        if (hazardDetails.getDescription() != null) {
            hazard.setDescription(hazardDetails.getDescription());
        }
        if (hazardDetails.getSourceCase() != null) {
            hazard.setSourceCase(hazardDetails.getSourceCase());
        }
        if (hazardDetails.getSimilarity() != null) {
            hazard.setSimilarity(hazardDetails.getSimilarity());
        }
        hazardMapper.updateById(hazard);
        return hazard;
    }

    public void deleteHazard(Long id) {
        if (hazardMapper.selectById(id) == null) {
            throw new BusinessException("危险源不存在");
        }
        hazardMapper.deleteById(id);
    }
}