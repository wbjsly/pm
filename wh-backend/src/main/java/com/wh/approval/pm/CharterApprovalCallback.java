package com.wh.approval.pm;

import com.wh.approval.ApprovalCompletedCallback;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CharterApprovalCallback implements ApprovalCompletedCallback {

    @Override
    public String getFlowCode() {
        return "PM_CHARTER_APPROVAL";
    }

    @Override
    public void onApproved(String bizId, Map<String, Object> params) {
        log.info("Charter {} approved", bizId);
    }

    @Override
    public void onRejected(String bizId, String rejectReason) {
        log.info("Charter {} rejected: {}", bizId, rejectReason);
    }
}
