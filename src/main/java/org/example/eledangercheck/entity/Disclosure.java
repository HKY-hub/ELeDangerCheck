package org.example.eledangercheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("disclosure")
public class Disclosure {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "disclosure_no")
    private String disclosureNo;

    @TableField(value = "title")
    private String title;

    @TableField(value = "task_id")
    private Long taskId;

    @TableField(value = "disclosure_type")
    private String disclosureType;

    @TableField(value = "content")
    private String content;

    @TableField(value = "pdf_path")
    private String pdfPath;

    @TableField(value = "word_path")
    private String wordPath;

    @TableField(value = "emergency_contact")
    private String emergencyContact;

    @TableField(value = "emergency_route")
    private String emergencyRoute;

    @TableField(value = "disclosure_status")
    private String disclosureStatus;

    @TableField(value = "issuer_signature")
    private String issuerSignature;

    @TableField(value = "receiver_signature")
    private String receiverSignature;

    @TableField(value = "disclosure_time")
    private LocalDateTime disclosureTime;

    @TableField(value = "confirm_time")
    private LocalDateTime confirmTime;

    @TableField(value = "create_time")
    private LocalDateTime createTime;

    @TableField(value = "update_time")
    private LocalDateTime updateTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDisclosureNo() {
        return disclosureNo;
    }

    public void setDisclosureNo(String disclosureNo) {
        this.disclosureNo = disclosureNo;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public Long getTaskId() {
        return taskId;
    }

    public void setTaskId(Long taskId) {
        this.taskId = taskId;
    }

    public String getDisclosureType() {
        return disclosureType;
    }

    public void setDisclosureType(String disclosureType) {
        this.disclosureType = disclosureType;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getPdfPath() {
        return pdfPath;
    }

    public void setPdfPath(String pdfPath) {
        this.pdfPath = pdfPath;
    }

    public String getWordPath() {
        return wordPath;
    }

    public void setWordPath(String wordPath) {
        this.wordPath = wordPath;
    }

    public String getEmergencyContact() {
        return emergencyContact;
    }

    public void setEmergencyContact(String emergencyContact) {
        this.emergencyContact = emergencyContact;
    }

    public String getEmergencyRoute() {
        return emergencyRoute;
    }

    public void setEmergencyRoute(String emergencyRoute) {
        this.emergencyRoute = emergencyRoute;
    }

    public String getDisclosureStatus() {
        return disclosureStatus;
    }

    public void setDisclosureStatus(String disclosureStatus) {
        this.disclosureStatus = disclosureStatus;
    }

    // 兼容前端status字段名（Jackson自动序列化getter方法）
    public String getStatus() {
        return this.disclosureStatus;
    }

    public void setStatus(String status) {
        this.disclosureStatus = status;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public LocalDateTime getUpdateTime() {
        return updateTime;
    }

    public void setUpdateTime(LocalDateTime updateTime) {
        this.updateTime = updateTime;
    }

    public String getIssuerSignature() {
        return issuerSignature;
    }

    public void setIssuerSignature(String issuerSignature) {
        this.issuerSignature = issuerSignature;
    }

    public String getReceiverSignature() {
        return receiverSignature;
    }

    public void setReceiverSignature(String receiverSignature) {
        this.receiverSignature = receiverSignature;
    }

    public LocalDateTime getDisclosureTime() {
        return disclosureTime;
    }

    public void setDisclosureTime(LocalDateTime disclosureTime) {
        this.disclosureTime = disclosureTime;
    }

    public LocalDateTime getConfirmTime() {
        return confirmTime;
    }

    public void setConfirmTime(LocalDateTime confirmTime) {
        this.confirmTime = confirmTime;
    }
}