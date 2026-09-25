package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.AccidentCase;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.AccidentCaseMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccidentCaseService {

    private final AccidentCaseMapper accidentCaseMapper;

    public AccidentCaseService(AccidentCaseMapper accidentCaseMapper) {
        this.accidentCaseMapper = accidentCaseMapper;
    }

    public AccidentCase createAccidentCase(AccidentCase accidentCase) {
        accidentCaseMapper.insert(accidentCase);
        return accidentCase;
    }

    public AccidentCase getAccidentCaseById(Long id) {
        AccidentCase accidentCase = accidentCaseMapper.selectById(id);
        if (accidentCase == null) {
            throw new BusinessException("事故案例不存在");
        }
        return accidentCase;
    }

    public List<AccidentCase> getAllAccidentCases() {
        return accidentCaseMapper.selectList(null);
    }

    public List<AccidentCase> getCasesByType(String accidentType) {
        return accidentCaseMapper.selectByAccidentType(accidentType);
    }

    public List<AccidentCase> getCasesByWorkType(String workType) {
        return accidentCaseMapper.selectByWorkType(workType);
    }

    public AccidentCase updateAccidentCase(Long id, AccidentCase caseDetails) {
        AccidentCase accidentCase = getAccidentCaseById(id);
        if (caseDetails.getCaseName() != null) {
            accidentCase.setCaseName(caseDetails.getCaseName());
        }
        if (caseDetails.getAccidentType() != null) {
            accidentCase.setAccidentType(caseDetails.getAccidentType());
        }
        if (caseDetails.getVoltageLevel() != null) {
            accidentCase.setVoltageLevel(caseDetails.getVoltageLevel());
        }
        if (caseDetails.getWorkType() != null) {
            accidentCase.setWorkType(caseDetails.getWorkType());
        }
        if (caseDetails.getEquipmentType() != null) {
            accidentCase.setEquipmentType(caseDetails.getEquipmentType());
        }
        if (caseDetails.getCaseDesc() != null) {
            accidentCase.setCaseDesc(caseDetails.getCaseDesc());
        }
        if (caseDetails.getLessonsLearned() != null) {
            accidentCase.setLessonsLearned(caseDetails.getLessonsLearned());
        }
        if (caseDetails.getHazardPoints() != null) {
            accidentCase.setHazardPoints(caseDetails.getHazardPoints());
        }
        accidentCaseMapper.updateById(accidentCase);
        return accidentCase;
    }

    public void deleteAccidentCase(Long id) {
        if (accidentCaseMapper.selectById(id) == null) {
            throw new BusinessException("事故案例不存在");
        }
        accidentCaseMapper.deleteById(id);
    }
}