package com.wh;

import com.wh.bo.pm.BudgetCreateRequest;
import com.wh.bo.pm.WhPmBudgetBo;
import com.wh.bo.pm.WhPmCostWarningBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.dao.pm.WhPmCostWarningDao;
import com.wh.entity.pm.WhPmActualCost;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmCostWarning;
import com.wh.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("成本预警 BO 测试")
class WhPmCostWarningBoTest {

    @Autowired
    private WhPmCostWarningBo costWarningBo;

    @Autowired
    private WhPmBudgetBo budgetBo;

    @Autowired
    private WhPmBudgetDao budgetDao;

    @Autowired
    private WhPmActualCostDao actualCostDao;

    @Autowired
    private WhPmCostWarningDao costWarningDao;

    @Autowired
    private TestFixtures fixtures;

    private String projectId;

    @BeforeEach
    void setUp() {
        WhPmCharter charter = fixtures.createTestProject("成本预警测试");
        projectId = charter.getId();
    }

    // ══════════════════════════════════════════════════
    //  查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("查询预警")
    class ListTests {

        @Test
        @DisplayName("listByProjectId - 无条件返回全部")
        void testListByProjectId_NoFilter() {
            // 没有预警数据时返回空
            List<WhPmCostWarning> result = costWarningBo.listByProjectId(null, null);
            assertTrue(result.isEmpty());
        }

        @Test
        @DisplayName("listByProjectId - 按项目过滤")
        void testListByProjectId_ByProject() {
            insertWarning("WARN", "0.90");

            List<WhPmCostWarning> matched = costWarningBo.listByProjectId(projectId, null);
            assertEquals(1, matched.size());

            List<WhPmCostWarning> empty = costWarningBo.listByProjectId("other-project", null);
            assertTrue(empty.isEmpty());
        }

        @Test
        @DisplayName("listByProjectId - 按状态过滤")
        void testListByProjectId_ByStatus() {
            insertWarning("WARN", "0.90");

            List<WhPmCostWarning> active = costWarningBo.listByProjectId(projectId, "ACTIVE");
            assertEquals(1, active.size());

            List<WhPmCostWarning> closed = costWarningBo.listByProjectId(projectId, "CLOSED");
            assertTrue(closed.isEmpty());
        }

        @Test
        @DisplayName("listActiveAll - 返回限制条数的活跃预警")
        void testListActiveAll() {
            insertWarning("WARN", "0.90");
            insertWarning("INFO", "0.85");

            List<WhPmCostWarning> result = costWarningBo.listActiveAll(10);
            assertFalse(result.isEmpty());
            assertEquals(2, result.size());
        }

        @Test
        @DisplayName("listActiveAll - 不返回已关闭的预警")
        void testListActiveAll_ExcludesClosed() {
            WhPmCostWarning active = insertWarning("WARN", "0.90");
            // 手动关闭一条
            active.setStatus("CLOSED");
            costWarningDao.updateById(active);

            insertWarning("INFO", "0.85");

            List<WhPmCostWarning> result = costWarningBo.listActiveAll(10);
            // 应只返回 1 条活跃预警
            assertTrue(result.stream().allMatch(w -> "ACTIVE".equals(w.getStatus())));
        }
    }

    // ══════════════════════════════════════════════════
    //  关闭预警
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("关闭预警")
    class CloseTests {

        @Test
        @DisplayName("关闭活跃预警 - 成功")
        void testCloseWarning_Success() {
            WhPmCostWarning warning = insertWarning("WARN", "0.90");

            costWarningBo.closeWarning(warning.getId(), "test_user");

            WhPmCostWarning updated = costWarningDao.selectById(warning.getId());
            assertEquals("CLOSED", updated.getStatus());
            assertEquals("test_user", updated.getClosedBy());
            assertNotNull(updated.getClosedAt());
        }

        @Test
        @DisplayName("关闭不存在的预警 - 抛出 404")
        void testCloseWarning_NotFound() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> costWarningBo.closeWarning("non-existent-id", "test_user"));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("关闭已关闭的预警 - 抛出异常")
        void testCloseWarning_AlreadyClosed() {
            WhPmCostWarning warning = insertWarning("WARN", "0.90");
            costWarningBo.closeWarning(warning.getId(), "user_a");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> costWarningBo.closeWarning(warning.getId(), "user_b"));
            assertTrue(ex.getMessage().contains("已关闭"));
        }
    }

    // ══════════════════════════════════════════════════
    //  触发计算
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("预警计算")
    class TriggerCalculationTests {

        @Test
        @DisplayName("实际成本超过 INFO 阈值 - 创建 INFO 预警")
        void testTriggerCalculation_CreatesInfoWarning() {
            String budgetId = createApprovedBudget("1000");

            // 实际成本 850，ratio=0.85 ∈ [0.80, 0.95) → INFO
            insertActualCost(budgetId, "850");

            costWarningBo.triggerCalculation();

            List<WhPmCostWarning> warnings = costWarningBo.listByProjectId(projectId, null);
            assertEquals(1, warnings.size());
            assertEquals("ACTIVE", warnings.get(0).getStatus());
            assertEquals("INFO", warnings.get(0).getLevel());
        }

        @Test
        @DisplayName("实际成本超过 WARN 阈值 - 创建 WARN 预警")
        void testTriggerCalculation_CreatesWarnWarning() {
            String budgetId = createApprovedBudget("1000");

            // 实际成本 960，ratio=0.96 ∈ [0.95, 1.00) → WARN
            insertActualCost(budgetId, "960");

            costWarningBo.triggerCalculation();

            List<WhPmCostWarning> warnings = costWarningBo.listByProjectId(projectId, "ACTIVE");
            assertFalse(warnings.isEmpty());
            assertEquals("WARN", warnings.get(0).getLevel());
        }

        @Test
        @DisplayName("实际成本超过 CRITICAL 阈值 - 创建 CRITICAL 预警")
        void testTriggerCalculation_CreatesCriticalWarning() {
            String budgetId = createApprovedBudget("1000");

            // 实际成本 1000，ratio=1.00 → CRITICAL
            insertActualCost(budgetId, "1000");

            costWarningBo.triggerCalculation();

            List<WhPmCostWarning> warnings = costWarningBo.listByProjectId(projectId, "ACTIVE");
            assertFalse(warnings.isEmpty());
            assertEquals("CRITICAL", warnings.get(0).getLevel());
        }

        @Test
        @DisplayName("实际成本低于阈值 - 不创建预警")
        void testTriggerCalculation_BelowThreshold_NoWarning() {
            String budgetId = createApprovedBudget("1000");

            // 实际成本 500，ratio=0.50 < 0.80 → 无预警
            insertActualCost(budgetId, "500");

            costWarningBo.triggerCalculation();

            List<WhPmCostWarning> warnings = costWarningBo.listByProjectId(projectId, null);
            assertTrue(warnings.isEmpty());
        }

        @Test
        @DisplayName("无 APPROVED 预算 - 不创建预警")
        void testTriggerCalculation_NoApprovedBudget_NoWarning() {
            // 创建 DRAFT 预算（非 APPROVED）
            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(projectId);
            WhPmBudget budget = budgetBo.create(req);

            insertActualCost(budget.getId(), "900");

            costWarningBo.triggerCalculation();

            List<WhPmCostWarning> warnings = costWarningBo.listByProjectId(projectId, null);
            assertTrue(warnings.isEmpty());
        }

        @Test
        @DisplayName("重复触发 - 关闭低级别预警并创建高级别预警")
        void testTriggerCalculation_UpgradeLevel() {
            String budgetId = createApprovedBudget("1000");
            insertActualCost(budgetId, "850");

            // 第一次触发：INFO
            costWarningBo.triggerCalculation();
            assertEquals(1, costWarningBo.listByProjectId(projectId, "ACTIVE").size());

            // 增加成本到 960，ratio=0.96 → WARN
            insertActualCost(budgetId, "110"); // 850 + 110 = 960
            costWarningBo.triggerCalculation();

            List<WhPmCostWarning> active = costWarningBo.listByProjectId(projectId, "ACTIVE");
            assertEquals(1, active.size());
            assertEquals("WARN", active.get(0).getLevel());
        }
    }

    // ══════════════════════════════════════════════════
    //  Helper
    // ══════════════════════════════════════════════════

    private WhPmCostWarning insertWarning(String level, String ratio) {
        WhPmCostWarning w = new WhPmCostWarning();
        w.setProjectId(projectId);
        w.setBudgetId("dummy-budget-id");
        w.setLevel(level);
        w.setRatio(ratio);
        w.setStatus("ACTIVE");
        w.setTriggeredAt(java.time.LocalDateTime.now().toString());
        costWarningDao.insert(w);
        return w;
    }

    /** 创建 APPROVED 预算并返回 budgetId */
    private String createApprovedBudget(String totalBudget) {
        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setProjectId(projectId);
        req.setManagementReserve(totalBudget);
        WhPmBudget budget = budgetBo.create(req);

        // 直接通过 DAO 设为 APPROVED（绕过 Flowable 审批流程）
        budget.setStatus("APPROVED");
        budgetDao.updateById(budget);
        return budget.getId();
    }

    private void insertActualCost(String budgetId, String amount) {
        WhPmActualCost cost = new WhPmActualCost();
        cost.setProjectId(projectId);
        cost.setBudgetItemId(budgetId);
        cost.setAmount(amount);
        cost.setCostDate("2026-01-15");
        cost.setCostType("LABOR");
        cost.setDescription("测试实际成本");
        actualCostDao.insert(cost);
    }
}
