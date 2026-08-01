package com.wh.approval.pm;

import com.wh.approval.ApprovalCallbackRegistry;
import com.wh.approval.ApprovalCompletedCallback;
import com.wh.approval.PmWbsModifyApprovalCallback;
import com.wh.bo.pm.WbsApprovalCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.verify;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("PmWbsModifyApprovalCallback 测试")
class PmWbsModifyApprovalCallbackTest {

    @Autowired
    private PmWbsModifyApprovalCallback callback;

    @Autowired
    private ApprovalCallbackRegistry registry;

    @MockBean
    private WbsApprovalCallback wbsApprovalCallback;

    private static final String BIZ_ID = "wbs_001";
    private static final String FLOW_CODE = "PM_WBS_MODIFY_APPROVAL";

    @Nested
    @DisplayName("Bean 与注册")
    class BeanRegistration {

        @Test
        @DisplayName("getFlowCode 应返回 PM_WBS_MODIFY_APPROVAL")
        void flowCode_shouldBeCorrect() {
            assertThat(callback.getFlowCode()).isEqualTo(FLOW_CODE);
        }

        @Test
        @DisplayName("应通过 ApprovalCallbackRegistry 可获取")
        void shouldBeRegisteredInRegistry() {
            ApprovalCompletedCallback retrieved = registry.getCallback(FLOW_CODE);
            assertThat(retrieved).isNotNull().isInstanceOf(PmWbsModifyApprovalCallback.class);
        }
    }

    @Nested
    @DisplayName("onApproved 方法")
    class OnApproved {

        @Test
        @DisplayName("审批通过时委托 applyChanges")
        void onApproved_delegatesToApplyChanges() {
            callback.onApproved(BIZ_ID, Map.of("comment", "WBS 变更通过"));
            verify(wbsApprovalCallback).applyChanges(BIZ_ID);
        }

        @Test
        @DisplayName("空参数审批通过不应抛出异常")
        void onApproved_withEmptyParams_shouldNotThrow() {
            assertDoesNotThrow(() -> callback.onApproved(BIZ_ID, Map.of()));
            verify(wbsApprovalCallback).applyChanges(BIZ_ID);
        }
    }

    @Nested
    @DisplayName("onRejected 方法")
    class OnRejected {

        @Test
        @DisplayName("审批驳回时委托 discardChanges")
        void onRejected_delegatesToDiscardChanges() {
            callback.onRejected(BIZ_ID, "不符合要求");
            verify(wbsApprovalCallback).discardChanges(BIZ_ID, "不符合要求");
        }
    }
}
