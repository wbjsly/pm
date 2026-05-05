package com.wh.bo.pm;

import lombok.Data;

@Data
public class WorkLogUpdateRequest {
    private String id;
    private String projectId;
    private String logDate;
    private String hoursWorked;
    private String workDescription;
}
