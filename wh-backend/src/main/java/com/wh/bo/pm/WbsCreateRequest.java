package com.wh.bo.pm;

import lombok.Data;

@Data
public class WbsCreateRequest {
    private String projectId;
    private String parentId;
    private String name;
    private String description;
    private String elementType;
    private String effortEstimate;
    private String budgetEstimate;
    private String ownerId;
    private String plannedOwnerId;
    private String productId;
    private String moduleId;
    private String priority;
    private String techDifficulty;
    private String plannedStartDate;
    private String plannedEndDate;
    private String remarks;
    private Integer sortOrder;
}
