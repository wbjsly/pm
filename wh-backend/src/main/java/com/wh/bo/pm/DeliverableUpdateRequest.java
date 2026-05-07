package com.wh.bo.pm;

import lombok.Data;

@Data
public class DeliverableUpdateRequest {
    private String name;
    private String description;
    private String plannedDeliveryDate;
    private String remarks;
}
