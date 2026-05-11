package com.wh.vo.pm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetComparisonVO {

    private String budgetId;

    private String version;

    private String projectId;

    private String projectName;

    private BigDecimal directBudget;

    private BigDecimal totalBudget;

    private BigDecimal managementReserve;

    private BigDecimal totalActual;

    private BigDecimal totalRatio;

    private List<BudgetItemVO> items;
}
