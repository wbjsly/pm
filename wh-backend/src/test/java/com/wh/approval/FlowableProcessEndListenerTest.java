package com.wh.approval;

import org.flowable.engine.delegate.DelegateExecution;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("FlowableProcessEndListener 测试")
class FlowableProcessEndListenerTest {

    @Mock private ApprovalCallbackRegistry registry;
    @Mock private DelegateExecution execution;
    @Mock private ApprovalCompletedCallback callback;

    private FlowableProcessEndListener listener;

    @BeforeEach
    void setUp() {
        listener = new FlowableProcessEndListener(registry);
    }

    @Test
    @DisplayName("未注册回调的流程编码 - 静默跳过")
    void unknownFlowCode_skips() {
        when(execution.getVariable("flowCode")).thenReturn("UNKNOWN_FLOW");
        when(execution.getVariable("bizId")).thenReturn("biz-1");
        when(execution.getVariable("approvalResult")).thenReturn("APPROVED");
        when(registry.getCallback("UNKNOWN_FLOW")).thenReturn(null);

        listener.notify(execution);

        verify(callback, never()).onApproved(anyString(), any());
    }

    @Test
    @DisplayName("APPROVED - 调用 onApproved 回调")
    void approved_callsCallback() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("flowCode")).thenReturn("PM_CHARTER_APPROVAL");
        when(execution.getVariable("bizId")).thenReturn("biz-1");
        when(execution.getVariable("approvalResult")).thenReturn("APPROVED");
        when(registry.getCallback("PM_CHARTER_APPROVAL")).thenReturn(callback);

        listener.notify(execution);

        verify(callback).onApproved(eq("biz-1"), argThat(p -> "proc-1".equals(p.get("processInstanceId"))));
    }

    @Test
    @DisplayName("REJECTED - 调用 onRejected 回调并携带原因")
    void rejected_callsCallback() {
        when(execution.getProcessInstanceId()).thenReturn("proc-1");
        when(execution.getVariable("flowCode")).thenReturn("PM_CHARTER_APPROVAL");
        when(execution.getVariable("bizId")).thenReturn("biz-1");
        when(execution.getVariable("approvalResult")).thenReturn("REJECTED");
        when(execution.getVariable("rejectReason")).thenReturn("不符合要求");
        when(registry.getCallback("PM_CHARTER_APPROVAL")).thenReturn(callback);

        listener.notify(execution);

        verify(callback).onRejected("biz-1", "不符合要求");
    }

    @Test
    @DisplayName("REJECTED 但无驳回原因 - 传入 null")
    void rejected_withoutReason() {
        when(execution.getVariable("flowCode")).thenReturn("PM_CHARTER_APPROVAL");
        when(execution.getVariable("bizId")).thenReturn("biz-1");
        when(execution.getVariable("approvalResult")).thenReturn("REJECTED");
        when(execution.getVariable("rejectReason")).thenReturn(null);
        when(registry.getCallback("PM_CHARTER_APPROVAL")).thenReturn(callback);

        listener.notify(execution);

        verify(callback).onRejected("biz-1", null);
    }

    @Test
    @DisplayName("审批结果为空 - 不调用任何回调")
    void nullResult_skips() {
        when(execution.getVariable("flowCode")).thenReturn("PM_CHARTER_APPROVAL");
        when(execution.getVariable("bizId")).thenReturn("biz-1");
        when(execution.getVariable("approvalResult")).thenReturn(null);
        when(registry.getCallback("PM_CHARTER_APPROVAL")).thenReturn(callback);

        listener.notify(execution);

        verify(callback, never()).onApproved(anyString(), any());
        verify(callback, never()).onRejected(anyString(), any());
    }
}
