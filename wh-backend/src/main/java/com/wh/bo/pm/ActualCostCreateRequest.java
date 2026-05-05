package com.wh.bo.pm;

import lombok.Data;

@Data
public class ActualCostCreateRequest {

    private String projectId;

    private String budgetItemId;

    private String activityId;

    private String costDate;

    private String costType;

    private String amount;

    private String description;

    private String sourceRef;
}
