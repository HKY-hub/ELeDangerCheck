package org.example.eledangercheck.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.eledangercheck.entity.WeatherCity;

import java.util.List;

public interface WeatherCityMapper extends BaseMapper<WeatherCity> {

    @Select("SELECT * FROM weather_city WHERE city_name LIKE CONCAT('%', #{keyword}, '%') LIMIT 20")
    List<WeatherCity> searchByKeyword(String keyword);

    @Select("SELECT * FROM weather_city WHERE city_code = #{cityCode}")
    WeatherCity selectByCityCode(String cityCode);
}
