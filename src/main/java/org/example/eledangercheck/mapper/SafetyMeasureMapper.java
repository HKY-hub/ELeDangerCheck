package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.SafetyMeasure;

import java.util.List;

public interface SafetyMeasureMapper extends BaseMapper<SafetyMeasure> {

    @Select("SELECT * FROM safety_measure WHERE hazard_id = #{hazardId} ORDER BY priority")
    List<SafetyMeasure> selectByHazardId(Long hazardId);

    @Select("SELECT sm.* FROM safety_measure sm JOIN hazard h ON sm.hazard_id = h.id WHERE h.task_id = #{taskId} ORDER BY sm.priority")
    List<SafetyMeasure> selectByTaskId(Long taskId);
}