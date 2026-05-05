package com.wh.vo.pm;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WorkHoursStatsVO {
    private int workDays;
    private int filledDays;
    private int unfilledDays;
    private int partialDays;
    private BigDecimal gapHours;
    private BigDecimal actualHours;
    private BigDecimal monthTarget;
}
