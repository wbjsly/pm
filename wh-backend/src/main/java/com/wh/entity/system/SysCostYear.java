package com.wh.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_sys_cost_year")
public class SysCostYear extends BaseEntity {
    private String name;
    private String startDate;
    private String endDate;
}
