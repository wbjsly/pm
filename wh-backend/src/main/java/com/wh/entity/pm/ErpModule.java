package com.wh.entity.pm;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("erp_module")
public class ErpModule extends BaseEntity {

    @TableField("PRODUCT_ID")
    private String productId;

    @TableField("MODULE_CODE")
    private String moduleCode;

    @TableField("MODULE_NAME")
    private String moduleName;

    @TableField("DESCRIPTION")
    private String description;

    @TableField("STATUS")
    private String status;

    @TableField("MODULE_VERSION")
    private String moduleVersion;

    @TableField("VERSION_RELEASE_DATE")
    private String versionReleaseDate;
}
