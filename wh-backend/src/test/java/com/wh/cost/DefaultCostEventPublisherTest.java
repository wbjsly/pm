package com.wh.cost;

import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.entity.pm.WhPmActualCost;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("DefaultCostEventPublisher 测试")
class DefaultCostEventPublisherTest {

    @Autowired
    private DefaultCostEventPublisher publisher;

    @Autowired
    private WhPmActualCostDao actualCostDao;

    // ══════════════════════════════════════════════════
    //  正常路径
    // ══════════════════════════════════════════════════

    @Test
    @DisplayName("publish - 所有字段正常，保存成功")
    void publish_allFieldsSet_savesCorrectly() {
        CostEvent event = new CostEvent();
        event.setProjectId("proj-001");
        event.setBudgetItemId("item-001");
        event.setAmount(new BigDecimal("1500.50"));
        event.setCostDate(LocalDate.of(2026, 6, 15));
        event.setSourceSystem(CostEvent.SourceSystem.MANUAL);
        event.setSourceRef("测试手动录入");
        event.setSourceId("src-001");

        publisher.publish(event);

        // 验证数据库记录
        WhPmActualCost cost = actualCostDao.selectList(null).stream()
                .filter(c -> "proj-001".equals(c.getProjectId()))
                .findFirst().orElse(null);
        assertNotNull(cost, "应成功插入成本记录");
        assertEquals("item-001", cost.getBudgetItemId());
        assertEquals("1500.50", cost.getAmount());
        assertEquals("2026-06-15", cost.getCostDate());
        assertEquals("manual", cost.getSourceSystem());
        assertEquals("测试手动录入", cost.getDescription());
        assertEquals("src-001", cost.getSourceId());
        assertEquals("", cost.getCostType(), "costType 应为空字符串");
    }

    // ══════════════════════════════════════════════════
    //  null 字段回退测试
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("null 字段回退")
    class NullFieldFallback {

        @Test
        @DisplayName("costDate 为 null → 使用当前日期")
        void nullCostDate_defaultsToToday() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-null-date");
            event.setAmount(new BigDecimal("100"));
            event.setSourceSystem(CostEvent.SourceSystem.MANUAL);
            event.setSourceRef("无日期");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-null-date".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals(LocalDate.now().toString(), cost.getCostDate(),
                    "costDate 为 null 时应使用当前日期");
        }

        @Test
        @DisplayName("amount 为 null → amount 存 '0'")
        void nullAmount_defaultsToZero() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-null-amount");
            event.setSourceSystem(CostEvent.SourceSystem.MANUAL);
            event.setSourceRef("无金额");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-null-amount".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals("0", cost.getAmount(), "amount 为 null 时 amount 应存 '0'");
        }

        @Test
        @DisplayName("sourceSystem 为 null → sourceSystem 存 'manual'")
        void nullSourceSystem_defaultsToManual() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-null-source");
            event.setAmount(new BigDecimal("200"));
            event.setSourceRef("无来源系统");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-null-source".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals("manual", cost.getSourceSystem(),
                    "sourceSystem 为 null 时 sourceSystem 应存 'manual'");
        }

        @Test
        @DisplayName("sourceRef 为 null → description 存 null")
        void nullSourceRef_storesNull() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-null-ref");
            event.setAmount(new BigDecimal("300"));
            event.setSourceSystem(CostEvent.SourceSystem.MANUAL);

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-null-ref".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertNull(cost.getDescription(), "sourceRef 为 null 时 description 应存 null");
        }
    }

    // ══════════════════════════════════════════════════
    //  SourceSystem 枚举映射测试
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("SourceSystem 枚举映射")
    class SourceSystemMapping {

        @Test
        @DisplayName("TIMESHEET → 'timesheet'")
        void timesheet_mapsToLowercase() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-ts");
            event.setAmount(new BigDecimal("100"));
            event.setCostDate(LocalDate.of(2026, 1, 1));
            event.setSourceSystem(CostEvent.SourceSystem.TIMESHEET);
            event.setSourceRef("工时");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-ts".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals("timesheet", cost.getSourceSystem());
        }

        @Test
        @DisplayName("PROCUREMENT → 'procurement'")
        void procurement_mapsToLowercase() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-pr");
            event.setAmount(new BigDecimal("200"));
            event.setCostDate(LocalDate.of(2026, 1, 1));
            event.setSourceSystem(CostEvent.SourceSystem.PROCUREMENT);
            event.setSourceRef("采购");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-pr".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals("procurement", cost.getSourceSystem());
        }

        @Test
        @DisplayName("REIMBURSEMENT → 'reimbursement'")
        void reimbursement_mapsToLowercase() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-re");
            event.setAmount(new BigDecimal("300"));
            event.setCostDate(LocalDate.of(2026, 1, 1));
            event.setSourceSystem(CostEvent.SourceSystem.REIMBURSEMENT);
            event.setSourceRef("报销");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-re".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals("reimbursement", cost.getSourceSystem());
        }

        @Test
        @DisplayName("MANUAL → 'manual'")
        void manual_mapsToLowercase() {
            CostEvent event = new CostEvent();
            event.setProjectId("proj-man");
            event.setAmount(new BigDecimal("400"));
            event.setCostDate(LocalDate.of(2026, 1, 1));
            event.setSourceSystem(CostEvent.SourceSystem.MANUAL);
            event.setSourceRef("手动");

            publisher.publish(event);

            WhPmActualCost cost = actualCostDao.selectList(null).stream()
                    .filter(c -> "proj-man".equals(c.getProjectId()))
                    .findFirst().orElse(null);
            assertNotNull(cost);
            assertEquals("manual", cost.getSourceSystem());
        }
    }
}
