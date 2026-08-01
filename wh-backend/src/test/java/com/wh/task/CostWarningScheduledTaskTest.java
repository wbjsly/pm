package com.wh.task;

import com.wh.bo.pm.WhPmCostWarningBo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("CostWarningScheduledTask 测试")
class CostWarningScheduledTaskTest {

    @Mock
    private WhPmCostWarningBo costWarningBo;

    @InjectMocks
    private CostWarningScheduledTask task;

    @Nested
    @DisplayName("executeCostWarningCalculation")
    class Execute {

        @Test
        @DisplayName("正常执行时调用 triggerCalculation")
        void normalExecution_callsTriggerCalculation() {
            task.executeCostWarningCalculation();
            verify(costWarningBo).triggerCalculation();
        }

        @Test
        @DisplayName("triggerCalculation 抛出异常时被吞掉，不向上传播")
        void exception_isSwallowed() {
            doThrow(new RuntimeException("计算失败"))
                    .when(costWarningBo).triggerCalculation();
            assertDoesNotThrow(() -> task.executeCostWarningCalculation());
        }
    }
}
