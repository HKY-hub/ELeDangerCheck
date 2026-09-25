package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.example.eledangercheck.entity.DisclosureSign;

import java.util.List;

public interface DisclosureSignMapper extends BaseMapper<DisclosureSign> {

    List<DisclosureSign> selectByDisclosureId(Long disclosureId);

    List<DisclosureSign> selectByUserId(Long userId);
}