package com.wh.bo.pm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetItemRequest {

    private String id;

    private String category;

    private String parentId;

    private String amount;

    private Integer level;

    private Integer sortOrder;

    // Labor detail
    private String roleCode;

    private String hours;

    private String costRate;

    // Procurement detail
    private String bomItem;

    private String qty;

    private String unitPrice;

    // Other detail
    private String description;

    // Children for tree structure
    private List<BudgetItemRequest> children;
}
