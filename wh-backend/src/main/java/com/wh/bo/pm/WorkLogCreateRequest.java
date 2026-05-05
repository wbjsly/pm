package com.wh.bo.pm;

import lombok.Data;

@Data
public class WorkLogCreateRequest {
    private String projectId;
    private String logDate;
    private String hoursWorked;
    private String workDescription;
}
