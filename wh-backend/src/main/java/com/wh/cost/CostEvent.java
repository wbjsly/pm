package com.wh.cost;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class CostEvent {

    public enum SourceSystem {
        TIMESHEET, PROCUREMENT, REIMBURSEMENT, MANUAL
    }

    private String projectId;
    private String budgetItemId;
    private BigDecimal amount;
    private LocalDate costDate;
    private SourceSystem sourceSystem;
    private String sourceRef;
    private String sourceId;
}
