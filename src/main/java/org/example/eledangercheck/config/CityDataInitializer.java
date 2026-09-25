package org.example.eledangercheck.config;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import jakarta.annotation.PostConstruct;
import org.example.eledangercheck.entity.WeatherCity;
import org.example.eledangercheck.mapper.WeatherCityMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Component
public class CityDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(CityDataInitializer.class);

    private static final String CITY_JSON_URL = "http://cdn.sojson.com/_city.json";

    @Autowired
    private WeatherCityMapper weatherCityMapper;

    @Autowired
    private RestTemplate restTemplate;

    @PostConstruct
    public void initCityData() {
        try {
            Long count = weatherCityMapper.selectCount(null);
            if (count != null && count > 100) {
                logger.info("城市数据已存在（{}条），补充关键城市", count);
                ensureKeyCities();
                return;
            }
            logger.info("开始初始化城市数据...");
            boolean success = fetchAndSaveCities();
            if (!success) {
                logger.warn("从远程拉取城市数据失败，使用内置常用城市数据兜底");
                initFallbackCities();
                ensureKeyCities();
            } else {
                // 拉取成功也补充关键城市（防止漏了）
                ensureKeyCities();
            }
        } catch (Exception e) {
            logger.error("城市数据初始化异常: {}", e.getMessage(), e);
            try {
                initFallbackCities();
                ensureKeyCities();
            } catch (Exception ex) {
                logger.error("兜底城市数据初始化也失败: {}", ex.getMessage());
            }
        }
    }

    // 确保关键城市存在（电力行业相关城市）
    private void ensureKeyCities() {
        String[][] keyCities = {
            {"101200101", "武汉", "湖北"},
            {"101200201", "襄阳", "湖北"},
            {"101200301", "鄂州", "湖北"},
            {"101200401", "孝感", "湖北"},
            {"101200501", "黄冈", "湖北"},
            {"101200601", "黄石", "湖北"},
            {"101200701", "咸宁", "湖北"},
            {"101200801", "荆州", "湖北"},
            {"101200901", "宜昌", "湖北"},
            {"101201001", "恩施", "湖北"},
            {"101201101", "十堰", "湖北"},
            {"101201201", "神农架", "湖北"},
            {"101201301", "随州", "湖北"},
            {"101201401", "荆门", "湖北"},
            {"101201501", "天门", "湖北"},
            {"101201601", "仙桃", "湖北"},
            {"101201701", "潜江", "湖北"},
            {"101190101", "南京", "江苏"},
            {"101190201", "无锡", "江苏"},
            {"101190301", "镇江", "江苏"},
            {"101190401", "苏州", "江苏"},
            {"101280101", "广州", "广东"},
            {"101280601", "深圳", "广东"},
            {"101120101", "济南", "山东"},
            {"101120201", "青岛", "山东"},
            {"101030100", "天津", "天津"},
            {"101010100", "北京", "北京"},
            {"101020100", "上海", "上海"}
        };
        int count = 0;
        for (String[] city : keyCities) {
            try {
                WeatherCity existing = weatherCityMapper.selectByCityCode(city[0]);
                if (existing != null) continue;
                WeatherCity wc = new WeatherCity();
                wc.setCityCode(city[0]);
                wc.setCityName(city[1]);
                wc.setProvince(city[2]);
                weatherCityMapper.insert(wc);
                count++;
            } catch (Exception e) {
                // 忽略重复
            }
        }
        if (count > 0) {
            logger.info("补充关键城市数据完成，新增 {} 条", count);
        }
    }

    private boolean fetchAndSaveCities() {
        try {
            logger.info("从 {} 拉取城市数据...", CITY_JSON_URL);
            ResponseEntity<String> response = restTemplate.getForEntity(CITY_JSON_URL, String.class);
            if (!response.getStatusCode().is2xxSuccessful() || response.getBody() == null) {
                logger.error("拉取城市数据失败，状态码: {}", response.getStatusCode());
                return false;
            }
            JSONArray cityArray = JSON.parseArray(response.getBody());
            if (cityArray == null || cityArray.isEmpty()) {
                logger.error("城市数据为空");
                return false;
            }
            logger.info("获取到 {} 条城市数据，开始保存...", cityArray.size());
            List<WeatherCity> cityList = new ArrayList<>();
            int savedCount = 0;
            for (int i = 0; i < cityArray.size(); i++) {
                JSONObject cityObj = cityArray.getJSONObject(i);
                String cityCode = cityObj.getString("city_code");
                String cityName = cityObj.getString("city_name");
                String province = cityObj.getString("province");
                String pinyin = cityObj.getString("pinyin");
                if (cityCode == null || cityName == null) {
                    continue;
                }
                WeatherCity existing = weatherCityMapper.selectByCityCode(cityCode);
                if (existing != null) {
                    continue;
                }
                WeatherCity city = new WeatherCity();
                city.setCityCode(cityCode);
                city.setCityName(cityName);
                city.setProvince(province);
                city.setPinyin(pinyin);
                cityList.add(city);
                if (cityList.size() >= 100) {
                    batchInsert(cityList);
                    savedCount += cityList.size();
                    cityList.clear();
                }
            }
            if (!cityList.isEmpty()) {
                batchInsert(cityList);
                savedCount += cityList.size();
            }
            logger.info("城市数据初始化完成，共保存 {} 条", savedCount);
            return true;
        } catch (Exception e) {
            logger.error("拉取并保存城市数据失败: {}", e.getMessage(), e);
            return false;
        }
    }

    private void batchInsert(List<WeatherCity> cityList) {
        for (WeatherCity city : cityList) {
            try {
                weatherCityMapper.insert(city);
            } catch (Exception e) {
                // 忽略重复插入等错误
            }
        }
    }

    private void initFallbackCities() {
        String[][] cities = {
            {"101010100", "北京", "北京"},
            {"101010200", "海淀", "北京"},
            {"101010300", "朝阳", "北京"},
            {"101010400", "顺义", "北京"},
            {"101010500", "怀柔", "北京"},
            {"101010600", "通州", "北京"},
            {"101010700", "昌平", "北京"},
            {"101010800", "延庆", "北京"},
            {"101010900", "丰台", "北京"},
            {"101020100", "上海", "上海"},
            {"101020200", "闵行", "上海"},
            {"101020300", "宝山", "上海"},
            {"101020400", "嘉定", "上海"},
            {"101020500", "浦东", "上海"},
            {"101020600", "徐汇", "上海"},
            {"101030100", "天津", "天津"},
            {"101030300", "宝坻", "天津"},
            {"101030400", "东丽", "天津"},
            {"101040100", "重庆", "重庆"},
            {"101040200", "万州", "重庆"},
            {"101040300", "涪陵", "重庆"},
            {"101090101", "石家庄", "河北"},
            {"101100101", "太原", "山西"},
            {"101080101", "呼和浩特", "内蒙古"},
            {"101050101", "哈尔滨", "黑龙江"},
            {"101060101", "长春", "吉林"},
            {"101070101", "沈阳", "辽宁"},
            {"101190101", "南京", "江苏"},
            {"101210101", "杭州", "浙江"},
            {"101220101", "合肥", "安徽"},
            {"101230101", "福州", "福建"},
            {"101240101", "南昌", "江西"},
            {"101120101", "济南", "山东"},
            {"101180101", "郑州", "河南"},
            {"101200101", "武汉", "湖北"},
            {"101250101", "长沙", "湖南"},
            {"101280101", "广州", "广东"},
            {"101280601", "深圳", "广东"},
            {"101280701", "珠海", "广东"},
            {"101280301", "惠州", "广东"},
            {"101300101", "南宁", "广西"},
            {"101310101", "海口", "海南"},
            {"101270101", "成都", "四川"},
            {"101260101", "贵阳", "贵州"},
            {"101290101", "昆明", "云南"},
            {"101140101", "拉萨", "西藏"},
            {"101110101", "西安", "陕西"},
            {"101160101", "兰州", "甘肃"},
            {"101150101", "西宁", "青海"},
            {"101170101", "银川", "宁夏"},
            {"101130101", "乌鲁木齐", "新疆"},
            {"101320101", "香港", "香港"},
            {"101330101", "澳门", "澳门"},
            {"101340101", "台北", "台湾"},
            {"101190501", "南通", "江苏"},
            {"101190601", "扬州", "江苏"},
            {"101191101", "常州", "江苏"},
            {"101210301", "嘉兴", "浙江"},
            {"101210601", "台州", "浙江"},
            {"101210701", "温州", "浙江"},
            {"101120201", "青岛", "山东"},
            {"101120701", "济宁", "山东"},
            {"101120801", "泰安", "山东"},
            {"101180901", "洛阳", "河南"},
            {"101180301", "新乡", "河南"},
            {"101180501", "平顶山", "河南"},
            {"101230201", "厦门", "福建"},
            {"101070201", "大连", "辽宁"},
            {"101190401", "苏州", "江苏"},
            {"101190201", "无锡", "江苏"},
            {"101210801", "宁波", "浙江"},
            {"101280201", "佛山", "广东"},
            {"101280401", "东莞", "广东"},
            {"101270401", "绵阳", "四川"},
            {"101271401", "乐山", "四川"},
            {"101110301", "宝鸡", "陕西"},
            {"101180801", "安阳", "河南"},
            {"101200901", "宜昌", "湖北"},
            {"101250201", "株洲", "湖南"},
            {"101220301", "芜湖", "安徽"},
            {"101240201", "九江", "江西"},
            {"101090501", "唐山", "河北"},
            {"101100201", "大同", "山西"},
            {"101080201", "包头", "内蒙古"},
            {"101050301", "齐齐哈尔", "黑龙江"},
            {"101060201", "吉林", "吉林"},
            {"101120601", "烟台", "山东"},
            {"101300401", "桂林", "广西"}
        };
        int count = 0;
        for (String[] city : cities) {
            try {
                WeatherCity existing = weatherCityMapper.selectByCityCode(city[0]);
                if (existing != null) {
                    continue;
                }
                WeatherCity wc = new WeatherCity();
                wc.setCityCode(city[0]);
                wc.setCityName(city[1]);
                wc.setProvince(city[2]);
                weatherCityMapper.insert(wc);
                count++;
            } catch (Exception e) {
                // 忽略重复
            }
        }
        logger.info("兜底城市数据初始化完成，共保存 {} 条", count);
    }
}
