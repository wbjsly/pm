package com.wh.bo.pm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetUpdateRequest {

    private String managementReserve;

    private List<BudgetItemRequest> items;
}
