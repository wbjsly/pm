package com.wh.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_dict_type")
public class WhDictType extends BaseEntity {

    @TableField("TYPE_CODE")
    private String typeCode;

    @TableField("TYPE_NAME")
    private String typeName;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @TableField("STATUS")
    private String status;
}
