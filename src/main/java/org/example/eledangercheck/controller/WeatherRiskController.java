package org.example.eledangercheck.controller;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.entity.WeatherCity;
import org.example.eledangercheck.service.WeatherRiskService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/weather")
public class WeatherRiskController {

    private static final Logger logger = LoggerFactory.getLogger(WeatherRiskController.class);

    private final WeatherRiskService weatherRiskService;

    public WeatherRiskController(WeatherRiskService weatherRiskService) {
        this.weatherRiskService = weatherRiskService;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchCities(@RequestParam String keyword) {
        logger.info("Search cities request with keyword: {}", keyword);
        List<WeatherCity> cities = weatherRiskService.searchCities(keyword);
        return ResponseEntity.ok(cities);
    }

    @GetMapping("/city/{cityCode}")
    public ResponseEntity<?> getWeather(@PathVariable String cityCode) {
        logger.info("Get weather request for cityCode: {}", cityCode);
        Map<String, Object> weatherData = weatherRiskService.getWeatherByCityCode(cityCode);
        return ResponseEntity.ok(weatherData);
    }

    @GetMapping("/report/{cityCode}")
    public ResponseEntity<?> getFullReport(@PathVariable String cityCode) {
        logger.info("Get full weather report request for cityCode: {}", cityCode);
        Map<String, Object> result = new HashMap<>();

        WeatherCity city = weatherRiskService.getCityByCode(cityCode);
        Map<String, Object> cityInfo = new HashMap<>();
        cityInfo.put("cityCode", city.getCityCode());
        cityInfo.put("cityName", city.getCityName());
        cityInfo.put("province", city.getProvince());
        result.put("cityInfo", cityInfo);

        Map<String, Object> weatherData = weatherRiskService.getWeatherByCityCode(cityCode);
        JSONObject data = (JSONObject) weatherData.get("data");
        JSONObject cityInfoApi = (JSONObject) weatherData.get("cityInfo");

        Map<String, Object> current = new HashMap<>();
        if (data != null) {
            current.put("temp", data.getString("wendu"));
            current.put("weatherText", "");
            current.put("shidu", data.getString("shidu"));
            current.put("quality", data.getString("quality"));
            current.put("pm25", data.get("pm25"));
            current.put("pm10", data.get("pm10"));
            current.put("ganmao", data.getString("ganmao"));
        }
        if (cityInfoApi != null) {
            current.put("updateTime", cityInfoApi.getString("updateTime"));
        }

        List<Map<String, Object>> forecastList = new java.util.ArrayList<>();
        if (data != null) {
            JSONArray forecastArray = data.getJSONArray("forecast");
            if (forecastArray != null) {
                for (int i = 0; i < Math.min(forecastArray.size(), 7); i++) {
                    JSONObject day = forecastArray.getJSONObject(i);
                    Map<String, Object> dayMap = new HashMap<>();
                    dayMap.put("date", day.getString("date"));
                    dayMap.put("ymd", day.getString("ymd"));
                    dayMap.put("week", day.getString("week"));
                    dayMap.put("dayWeather", day.getString("type"));
                    dayMap.put("nightWeather", day.getString("type"));
                    dayMap.put("tempMax", day.getString("high").replace("高温 ", "").replace("℃", ""));
                    dayMap.put("tempMin", day.getString("low").replace("低温 ", "").replace("℃", ""));
                    dayMap.put("windDirDay", day.getString("fx"));
                    dayMap.put("windScaleDay", day.getString("fl").replace("级", ""));
                    dayMap.put("aqi", day.get("aqi"));
                    dayMap.put("notice", day.getString("notice"));
                    dayMap.put("sunrise", day.getString("sunrise"));
                    dayMap.put("sunset", day.getString("sunset"));
                    forecastList.add(dayMap);
                }
                if (!forecastList.isEmpty()) {
                    current.put("weatherText", forecastList.get(0).get("dayWeather"));
                    current.put("windDir", forecastList.get(0).get("windDirDay"));
                    current.put("windScale", forecastList.get(0).get("windScaleDay"));
                    current.put("sunrise", forecastList.get(0).get("sunrise"));
                    current.put("sunset", forecastList.get(0).get("sunset"));
                    current.put("notice", forecastList.get(0).get("notice"));
                    current.put("aqi", forecastList.get(0).get("aqi"));
                }
            }

            JSONObject yesterday = data.getJSONObject("yesterday");
            if (yesterday != null) {
                Map<String, Object> yesterdayMap = new HashMap<>();
                yesterdayMap.put("date", yesterday.getString("date"));
                yesterdayMap.put("ymd", yesterday.getString("ymd"));
                yesterdayMap.put("week", yesterday.getString("week"));
                yesterdayMap.put("dayWeather", yesterday.getString("type"));
                yesterdayMap.put("tempMax", yesterday.getString("high").replace("高温 ", "").replace("℃", ""));
                yesterdayMap.put("tempMin", yesterday.getString("low").replace("低温 ", "").replace("℃", ""));
                yesterdayMap.put("windDirDay", yesterday.getString("fx"));
                yesterdayMap.put("windScaleDay", yesterday.getString("fl").replace("级", ""));
                yesterdayMap.put("aqi", yesterday.get("aqi"));
                yesterdayMap.put("sunrise", yesterday.getString("sunrise"));
                yesterdayMap.put("sunset", yesterday.getString("sunset"));
                result.put("yesterday", yesterdayMap);
            }
        }
        result.put("current", current);
        result.put("forecast", forecastList);

        List<Map<String, Object>> risks = weatherRiskService.calculateDynamicRisks(weatherData);
        result.put("risks", risks);

        List<Map<String, Object>> hourly = weatherRiskService.generateHourlyForecast(weatherData);
        result.put("hourly", hourly);

        Map<String, Object> alert = weatherRiskService.generateWeatherAlert(weatherData);
        result.put("alert", alert);

        result.put("date", weatherData.get("date"));
        result.put("time", weatherData.get("time"));

        return ResponseEntity.ok(result);
    }
}
