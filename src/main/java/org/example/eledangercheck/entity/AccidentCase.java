package org.example.eledangercheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("accident_case")
public class AccidentCase {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "case_name")
    private String caseName;

    @TableField(value = "accident_type")
    private String accidentType;

    @TableField(value = "work_type")
    private String workType;

    @TableField(value = "voltage_level")
    private String voltageLevel;

    @TableField(value = "equipment_type")
    private String equipmentType;

    @TableField(value = "severity")
    private String severity;

    @TableField(value = "case_desc")
    private String caseDesc;

    @TableField(value = "hazard_points")
    private String hazardPoints;

    @TableField(value = "cause_analysis")
    private String causeAnalysis;

    @TableField(value = "lessons_learned")
    private String lessonsLearned;

    @TableField(value = "occur_time")
    private LocalDateTime occurTime;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCaseName() {
        return caseName;
    }

    public void setCaseName(String caseName) {
        this.caseName = caseName;
    }

    public String getAccidentType() {
        return accidentType;
    }

    public void setAccidentType(String accidentType) {
        this.accidentType = accidentType;
    }

    public String getWorkType() {
        return workType;
    }

    public void setWorkType(String workType) {
        this.workType = workType;
    }

    public String getVoltageLevel() {
        return voltageLevel;
    }

    public void setVoltageLevel(String voltageLevel) {
        this.voltageLevel = voltageLevel;
    }

    public String getEquipmentType() {
        return equipmentType;
    }

    public void setEquipmentType(String equipmentType) {
        this.equipmentType = equipmentType;
    }

    public String getSeverity() {
        return severity;
    }

    public void setSeverity(String severity) {
        this.severity = severity;
    }

    public String getCaseDesc() {
        return caseDesc;
    }

    public void setCaseDesc(String caseDesc) {
        this.caseDesc = caseDesc;
    }

    public String getHazardPoints() {
        return hazardPoints;
    }

    public void setHazardPoints(String hazardPoints) {
        this.hazardPoints = hazardPoints;
    }

    public String getCauseAnalysis() {
        return causeAnalysis;
    }

    public void setCauseAnalysis(String causeAnalysis) {
        this.causeAnalysis = causeAnalysis;
    }

    public String getLessonsLearned() {
        return lessonsLearned;
    }

    public void setLessonsLearned(String lessonsLearned) {
        this.lessonsLearned = lessonsLearned;
    }

    public LocalDateTime getOccurTime() {
        return occurTime;
    }

    public void setOccurTime(LocalDateTime occurTime) {
        this.occurTime = occurTime;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }
}