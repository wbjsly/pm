package com.wh.entity.system;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_menu")
public class SysMenu extends BaseEntity {

    @TableField("PARENT_ID")
    private String parentId;

    @TableField("TITLE")
    private String title;

    @TableField("PATH")
    private String path;

    @TableField("COMPONENT")
    private String component;

    @TableField("ICON")
    private String icon;

    @TableField("SORT_ORDER")
    private Integer sortOrder;

    @TableField("PERM")
    private String perm;

    @TableField("STATUS")
    private String status;
}
