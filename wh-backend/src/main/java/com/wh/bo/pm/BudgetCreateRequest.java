package com.wh.bo.pm;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class BudgetCreateRequest {

    private String projectId;

    private String managementReserve;

    private List<BudgetItemRequest> items;
}
