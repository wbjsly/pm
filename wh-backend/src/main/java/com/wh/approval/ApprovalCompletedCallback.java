package com.wh.approval;

import java.util.Map;

public interface ApprovalCompletedCallback {
    String getFlowCode();
    void onApproved(String bizId, Map<String, Object> params);
    void onRejected(String bizId, String rejectReason);
}
