package com.wh.bo.pm;

import lombok.Data;

@Data
public class DeliverableCreateRequest {
    private String name;
    private String description;
    private String plannedDeliveryDate;
    private String projectId;
    private String remarks;
}
