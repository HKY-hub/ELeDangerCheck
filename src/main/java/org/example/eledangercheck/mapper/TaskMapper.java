package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.Task;

import java.util.List;

public interface TaskMapper extends BaseMapper<Task> {

    @Select("SELECT * FROM task WHERE principal_id = #{principalId}")
    List<Task> selectByPrincipalId(Long principalId);

    @Select("SELECT * FROM task WHERE status = #{status}")
    List<Task> selectByStatus(String status);
}