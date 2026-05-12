package com.wh.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_dict_item")
public class WhDictItem extends BaseEntity {

    @TableField("TYPE_CODE")
    private String typeCode;

    @TableField("ITEM_CODE")
    private String itemCode;

    @TableField("ITEM_LABEL")
    private String itemLabel;

    @TableField("ITEM_VALUE")
    private String itemValue;

    @TableField("TAG_TYPE")
    private String tagType;

    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @TableField("STATUS")
    private String status;
}
