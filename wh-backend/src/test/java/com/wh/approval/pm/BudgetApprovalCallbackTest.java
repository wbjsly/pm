package com.wh.approval.pm;

import com.wh.approval.ApprovalCallbackRegistry;
import com.wh.approval.ApprovalCompletedCallback;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("BudgetApprovalCallback 测试")
class BudgetApprovalCallbackTest {

    @Autowired
    private BudgetApprovalCallback callback;

    @Autowired
    private ApprovalCallbackRegistry registry;

    private static final String BIZ_ID = "budget_001";
    private static final String FLOW_CODE = "PM_BUDGET_APPROVAL";

    @Nested
    @DisplayName("Bean 与注册")
    class BeanRegistration {

        @Test
        @DisplayName("Spring 应成功创建 BudgetApprovalCallback bean")
        void bean_shouldBeCreated() {
            assertThat(callback).isNotNull();
        }

        @Test
        @DisplayName("getFlowCode 应返回 PM_BUDGET_APPROVAL")
        void flowCode_shouldBeCorrect() {
            assertThat(callback.getFlowCode()).isEqualTo(FLOW_CODE);
        }

        @Test
        @DisplayName("应通过 ApprovalCallbackRegistry 可获取")
        void shouldBeRegisteredInRegistry() {
            ApprovalCompletedCallback retrieved = registry.getCallback(FLOW_CODE);
            assertThat(retrieved).isNotNull().isInstanceOf(BudgetApprovalCallback.class);
        }
    }

    @Nested
    @DisplayName("onApproved 方法")
    class OnApproved {

        @Test
        @DisplayName("审批通过回调不应抛出异常")
        void onApproved_shouldNotThrow() {
            assertDoesNotThrow(() ->
                    callback.onApproved(BIZ_ID, Map.of("comment", "预算审批通过")));
        }

        @Test
        @DisplayName("空参数审批通过不应抛出异常")
        void onApproved_withEmptyParams_shouldNotThrow() {
            assertDoesNotThrow(() ->
                    callback.onApproved(BIZ_ID, Map.of()));
        }
    }

    @Nested
    @DisplayName("onRejected 方法")
    class OnRejected {

        @Test
        @DisplayName("审批驳回回调不应抛出异常")
        void onRejected_shouldNotThrow() {
            assertDoesNotThrow(() ->
                    callback.onRejected(BIZ_ID, "预算超支"));
        }

        @Test
        @DisplayName("空驳回原因不应抛出异常")
        void onRejected_withEmptyReason_shouldNotThrow() {
            assertDoesNotThrow(() ->
                    callback.onRejected(BIZ_ID, ""));
        }
    }
}
