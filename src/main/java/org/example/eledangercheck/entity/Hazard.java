package org.example.eledangercheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("hazard")
public class Hazard {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "hazard_name")
    private String hazardName;

    @TableField(value = "category")
    private String category;

    @TableField(value = "hazard_level")
    private String hazardLevel;

    @TableField(value = "location")
    private String location;

    @TableField(value = "description")
    private String description;

    @TableField(value = "source_case")
    private String sourceCase;

    @TableField(value = "similarity")
    private Double similarity;

    @TableField(value = "task_id")
    private Long taskId;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getHazardName() {
        return hazardName;
    }

    public void setHazardName(String hazardName) {
        this.hazardName = hazardName;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getHazardLevel() {
        // 归一化风险等级，确保始终返回 high/medium/low
        return normalizeLevel(hazardLevel);
    }

    public void setHazardLevel(String hazardLevel) {
        this.hazardLevel = hazardLevel;
    }

    /**
     * 将各种风险等级表述归一化为标准的 high/medium/low
     */
    public static String normalizeLevel(String level) {
        if (level == null || level.isEmpty()) {
            return "low";
        }
        // 已经是标准值，直接返回
        if ("high".equalsIgnoreCase(level) || "medium".equalsIgnoreCase(level) || "low".equalsIgnoreCase(level)) {
            return level.toLowerCase();
        }
        // 中文高风险等级
        if (level.contains("高") || level.contains("重大") || level.contains("特别重大") || level.contains("严重")) {
            return "high";
        }
        // 中文中风险等级
        if (level.contains("中") || level.contains("较大")) {
            return "medium";
        }
        // 中文低风险等级
        if (level.contains("低") || level.contains("一般") || level.contains("轻微")) {
            return "low";
        }
        // 默认返回 low，避免显示"未知"
        return "low";
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceCase() {
        return sourceCase;
    }

    public void setSourceCase(String sourceCase) {
        this.sourceCase = sourceCase;
    }

    public Double getSimilarity() {
        return similarity;
    }

    public void setSimilarity(Double similarity) {
        this.similarity = similarity;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}