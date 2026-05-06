package com.wh.bo.pm;

import lombok.Data;

@Data
public class CharterUpdateRequest {
    private String id;
    private String projectName;
    private String projectCode;
    private String projectShortName;
    private String description;
    private String objectives;
    private String scopeSummary;
    private String sponsorId;
    private String pmId;
    private String budgetCap;
    private String startDate;
    private String endDate;
    private String keyStakeholders;
    private String remarks;
    private String contractNo;
    private String projectCategory;
    private String outputValueTaxable;
    private String outputValueExcludingTax;
    private String taxRate;
    private String taxAmount;
    private String progress;
}
