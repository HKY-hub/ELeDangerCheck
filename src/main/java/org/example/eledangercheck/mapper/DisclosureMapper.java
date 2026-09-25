package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.Disclosure;

import java.util.List;

public interface DisclosureMapper extends BaseMapper<Disclosure> {

    @Select("SELECT * FROM disclosure WHERE task_id = #{taskId}")
    Disclosure selectByTaskId(Long taskId);

    @Select("SELECT * FROM disclosure WHERE disclosure_status = #{status}")
    List<Disclosure> selectByDisclosureStatus(String status);
}