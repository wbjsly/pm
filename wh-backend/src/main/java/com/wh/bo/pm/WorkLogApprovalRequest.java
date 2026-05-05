package com.wh.bo.pm;

import lombok.Data;

import java.util.List;

@Data
public class WorkLogApprovalRequest {
    private List<String> ids;
    private String reason;
}
