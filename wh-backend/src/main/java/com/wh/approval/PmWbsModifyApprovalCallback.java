package com.wh.approval;

import com.wh.bo.pm.WbsApprovalCallback;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.entity.pm.WhPmWbsElement;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Callback registered with ApprovalCallbackRegistry for PM_WBS_MODIFY_APPROVAL flow.
 * Triggered by FlowableProcessEndListener when the process ends.
 */
@Slf4j
@Component
public class PmWbsModifyApprovalCallback implements ApprovalCompletedCallback {

    private final WbsApprovalCallback wbsApprovalCallback;

    public PmWbsModifyApprovalCallback(WbsApprovalCallback wbsApprovalCallback) {
        this.wbsApprovalCallback = wbsApprovalCallback;
    }

    @Override
    public String getFlowCode() {
        return "PM_WBS_MODIFY_APPROVAL";
    }

    @Override
    public void onApproved(String bizId, Map<String, Object> params) {
        log.info("WBS modification approved for bizId={}", bizId);
        wbsApprovalCallback.applyChanges(bizId);
    }

    @Override
    public void onRejected(String bizId, String rejectReason) {
        log.info("WBS modification rejected for bizId={}, reason={}", bizId, rejectReason);
        wbsApprovalCallback.discardChanges(bizId, rejectReason);
    }
}
