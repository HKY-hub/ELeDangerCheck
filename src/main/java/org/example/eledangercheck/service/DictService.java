package org.example.eledangercheck.service;

import org.example.eledangercheck.entity.HazardDict;
import org.example.eledangercheck.entity.MeasureDict;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.HazardDictMapper;
import org.example.eledangercheck.mapper.MeasureDictMapper;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class DictService {

    private final HazardDictMapper hazardDictMapper;
    private final MeasureDictMapper measureDictMapper;

    public DictService(HazardDictMapper hazardDictMapper, MeasureDictMapper measureDictMapper) {
        this.hazardDictMapper = hazardDictMapper;
        this.measureDictMapper = measureDictMapper;
    }

    public HazardDict createHazardDict(HazardDict hazardDict) {
        hazardDictMapper.insert(hazardDict);
        return hazardDict;
    }

    public HazardDict getHazardDictById(Long id) {
        HazardDict hazardDict = hazardDictMapper.selectById(id);
        if (hazardDict == null) {
            throw new BusinessException("危险源字典不存在");
        }
        return hazardDict;
    }

    public HazardDict getHazardDictByCode(String code) {
        HazardDict hazardDict = hazardDictMapper.selectByDictCode(code);
        if (hazardDict == null) {
            throw new BusinessException("未找到该危险源编码");
        }
        return hazardDict;
    }

    public List<HazardDict> getAllHazardDicts() {
        return hazardDictMapper.selectList(null);
    }

    public List<HazardDict> getHazardDictsByLevel(String level) {
        return hazardDictMapper.selectByLevel(level);
    }

    public void deleteHazardDict(Long id) {
        if (hazardDictMapper.selectById(id) == null) {
            throw new BusinessException("危险源字典不存在");
        }
        hazardDictMapper.deleteById(id);
    }

    public HazardDict updateHazardDict(Long id, HazardDict hazardDict) {
        HazardDict existing = getHazardDictById(id);
        if (hazardDict.getDictCode() != null) {
            existing.setDictCode(hazardDict.getDictCode());
        }
        if (hazardDict.getDictName() != null) {
            existing.setDictName(hazardDict.getDictName());
        }
        if (hazardDict.getDictDesc() != null) {
            existing.setDictDesc(hazardDict.getDictDesc());
        }
        hazardDictMapper.updateById(existing);
        return existing;
    }

    public MeasureDict createMeasureDict(MeasureDict measureDict) {
        measureDictMapper.insert(measureDict);
        return measureDict;
    }

    public MeasureDict getMeasureDictById(Long id) {
        MeasureDict measureDict = measureDictMapper.selectById(id);
        if (measureDict == null) {
            throw new BusinessException("安全措施字典不存在");
        }
        return measureDict;
    }

    public List<MeasureDict> getMeasuresByHazardCode(String hazardCode) {
        return measureDictMapper.selectByHazardCode(hazardCode);
    }

    public List<MeasureDict> getAllMeasureDicts() {
        return measureDictMapper.selectList(null);
    }

    public void deleteMeasureDict(Long id) {
        if (measureDictMapper.selectById(id) == null) {
            throw new BusinessException("安全措施字典不存在");
        }
        measureDictMapper.deleteById(id);
    }

    public MeasureDict updateMeasureDict(Long id, MeasureDict measureDict) {
        MeasureDict existing = getMeasureDictById(id);
        if (measureDict.getDictCode() != null) {
            existing.setDictCode(measureDict.getDictCode());
        }
        if (measureDict.getDictName() != null) {
            existing.setDictName(measureDict.getDictName());
        }
        if (measureDict.getDictDesc() != null) {
            existing.setDictDesc(measureDict.getDictDesc());
        }
        if (measureDict.getHazardCode() != null) {
            existing.setHazardCode(measureDict.getHazardCode());
        }
        if (measureDict.getPriority() != null) {
            existing.setPriority(measureDict.getPriority());
        }
        measureDictMapper.updateById(existing);
        return existing;
    }
}