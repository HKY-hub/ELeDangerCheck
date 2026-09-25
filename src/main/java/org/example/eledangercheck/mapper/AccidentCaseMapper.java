package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.AccidentCase;

import java.util.List;

public interface AccidentCaseMapper extends BaseMapper<AccidentCase> {

    @Select("SELECT * FROM accident_case WHERE accident_type = #{accidentType}")
    List<AccidentCase> selectByAccidentType(String accidentType);

    @Select("SELECT * FROM accident_case WHERE work_type = #{workType}")
    List<AccidentCase> selectByWorkType(String workType);
}