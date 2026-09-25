package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.MeasureDict;

import java.util.List;

public interface MeasureDictMapper extends BaseMapper<MeasureDict> {

    @Select("SELECT * FROM measure_dict WHERE dict_code = #{dictCode}")
    MeasureDict selectByDictCode(String dictCode);

    @Select("SELECT * FROM measure_dict WHERE hazard_code = #{hazardCode} ORDER BY priority")
    List<MeasureDict> selectByHazardCode(String hazardCode);
}