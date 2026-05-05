package com.wh.approval;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class ApprovalCallbackRegistry {

    private final Map<String, ApprovalCompletedCallback> callbacks = new HashMap<>();

    @Autowired
    public ApprovalCallbackRegistry(Map<String, ApprovalCompletedCallback> callbackMap) {
        for (ApprovalCompletedCallback callback : callbackMap.values()) {
            callbacks.put(callback.getFlowCode(), callback);
        }
    }

    public ApprovalCompletedCallback getCallback(String flowCode) {
        return callbacks.get(flowCode);
    }
}
