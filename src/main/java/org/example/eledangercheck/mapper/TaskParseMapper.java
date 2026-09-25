package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.eledangercheck.entity.TaskParse;

public interface TaskParseMapper extends BaseMapper<TaskParse> {

    TaskParse selectByTaskId(Long taskId);
}