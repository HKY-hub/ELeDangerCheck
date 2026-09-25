package org.example.eledangercheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

import java.time.LocalDateTime;

@TableName("disclosure_sign")
public class DisclosureSign {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "disclosure_id")
    private Long disclosureId;

    @TableField(value = "user_id")
    private Long userId;

    @TableField(value = "sign_image")
    private String signImage;

    @TableField(value = "sign_time")
    private LocalDateTime signTime;

    @TableField(value = "sign_status")
    private String signStatus;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getDisclosureId() {
        return disclosureId;
    }

    public void setDisclosureId(Long disclosureId) {
        this.disclosureId = disclosureId;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getSignImage() {
        return signImage;
    }

    public void setSignImage(String signImage) {
        this.signImage = signImage;
    }

    public LocalDateTime getSignTime() {
        return signTime;
    }

    public void setSignTime(LocalDateTime signTime) {
        this.signTime = signTime;
    }

    public String getSignStatus() {
        return signStatus;
    }

    public void setSignStatus(String signStatus) {
        this.signStatus = signStatus;
    }
}