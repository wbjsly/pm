package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_product")
public class ErpProduct extends BaseEntity {

    @TableField("PRODUCT_CODE")
    private String productCode;

    @TableField("PRODUCT_NAME")
    private String productName;

    @TableField("STATUS")
    private String status;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("PRODUCT_VERSION")
    private String productVersion;

    @TableField("VERSION_RELEASE_DATE")
    private String versionReleaseDate;

    @TableField(exist = false)
    private Integer moduleCount;
}
