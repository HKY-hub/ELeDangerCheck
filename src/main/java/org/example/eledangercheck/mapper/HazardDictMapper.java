package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.HazardDict;

import java.util.List;

public interface HazardDictMapper extends BaseMapper<HazardDict> {

    @Select("SELECT * FROM hazard_dict WHERE dict_code = #{dictCode}")
    HazardDict selectByDictCode(String dictCode);

    @Select("SELECT * FROM hazard_dict WHERE level = #{level}")
    List<HazardDict> selectByLevel(String level);
}