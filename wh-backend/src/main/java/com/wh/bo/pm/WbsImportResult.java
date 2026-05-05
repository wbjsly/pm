package com.wh.bo.pm;

import lombok.Data;

import java.util.List;

@Data
public class WbsImportResult {
    private int total;
    private int success;
    private int degraded;
    private int failed;
    private List<ImportDetail> details;

    @Data
    public static class ImportDetail {
        private int row;
        private String name;
        private String status; // SUCCESS, DEGRADED, FAILED
        private String reason;
    }
}
