package com.wh.approval;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.ExecutionListener;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@Component
public class FlowableProcessEndListener implements ExecutionListener {

    private final ApprovalCallbackRegistry callbackRegistry;

    public FlowableProcessEndListener(ApprovalCallbackRegistry callbackRegistry) {
        this.callbackRegistry = callbackRegistry;
    }

    @Override
    public void notify(DelegateExecution execution) {
        String flowCode = (String) execution.getVariable("flowCode");
        String bizId = (String) execution.getVariable("bizId");
        String approvalResult = (String) execution.getVariable("approvalResult");

        ApprovalCompletedCallback callback = callbackRegistry.getCallback(flowCode);
        if (callback == null) {
            log.warn("No callback registered for flowCode: {}", flowCode);
            return;
        }

        Map<String, Object> params = new HashMap<>();
        params.put("processInstanceId", execution.getProcessInstanceId());

        if ("APPROVED".equals(approvalResult)) {
            log.info("Process {} completed with APPROVED for bizId {}", flowCode, bizId);
            callback.onApproved(bizId, params);
        } else if ("REJECTED".equals(approvalResult)) {
            String rejectReason = (String) execution.getVariable("rejectReason");
            log.info("Process {} completed with REJECTED for bizId {}", flowCode, bizId);
            callback.onRejected(bizId, rejectReason);
        }
    }
}
