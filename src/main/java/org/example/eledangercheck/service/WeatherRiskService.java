package org.example.eledangercheck.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.entity.WeatherCity;
import org.example.eledangercheck.exception.BusinessException;
import org.example.eledangercheck.mapper.WeatherCityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class WeatherRiskService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherRiskService.class);

    private static final String WEATHER_API_URL = "http://t.weather.itboy.net/api/weather/city/{cityCode}";

    private final RestTemplate restTemplate;
    private final WeatherCityMapper weatherCityMapper;

    public WeatherRiskService(RestTemplate restTemplate, WeatherCityMapper weatherCityMapper) {
        this.restTemplate = restTemplate;
        this.weatherCityMapper = weatherCityMapper;
    }

    public Map<String, Object> getWeatherByCityCode(String cityCode) {
        try {
            logger.info("Fetching weather data for cityCode: {}", cityCode);
            ResponseEntity<String> response = restTemplate.getForEntity(WEATHER_API_URL, String.class, cityCode);
            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                JSONObject json = JSON.parseObject(response.getBody());
                if (json.getIntValue("status") == 200) {
                    return json.getInnerMap();
                } else {
                    logger.warn("Weather API returned non-200 status: {}", json.getIntValue("status"));
                    throw new BusinessException("天气数据获取失败，状态码: " + json.getIntValue("status"));
                }
            } else {
                logger.error("Weather API request failed with status: {}", response.getStatusCode());
                throw new BusinessException("天气API请求失败");
            }
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error fetching weather data for cityCode {}: {}", cityCode, e.getMessage(), e);
            throw new BusinessException("获取天气数据异常: " + e.getMessage());
        }
    }

    public List<WeatherCity> searchCities(String keyword) {
        try {
            logger.info("Searching cities with keyword: {}", keyword);
            return weatherCityMapper.searchByKeyword(keyword);
        } catch (Exception e) {
            logger.error("Error searching cities with keyword {}: {}", keyword, e.getMessage(), e);
            throw new BusinessException("搜索城市异常: " + e.getMessage());
        }
    }

    public WeatherCity getCityByCode(String cityCode) {
        try {
            logger.info("Getting city info for cityCode: {}", cityCode);
            WeatherCity city = weatherCityMapper.selectByCityCode(cityCode);
            if (city == null) {
                throw new BusinessException("未找到城市代码: " + cityCode);
            }
            return city;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Error getting city by code {}: {}", cityCode, e.getMessage(), e);
            throw new BusinessException("获取城市信息异常: " + e.getMessage());
        }
    }

    public List<Map<String, Object>> calculateDynamicRisks(Map<String, Object> weatherData) {
        List<Map<String, Object>> risks = new ArrayList<>();
        try {
            JSONObject data = (JSONObject) weatherData.get("data");
            if (data == null) {
                logger.warn("Weather data is missing 'data' field");
                return risks;
            }

            String wenduStr = data.getString("wendu");
            String quality = data.getString("quality");
            String shidu = data.getString("shidu");
            com.alibaba.fastjson.JSONArray forecastArray = data.getJSONArray("forecast");

            String todayType = "";
            String todayHigh = "";
            String todayLow = "";
            String todayFx = "";
            String todayFl = "";
            String todayNotice = "";

            if (forecastArray != null && !forecastArray.isEmpty()) {
                JSONObject today = forecastArray.getJSONObject(0);
                todayType = today.getString("type");
                todayHigh = today.getString("high");
                todayLow = today.getString("low");
                todayFx = today.getString("fx");
                todayFl = today.getString("fl");
                todayNotice = today.getString("notice");
            }

            double temp = 0;
            try {
                if (wenduStr != null && !wenduStr.isEmpty()) {
                    temp = Double.parseDouble(wenduStr);
                }
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse temperature: {}", wenduStr);
            }

            double highTemp = temp;
            double lowTemp = temp;
            try {
                if (todayHigh != null && todayHigh.contains("高温")) {
                    String highNum = todayHigh.replaceAll("[^0-9.-]", "");
                    if (!highNum.isEmpty()) {
                        highTemp = Double.parseDouble(highNum);
                    }
                }
                if (todayLow != null && todayLow.contains("低温")) {
                    String lowNum = todayLow.replaceAll("[^0-9.-]", "");
                    if (!lowNum.isEmpty()) {
                        lowTemp = Double.parseDouble(lowNum);
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse high/low temperature");
            }

            int windLevel = 0;
            try {
                if (todayFl != null && todayFl.contains("级")) {
                    String levelStr = todayFl.replaceAll("[^0-9]", "");
                    if (!levelStr.isEmpty()) {
                        windLevel = Integer.parseInt(levelStr);
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse wind level: {}", todayFl);
            }

            int humidity = 0;
            try {
                if (shidu != null && shidu.contains("%")) {
                    String humStr = shidu.replaceAll("[^0-9]", "");
                    if (!humStr.isEmpty()) {
                        humidity = Integer.parseInt(humStr);
                    }
                }
            } catch (NumberFormatException e) {
                logger.warn("Failed to parse humidity: {}", shidu);
            }

            if (todayType.contains("雷") || todayType.contains("雷雨") || todayType.contains("雷暴")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "雷暴天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "⚡");
                risk.put("description", "当前为雷暴/雷雨天气，存在雷击风险");
                risk.put("measures", Arrays.asList("禁止高空作业", "禁止带电作业", "人员应撤离到安全区域", "设备做好防雷接地检查"));
                risk.put("workRestriction", "禁止高空作业和带电作业");
                risks.add(risk);
            }

            if (todayType.contains("暴雨") || todayType.contains("大暴雨") || todayType.contains("特大暴雨")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "暴雨天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🌊");
                risk.put("description", "当前为暴雨天气，户外作业存在严重安全隐患");
                risk.put("measures", Arrays.asList("立即停止户外作业", "检查排水系统", "做好防汛准备", "妥善保管电气设备防止受潮", "人员转移到安全地带"));
                risk.put("workRestriction", "立即停止所有户外作业");
                risks.add(risk);
            } else if (todayType.contains("大雨")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "大雨天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🌧️");
                risk.put("description", "当前为大雨天气，户外作业存在安全隐患");
                risk.put("measures", Arrays.asList("禁止户外作业", "检查排水系统", "做好防汛准备", "妥善保管电气设备防止受潮"));
                risk.put("workRestriction", "禁止户外作业");
                risks.add(risk);
            } else if (todayType.contains("中雨")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "中雨天气风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "🌧️");
                risk.put("description", "当前为中雨天气，地面湿滑，影响户外作业");
                risk.put("measures", Arrays.asList("注意防滑", "做好防雨措施", "避免高空作业", "电气设备注意防水"));
                risk.put("workRestriction", "减少户外作业，必要时做好防护");
                risks.add(risk);
            }

            if (highTemp >= 37 || temp >= 37) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "高温酷暑风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🔥");
                risk.put("description", "当前气温极高（≥37℃），极易中暑");
                risk.put("measures", Arrays.asList("立即停止高温时段作业", "配备防暑降温药品", "保证充足饮水供应", "采取通风降温措施", "安排轮班休息"));
                risk.put("workRestriction", "停止高温时段（11:00-15:00）户外作业");
                risks.add(risk);
            } else if (highTemp >= 35 || temp >= 35) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "高温天气风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "🔥");
                risk.put("description", "当前气温较高（≥35℃），作业人员易中暑");
                risk.put("measures", Arrays.asList("注意防暑降温", "避免高温时段作业", "配备防暑药品", "定时轮换休息", "保证充足饮水"));
                risk.put("workRestriction", "调整作业时间，避开高温时段");
                risks.add(risk);
            }

            if (lowTemp <= -10 || temp <= -10) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "严寒天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🥶");
                risk.put("description", "当前气温极低（≤-10℃），有严重冻伤风险");
                risk.put("measures", Arrays.asList("停止户外作业", "加强保暖措施", "设备做好防冻", "关注人员身体状况", "提供热饮和保暖物资"));
                risk.put("workRestriction", "停止户外作业");
                risks.add(risk);
            } else if (lowTemp <= 0 || temp <= 0) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "低温天气风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "🥶");
                risk.put("description", "当前气温较低（≤0℃），存在冻伤和滑倒风险");
                risk.put("measures", Arrays.asList("注意防冻防滑", "穿戴保暖防护用品", "及时清理作业面积雪积冰", "设备做好防冻措施"));
                risk.put("workRestriction", "注意保暖，减少户外停留时间");
                risks.add(risk);
            }

            if (windLevel >= 8) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "强风天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🌪️");
                risk.put("description", "当前风力极大（≥8级），高空作业非常危险");
                risk.put("measures", Arrays.asList("立即停止所有高空作业", "加固临时设施和设备", "人员撤离到安全区域", "检查户外设施稳固性"));
                risk.put("workRestriction", "立即停止所有高空作业");
                risks.add(risk);
            } else if (windLevel >= 6) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "大风天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🌪️");
                risk.put("description", "当前风力较大（≥6级），高空作业有坠落风险");
                risk.put("measures", Arrays.asList("禁止高空作业", "加固临时设施和设备", "避免在高空设备附近停留", "检查户外广告牌等易坠物"));
                risk.put("workRestriction", "禁止高空作业");
                risks.add(risk);
            } else if (windLevel >= 4) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "有风天气注意");
                risk.put("riskLevel", "info");
                risk.put("icon", "💨");
                risk.put("description", "当前风力3-5级，高空作业需注意");
                risk.put("measures", Arrays.asList("做好防风措施", "高空作业系好安全带", "轻小物体妥善放置"));
                risk.put("workRestriction", "正常作业，注意防风");
                risks.add(risk);
            }

            if (quality != null && (quality.contains("重") || quality.contains("严重"))) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "重度雾霾风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "🌫️");
                risk.put("description", "当前空气质量严重污染，能见度极低");
                risk.put("measures", Arrays.asList("停止户外作业", "佩戴专业防护口罩", "加强作业现场照明", "人员减少户外活动"));
                risk.put("workRestriction", "停止户外作业");
                risks.add(risk);
            } else if (quality != null && (quality.contains("中度") || quality.contains("中"))) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "中度雾霾风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "🌫️");
                risk.put("description", "当前空气质量中度污染，能见度较低");
                risk.put("measures", Arrays.asList("注意交通安全", "佩戴防护口罩", "减少户外作业时间", "加强作业现场照明"));
                risk.put("workRestriction", "注意交通安全，必要时暂停户外作业");
                risks.add(risk);
            }

            if (todayType.contains("暴雪") || todayType.contains("大暴雪")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "暴雪天气风险");
                risk.put("riskLevel", "high");
                risk.put("icon", "❄️");
                risk.put("description", "当前为暴雪天气，严重影响作业安全");
                risk.put("measures", Arrays.asList("停止户外作业", "及时清理积雪", "加强保暖防冻", "车辆安装防滑链", "检查建筑物承重"));
                risk.put("workRestriction", "停止户外作业");
                risks.add(risk);
            } else if (todayType.contains("雪") || todayType.contains("中雪")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "降雪天气风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "❄️");
                risk.put("description", "当前为降雪天气，路面湿滑");
                risk.put("measures", Arrays.asList("注意防滑", "及时清理作业面积雪", "穿戴防滑鞋具", "车辆安装防滑链"));
                risk.put("workRestriction", "注意防滑，谨慎作业");
                risks.add(risk);
            }

            if (todayType.contains("雾") || todayType.contains("大雾")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "大雾天气风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "🌫️");
                risk.put("description", "当前为大雾天气，能见度低");
                risk.put("measures", Arrays.asList("注意交通安全", "加强现场照明", "减少车辆行驶", "人员作业保持安全距离"));
                risk.put("workRestriction", "注意安全，必要时暂停作业");
                risks.add(risk);
            }

            if (humidity >= 80 && todayType.contains("雨")) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "高湿触电风险");
                risk.put("riskLevel", "medium");
                risk.put("icon", "⚡");
                risk.put("description", "空气湿度大，存在触电隐患");
                risk.put("measures", Arrays.asList("检查电气绝缘", "作业人员穿戴绝缘防护", "避免带电作业", "设备做好防潮"));
                risk.put("workRestriction", "谨慎进行电气作业");
                risks.add(risk);
            }

            if (risks.isEmpty()) {
                Map<String, Object> risk = new HashMap<>();
                risk.put("riskName", "天气状况良好");
                risk.put("riskLevel", "info");
                risk.put("icon", "✅");
                risk.put("description", "当前天气状况良好，适宜正常作业");
                risk.put("measures", Arrays.asList("正常作业", "做好常规安全检查", "关注天气变化"));
                risk.put("workRestriction", "无特殊限制");
                risks.add(risk);
            }

        } catch (Exception e) {
            logger.error("Error calculating dynamic risks: {}", e.getMessage(), e);
            throw new BusinessException("计算动态风险异常: " + e.getMessage());
        }
        return risks;
    }

    public List<Map<String, Object>> generateHourlyForecast(Map<String, Object> weatherData) {
        List<Map<String, Object>> hourlyList = new ArrayList<>();
        try {
            JSONObject data = (JSONObject) weatherData.get("data");
            if (data == null) {
                return hourlyList;
            }

            double currentTemp = 0;
            String wenduStr = data.getString("wendu");
            try {
                if (wenduStr != null && !wenduStr.isEmpty()) {
                    currentTemp = Double.parseDouble(wenduStr);
                }
            } catch (NumberFormatException e) {
                currentTemp = 25;
            }

            com.alibaba.fastjson.JSONArray forecastArray = data.getJSONArray("forecast");
            double todayHigh = currentTemp;
            double todayLow = currentTemp;
            double tomorrowHigh = currentTemp;
            double tomorrowLow = currentTemp;
            String todayType = "晴";
            String tomorrowType = "晴";

            if (forecastArray != null && forecastArray.size() >= 2) {
                JSONObject today = forecastArray.getJSONObject(0);
                JSONObject tomorrow = forecastArray.getJSONObject(1);

                todayType = today.getString("type");
                tomorrowType = tomorrow.getString("type");

                try {
                    String highStr = today.getString("high").replaceAll("[^0-9.-]", "");
                    String lowStr = today.getString("low").replaceAll("[^0-9.-]", "");
                    if (!highStr.isEmpty()) todayHigh = Double.parseDouble(highStr);
                    if (!lowStr.isEmpty()) todayLow = Double.parseDouble(lowStr);
                } catch (Exception e) {
                    logger.warn("Failed to parse today temp");
                }

                try {
                    String highStr = tomorrow.getString("high").replaceAll("[^0-9.-]", "");
                    String lowStr = tomorrow.getString("low").replaceAll("[^0-9.-]", "");
                    if (!highStr.isEmpty()) tomorrowHigh = Double.parseDouble(highStr);
                    if (!lowStr.isEmpty()) tomorrowLow = Double.parseDouble(lowStr);
                } catch (Exception e) {
                    logger.warn("Failed to parse tomorrow temp");
                }
            }

            int currentHour = java.time.LocalTime.now().getHour();

            for (int i = 0; i < 12; i++) {
                int hour = (currentHour + i) % 24;
                Map<String, Object> hourData = new HashMap<>();

                String timeStr = String.format("%02d:00", hour);
                hourData.put("time", timeStr);
                hourData.put("hour", hour);

                double temp = calculateHourTemp(hour, currentTemp, todayHigh, todayLow, tomorrowHigh, tomorrowLow, currentHour + i);
                hourData.put("temp", Math.round(temp * 10) / 10.0);

                String weatherType = getHourWeatherType(hour, todayType, tomorrowType, currentHour + i);
                hourData.put("weather", weatherType);
                hourData.put("icon", getWeatherEmoji(weatherType));

                int windLevel = 2 + (int)(Math.random() * 2);
                hourData.put("windLevel", windLevel + "级");
                hourData.put("windDir", "东南风");

                hourlyList.add(hourData);
            }

        } catch (Exception e) {
            logger.error("Error generating hourly forecast: {}", e.getMessage(), e);
        }
        return hourlyList;
    }

    private double calculateHourTemp(int hour, double currentTemp, double todayHigh, double todayLow, double tomorrowHigh, double tomorrowLow, int hoursFromNow) {
        double dayProgress = (hour - 6.0) / 12.0;
        if (dayProgress < 0) dayProgress = 0;
        if (dayProgress > 1) dayProgress = 1;

        double temp;
        if (hour >= 6 && hour <= 14) {
            double progress = (hour - 6.0) / 8.0;
            temp = todayLow + (todayHigh - todayLow) * Math.sin(progress * Math.PI / 2);
        } else if (hour > 14 && hour <= 22) {
            double progress = (hour - 14.0) / 8.0;
            temp = todayHigh - (todayHigh - todayLow) * Math.sin(progress * Math.PI / 2);
        } else {
            if (hour > 22) {
                double progress = (hour - 22.0) / 8.0;
                temp = todayLow + (tomorrowLow - todayLow) * progress * 0.5;
            } else {
                double progress = hour / 6.0;
                temp = todayLow + (tomorrowHigh - todayLow) * progress * 0.3;
            }
        }

        if (hoursFromNow <= 1) {
            temp = currentTemp + (temp - currentTemp) * 0.3;
        }

        return temp;
    }

    private String getHourWeatherType(int hour, String todayType, String tomorrowType, int hoursFromNow) {
        if (hoursFromNow < 12) {
            return todayType;
        } else {
            return tomorrowType;
        }
    }

    private String getWeatherEmoji(String weatherType) {
        if (weatherType == null) return "☀️";
        if (weatherType.contains("雷") || weatherType.contains("雷暴")) return "⛈️";
        if (weatherType.contains("暴雨") || weatherType.contains("大暴雨")) return "🌧️";
        if (weatherType.contains("大雨")) return "🌧️";
        if (weatherType.contains("中雨")) return "🌧️";
        if (weatherType.contains("小雨")) return "🌦️";
        if (weatherType.contains("雪")) return "❄️";
        if (weatherType.contains("雾")) return "🌫️";
        if (weatherType.contains("霾")) return "🌫️";
        if (weatherType.contains("多云")) return "⛅";
        if (weatherType.contains("阴")) return "☁️";
        if (weatherType.contains("晴")) return "☀️";
        if (weatherType.contains("风")) return "💨";
        return "🌤️";
    }

    public Map<String, Object> generateWeatherAlert(Map<String, Object> weatherData) {
        Map<String, Object> alert = new HashMap<>();
        try {
            JSONObject data = (JSONObject) weatherData.get("data");
            if (data == null) {
                alert.put("hasAlert", false);
                return alert;
            }

            com.alibaba.fastjson.JSONArray forecastArray = data.getJSONArray("forecast");
            String todayType = "";
            String todayHigh = "";
            String todayFl = "";
            String quality = data.getString("quality");

            if (forecastArray != null && !forecastArray.isEmpty()) {
                JSONObject today = forecastArray.getJSONObject(0);
                todayType = today.getString("type");
                todayHigh = today.getString("high");
                todayFl = today.getString("fl");
            }

            List<Map<String, Object>> alerts = new ArrayList<>();
            boolean hasHighAlert = false;

            if (todayType.contains("暴雨") || todayType.contains("大暴雨")) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "暴雨预警");
                a.put("level", "red");
                a.put("levelText", "红色预警");
                a.put("description", "预计将出现暴雨天气，请做好防汛准备");
                alerts.add(a);
                hasHighAlert = true;
            } else if (todayType.contains("大雨")) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "暴雨预警");
                a.put("level", "orange");
                a.put("levelText", "橙色预警");
                a.put("description", "预计将出现大雨天气，请注意防范");
                alerts.add(a);
                hasHighAlert = true;
            } else if (todayType.contains("中雨")) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "降雨提醒");
                a.put("level", "yellow");
                a.put("levelText", "黄色提醒");
                a.put("description", "预计将出现中雨天气，请做好防雨措施");
                alerts.add(a);
            }

            if (todayType.contains("雷") || todayType.contains("雷暴")) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "雷电预警");
                a.put("level", "orange");
                a.put("levelText", "橙色预警");
                a.put("description", "预计将有雷暴天气，注意防雷");
                alerts.add(a);
                hasHighAlert = true;
            }

            double highTemp = 0;
            try {
                if (todayHigh != null && todayHigh.contains("高温")) {
                    String highNum = todayHigh.replaceAll("[^0-9.-]", "");
                    if (!highNum.isEmpty()) highTemp = Double.parseDouble(highNum);
                }
            } catch (NumberFormatException e) {
                // ignore
            }

            if (highTemp >= 37) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "高温预警");
                a.put("level", "red");
                a.put("levelText", "红色预警");
                a.put("description", "最高气温将达到37℃以上，请注意防暑降温");
                alerts.add(a);
                hasHighAlert = true;
            } else if (highTemp >= 35) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "高温提醒");
                a.put("level", "orange");
                a.put("levelText", "橙色提醒");
                a.put("description", "最高气温将达到35℃以上，注意防暑");
                alerts.add(a);
                hasHighAlert = true;
            }

            int windLevel = 0;
            try {
                if (todayFl != null && todayFl.contains("级")) {
                    String levelStr = todayFl.replaceAll("[^0-9]", "");
                    if (!levelStr.isEmpty()) windLevel = Integer.parseInt(levelStr);
                }
            } catch (NumberFormatException e) {
                // ignore
            }

            if (windLevel >= 8) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "大风预警");
                a.put("level", "red");
                a.put("levelText", "红色预警");
                a.put("description", "预计风力将达到8级以上，请做好防风准备");
                alerts.add(a);
                hasHighAlert = true;
            } else if (windLevel >= 6) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "大风预警");
                a.put("level", "blue");
                a.put("levelText", "蓝色预警");
                a.put("description", "预计风力将达到6级以上，注意防风");
                alerts.add(a);
                hasHighAlert = true;
            }

            if (todayType.contains("暴雪") || todayType.contains("大暴雪")) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "暴雪预警");
                a.put("level", "red");
                a.put("levelText", "红色预警");
                a.put("description", "预计将出现暴雪天气，请注意防范");
                alerts.add(a);
                hasHighAlert = true;
            }

            if (quality != null && quality.contains("重")) {
                Map<String, Object> a = new HashMap<>();
                a.put("type", "重污染预警");
                a.put("level", "orange");
                a.put("levelText", "橙色预警");
                a.put("description", "空气质量重度污染，请注意防护");
                alerts.add(a);
                hasHighAlert = true;
            }

            alert.put("hasAlert", !alerts.isEmpty());
            alert.put("hasHighAlert", hasHighAlert);
            alert.put("alerts", alerts);
            alert.put("alertCount", alerts.size());

        } catch (Exception e) {
            logger.error("Error generating weather alert: {}", e.getMessage(), e);
            alert.put("hasAlert", false);
        }
        return alert;
    }
}
