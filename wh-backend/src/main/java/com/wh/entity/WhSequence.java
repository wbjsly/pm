package com.wh.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.io.Serializable;

@Data
@TableName("wh_sequence")
public class WhSequence implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId("SEQ_NAME")
    private String seqName;

    @TableField("SEQ_VALUE")
    private Integer seqValue;

    @TableField("SEQ_PREFIX")
    private String seqPrefix;

    @TableField("SEQ_YEAR")
    private Integer seqYear;
}
