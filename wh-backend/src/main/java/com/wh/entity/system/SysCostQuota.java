package com.wh.entity.system;

import com.baomidou.mybatisplus.annotation.TableName;
import com.wh.entity.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wh_sys_cost_quota")
public class SysCostQuota extends BaseEntity {
    private String positionId;
    private String yearId;
    private Integer versionNo;
    private BigDecimal dailyRate;
    private String effectiveDate;
    private String changeReason;
}
