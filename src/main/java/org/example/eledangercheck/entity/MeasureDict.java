package org.example.eledangercheck.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("measure_dict")
public class MeasureDict {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField(value = "dict_code")
    private String dictCode;

    @TableField(value = "dict_name")
    private String dictName;

    @TableField(value = "dict_desc")
    private String dictDesc;

    @TableField(value = "hazard_code")
    private String hazardCode;

    @TableField(value = "priority")
    private Integer priority;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getDictCode() {
        return dictCode;
    }

    public void setDictCode(String dictCode) {
        this.dictCode = dictCode;
    }

    public String getDictName() {
        return dictName;
    }

    public void setDictName(String dictName) {
        this.dictName = dictName;
    }

    public String getDictDesc() {
        return dictDesc;
    }

    public void setDictDesc(String dictDesc) {
        this.dictDesc = dictDesc;
    }

    public String getHazardCode() {
        return hazardCode;
    }

    public void setHazardCode(String hazardCode) {
        this.hazardCode = hazardCode;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }
}