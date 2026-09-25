package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.eledangercheck.entity.Evaluation;

import java.util.List;

public interface EvaluationMapper extends BaseMapper<Evaluation> {

    List<Evaluation> selectByTaskId(Long taskId);

    List<Evaluation> selectByDisclosureId(Long disclosureId);
}