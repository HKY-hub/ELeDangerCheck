package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.Hazard;

import java.util.List;

public interface HazardMapper extends BaseMapper<Hazard> {

    @Select("SELECT * FROM hazard WHERE task_id = #{taskId}")
    List<Hazard> selectByTaskId(Long taskId);

    @Select("SELECT * FROM hazard WHERE hazard_level = #{level}")
    List<Hazard> selectByHazardLevel(String level);
}