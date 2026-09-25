package org.example.eledangercheck.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import org.example.eledangercheck.entity.WeatherCity;
import org.example.eledangercheck.exception.BusinessException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * AGENT天气校验服务
 * 在生成安全交底卡前检查作业地点和日期的天气情况
 * 恶劣天气时拒绝生成并给出醒目提醒
 */
@Service
public class AgentWeatherService {

    private static final Logger logger = LoggerFactory.getLogger(AgentWeatherService.class);

    private final WeatherRiskService weatherRiskService;

    public AgentWeatherService(WeatherRiskService weatherRiskService) {
        this.weatherRiskService = weatherRiskService;
    }

    /**
     * 检查作业天气是否适合
     * @param cityCode 城市编码
     * @param cityName 城市名称
     * @param workDate 作业日期 (yyyy-MM-dd)
     * @return 天气检查结果
     */
    public Map<String, Object> checkWorkWeather(String cityCode, String cityName, String workDate) {
        Map<String, Object> result = new HashMap<>();
        result.put("cityCode", cityCode);
        result.put("cityName", cityName);
        result.put("workDate", workDate);

        try {
            // 获取天气数据
            Map<String, Object> weatherData = weatherRiskService.getWeatherByCityCode(cityCode);
            JSONObject data = JSON.parseObject(JSON.toJSONString(weatherData.get("data")));

            // 查找目标日期的天气预报
            JSONObject forecastDay = findForecastByDate(data, workDate);

            if (forecastDay == null) {
                // 没有该日期的预报（超过7天或历史日期）
                result.put("hasForecast", false);
                result.put("isSafe", true);
                result.put("weatherInfo", null);
                result.put("message", "暂无" + workDate + "的天气预报，建议作业前再次确认天气情况");
                result.put("highRisks", new ArrayList<>());
                result.put("mediumRisks", new ArrayList<>());
                return result;
            }

            // 有预报数据，计算风险
            result.put("hasForecast", true);

            // 提取天气信息
            Map<String, Object> weatherInfo = extractWeatherInfo(forecastDay);
            result.put("weatherInfo", weatherInfo);

            // 计算动态风险
            List<Map<String, Object>> risks = calculateRisksFromForecast(forecastDay);
            List<Map<String, Object>> highRisks = new ArrayList<>();
            List<Map<String, Object>> mediumRisks = new ArrayList<>();

            for (Map<String, Object> risk : risks) {
                String level = (String) risk.get("riskLevel");
                if ("high".equals(level)) {
                    highRisks.add(risk);
                } else if ("medium".equals(level)) {
                    mediumRisks.add(risk);
                }
            }

            result.put("highRisks", highRisks);
            result.put("mediumRisks", mediumRisks);

            // 判断是否安全（高危风险时不安全）
            boolean isSafe = highRisks.isEmpty();
            result.put("isSafe", isSafe);

            if (!isSafe) {
                // 构建拒绝消息
                StringBuilder msg = new StringBuilder();
                msg.append("⚠️ 【恶劣天气警告】").append(cityName).append(" ").append(workDate).append(" 不适合作业\n\n");
                msg.append("检测到以下高危风险：\n");
                for (int i = 0; i < highRisks.size(); i++) {
                    Map<String, Object> r = highRisks.get(i);
                    msg.append(i + 1).append(". ").append(r.get("riskName"))
                       .append(" - ").append(r.get("description")).append("\n");
                }
                msg.append("\n📌 作业限制：\n");
                Set<String> restrictions = new LinkedHashSet<>();
                for (Map<String, Object> r : highRisks) {
                    String restriction = (String) r.get("workRestriction");
                    if (restriction != null && !restriction.isEmpty() && !"无特殊限制".equals(restriction)) {
                        restrictions.add("• " + restriction);
                    }
                }
                for (String r : restrictions) {
                    msg.append(r).append("\n");
                }
                msg.append("\n❌ 为确保作业安全，系统拒绝在该天气条件下生成安全交底卡。\n");
                msg.append("建议：调整作业日期或等待天气条件改善后再进行作业。");

                result.put("message", msg.toString());
            } else if (!mediumRisks.isEmpty()) {
                result.put("message", "ℹ️ " + cityName + " " + workDate + " 存在中等天气风险，请注意防范，可正常生成交底卡。");
            } else {
                result.put("message", "✅ " + cityName + " " + workDate + " 天气良好，适合电力作业。");
            }

            return result;

        } catch (Exception e) {
            logger.error("天气校验失败: {}", e.getMessage(), e);
            result.put("hasForecast", false);
            result.put("isSafe", true);
            result.put("weatherInfo", null);
            result.put("message", "天气数据获取失败（" + e.getMessage() + "），将跳过天气检查");
            result.put("highRisks", new ArrayList<>());
            result.put("mediumRisks", new ArrayList<>());
            result.put("weatherError", e.getMessage());
            return result;
        }
    }

    /**
     * 根据日期查找对应的天气预报
     */
    private JSONObject findForecastByDate(JSONObject data, String workDate) {
        try {
            JSONArray forecast = data.getJSONArray("forecast");
            if (forecast == null || forecast.isEmpty()) {
                return null;
            }

            LocalDate targetDate = LocalDate.parse(workDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));

            for (int i = 0; i < forecast.size(); i++) {
                JSONObject day = forecast.getJSONObject(i);
                String ymd = day.getString("ymd");
                if (ymd != null && ymd.equals(workDate)) {
                    return day;
                }
            }

            return null;
        } catch (Exception e) {
            logger.warn("查找天气预报失败: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 从预报数据中提取天气信息
     */
    private Map<String, Object> extractWeatherInfo(JSONObject day) {
        Map<String, Object> info = new HashMap<>();
        info.put("date", day.getString("ymd"));
        info.put("week", day.getString("week"));
        info.put("type", day.getString("type"));
        info.put("high", day.getString("high"));
        info.put("low", day.getString("low"));
        info.put("windDir", day.getString("fx"));
        info.put("windScale", day.getString("fl"));
        info.put("aqi", day.getInteger("aqi"));
        info.put("notice", day.getString("notice"));
        info.put("sunrise", day.getString("sunrise"));
        info.put("sunset", day.getString("sunset"));
        return info;
    }

    /**
     * 根据单日预报计算风险（基于WeatherRiskService的逻辑简化版）
     */
    private List<Map<String, Object>> calculateRisksFromForecast(JSONObject day) {
        List<Map<String, Object>> risks = new ArrayList<>();

        String type = day.getString("type");
        String high = day.getString("high");
        String low = day.getString("low");
        String fl = day.getString("fl");
        Integer aqi = day.getInteger("aqi");
        String notice = day.getString("notice");

        // 解析温度
        int highTemp = extractTemp(high);
        int lowTemp = extractTemp(low);

        // 解析风力
        int windLevel = extractWindLevel(fl);

        // 1. 雷暴风险
        if (type != null && (type.contains("雷") || type.contains("雷雨") || type.contains("雷暴"))) {
            risks.add(buildRisk("雷暴天气", "high", "⚡",
                "预报有雷暴天气，户外作业存在雷击风险",
                Arrays.asList("立即停止所有户外作业", "人员撤离到安全建筑物内", "远离高大树木、杆塔等易遭雷击物体"),
                "立即停止所有户外作业"));
        }

        // 2. 降雨风险
        if (type != null) {
            if (type.contains("暴雨") || type.contains("大暴雨") || type.contains("特大暴雨")) {
                risks.add(buildRisk("暴雨天气", "high", "🌧️",
                    "预报有暴雨，户外作业环境恶劣，存在触电和滑坡风险",
                    Arrays.asList("停止所有户外作业", "检查排水设施", "做好防汛准备"),
                    "立即停止所有户外作业"));
            } else if (type.contains("大雨")) {
                risks.add(buildRisk("大雨天气", "high", "🌧️",
                    "预报有大雨，户外作业存在滑倒、触电风险",
                    Arrays.asList("停止户外作业", "做好防雨措施"),
                    "停止户外作业"));
            } else if (type.contains("中雨")) {
                risks.add(buildRisk("中雨天气", "medium", "🌦️",
                    "预报有中雨，作业条件受影响",
                    Arrays.asList("减少户外作业", "做好防雨防滑措施"),
                    "减少户外作业，必要时做好防护"));
            }
        }

        // 3. 高温风险
        if (highTemp >= 37) {
            risks.add(buildRisk("高温预警", "high", "🔥",
                "最高气温达" + highTemp + "℃，存在中暑风险",
                Arrays.asList("停止高温时段户外作业", "配备防暑降温用品", "增加休息频次"),
                "停止高温时段（11:00-15:00）户外作业"));
        } else if (highTemp >= 35) {
            risks.add(buildRisk("高温天气", "medium", "🌡️",
                "最高气温达" + highTemp + "℃，注意防暑降温",
                Arrays.asList("调整作业时间避开高温", "配备防暑用品"),
                "调整作业时间，避开高温时段"));
        }

        // 4. 大风风险
        if (windLevel >= 8) {
            risks.add(buildRisk("大风预警", "high", "💨",
                "风力达" + windLevel + "级，高空作业极其危险",
                Arrays.asList("立即停止所有高空作业", "加固临时设施", "人员撤离危险区域"),
                "立即停止所有高空作业"));
        } else if (windLevel >= 6) {
            risks.add(buildRisk("大风天气", "high", "💨",
                "风力达" + windLevel + "级，高空作业危险",
                Arrays.asList("禁止高空作业", "注意防范坠物"),
                "禁止高空作业"));
        }

        // 5. 雾霾/空气质量
        if (aqi != null && aqi > 200) {
            risks.add(buildRisk("重度污染", "high", "😷",
                "空气质量指数" + aqi + "，重度污染",
                Arrays.asList("减少户外作业", "佩戴防护口罩"),
                "停止户外作业"));
        } else if (aqi != null && aqi > 150) {
            risks.add(buildRisk("中度污染", "medium", "😷",
                "空气质量指数" + aqi + "，中度污染",
                Arrays.asList("减少户外停留时间", "佩戴防护口罩"),
                "注意安全，必要时暂停作业"));
        }

        // 6. 降雪风险
        if (type != null && (type.contains("暴雪") || type.contains("大暴雪"))) {
            risks.add(buildRisk("暴雪天气", "high", "❄️",
                "预报有暴雪，作业环境恶劣",
                Arrays.asList("停止户外作业", "做好防寒防冻"),
                "停止户外作业"));
        } else if (type != null && (type.contains("雪") && !type.contains("小雪"))) {
            risks.add(buildRisk("降雪天气", "medium", "🌨️",
                "预报有降雪，注意防滑防冻",
                Arrays.asList("注意防滑保暖", "谨慎作业"),
                "注意防滑，谨慎作业"));
        }

        // 7. 大雾
        if (type != null && (type.contains("雾") || type.contains("大雾"))) {
            risks.add(buildRisk("大雾天气", "medium", "🌫️",
                "预报有大雾，能见度低",
                Arrays.asList("注意交通安全", "必要时暂停户外作业"),
                "注意交通安全，必要时暂停户外作业"));
        }

        // 8. 沙尘
        if (type != null && (type.contains("沙尘") || type.contains("沙尘暴"))) {
            risks.add(buildRisk("沙尘天气", "medium", "🌪️",
                "预报有沙尘天气",
                Arrays.asList("佩戴防护用品", "减少户外停留"),
                "注意安全，必要时暂停作业"));
        }

        return risks;
    }

    /**
     * 构建风险对象
     */
    private Map<String, Object> buildRisk(String name, String level, String icon,
                                          String description, List<String> measures, String restriction) {
        Map<String, Object> risk = new HashMap<>();
        risk.put("riskName", name);
        risk.put("riskLevel", level);
        risk.put("icon", icon);
        risk.put("description", description);
        risk.put("measures", measures);
        risk.put("workRestriction", restriction);
        return risk;
    }

    /**
     * 从温度字符串提取数字
     */
    private int extractTemp(String tempStr) {
        if (tempStr == null || tempStr.isEmpty()) return 0;
        try {
            // 格式如 "高温 35℃"
            String numStr = tempStr.replaceAll("[^0-9-]", "");
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 从风力字符串提取数字
     */
    private int extractWindLevel(String fl) {
        if (fl == null || fl.isEmpty()) return 0;
        try {
            // 格式如 "3级"
            String numStr = fl.replaceAll("[^0-9]", "");
            if (numStr.isEmpty()) return 0;
            return Integer.parseInt(numStr);
        } catch (Exception e) {
            return 0;
        }
    }

    /**
     * 搜索城市
     */
    public List<WeatherCity> searchCities(String keyword) {
        return weatherRiskService.searchCities(keyword);
    }
}
