package org.example.eledangercheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("safety_measure")
public class SafetyMeasure {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "hazard_id")
    private Long hazardId;

    @TableField(value = "measure_code")
    private String measureCode;

    @TableField(value = "measure_name")
    private String measureName;

    @TableField(value = "measure_desc")
    private String measureDesc;

    @TableField(value = "priority")
    private Integer priority;

    @TableField(value = "status")
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getHazardId() {
        return hazardId;
    }

    public void setHazardId(Long hazardId) {
        this.hazardId = hazardId;
    }

    public String getMeasureCode() {
        return measureCode;
    }

    public void setMeasureCode(String measureCode) {
        this.measureCode = measureCode;
    }

    public String getMeasureName() {
        return measureName;
    }

    public void setMeasureName(String measureName) {
        this.measureName = measureName;
    }

    public String getMeasureDesc() {
        return measureDesc;
    }

    public void setMeasureDesc(String measureDesc) {
        this.measureDesc = measureDesc;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}