package com.wh.vo.pm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetItemVO {

    private String id;

    private String category;

    private Integer level;

    private String name;

    private BigDecimal budgetAmount;

    private BigDecimal actualAmount;

    private BigDecimal ratio;

    // Labor detail
    private String roleCode;

    private String hours;

    private BigDecimal costRate;

    // Procurement detail
    private String bomItem;

    private BigDecimal qty;

    private BigDecimal unitPrice;

    private List<BudgetItemVO> children;
}
