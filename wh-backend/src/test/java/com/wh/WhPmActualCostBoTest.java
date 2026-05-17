package com.wh;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.ActualCostCreateRequest;
import com.wh.bo.pm.BudgetCreateRequest;
import com.wh.bo.pm.BudgetItemRequest;
import com.wh.bo.pm.CharterCreateRequest;
import com.wh.bo.pm.WhPmActualCostBo;
import com.wh.bo.pm.WhPmBudgetBo;
import com.wh.bo.pm.WhPmCharterBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.entity.pm.WhPmActualCost;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmBudgetItem;
import com.wh.fixtures.TestFixtures;
import com.wh.vo.pm.BudgetDetailVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("实际成本BO 集成测试")
class WhPmActualCostBoTest {

    @Autowired
    private WhPmActualCostBo actualCostBo;

    @Autowired
    private WhPmBudgetBo budgetBo;

    @Autowired
    private WhPmCharterBo charterBo;

    @Autowired
    private WhPmActualCostDao actualCostDao;

    @Autowired
    private TestFixtures fixtures;

    private String projectId;
    private String laborItemId;
    private String travelItemId;
    private String procurementItemId;

    @BeforeEach
    void setUp() {
        // Create an approved project with budget items
        WhPmCharter project = fixtures.createTestProject("实际成本测试-" + UUID.randomUUID().toString().substring(0, 6));
        projectId = project.getId();

        // Create budget with items
        BudgetCreateRequest budgetReq = new BudgetCreateRequest();
        budgetReq.setProjectId(projectId);

        BudgetItemRequest labor = new BudgetItemRequest();
        labor.setCategory("LABOR");
        labor.setAmount("160000");
        labor.setLevel(1);
        labor.setRoleCode("DEV");
        labor.setHours("1600");
        labor.setCostRate("100");

        BudgetItemRequest travel = new BudgetItemRequest();
        travel.setCategory("TRAVEL");
        travel.setAmount("30000");
        travel.setLevel(1);
        travel.setDescription("差旅费用");

        BudgetItemRequest procurement = new BudgetItemRequest();
        procurement.setCategory("PROCUREMENT");
        procurement.setAmount("50000");
        procurement.setLevel(1);
        procurement.setBomItem("Server");
        procurement.setQty("2");
        procurement.setUnitPrice("25000");

        budgetReq.setItems(List.of(labor, travel, procurement));
        WhPmBudget budget = budgetBo.create(budgetReq);

        // Get the budget item IDs from detail
        BudgetDetailVO detail = budgetBo.getDetailWithItems(budget.getId());
        for (var item : detail.getItems()) {
            switch (item.getCategory()) {
                case "LABOR":
                    laborItemId = item.getId();
                    break;
                case "TRAVEL":
                    travelItemId = item.getId();
                    break;
                case "PROCUREMENT":
                    procurementItemId = item.getId();
                    break;
            }
        }
    }

    // ========== helpers ==========

    private ActualCostCreateRequest createCostRequest(String budgetItemId, String costType, String amount) {
        ActualCostCreateRequest req = new ActualCostCreateRequest();
        req.setProjectId(projectId);
        req.setBudgetItemId(budgetItemId);
        req.setCostDate("2026-01-15");
        req.setCostType(costType);
        req.setAmount(amount);
        req.setDescription("测试实际成本");
        return req;
    }

    // ═══════════════════════════════════════════════════════
    //  创建实际成本
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("创建实际成本")
    class CreateTests {

        @Test
        @DisplayName("创建与预算科目匹配的实际成本")
        void testCreate_WithMatchingCostType_Success() {
            ActualCostCreateRequest req = createCostRequest(laborItemId, "LABOR", "80000");

            WhPmActualCost cost = actualCostBo.create(req);

            assertNotNull(cost.getId());
            assertEquals(laborItemId, cost.getBudgetItemId());
            assertEquals("LABOR", cost.getCostType());
            assertEquals("80000", cost.getAmount());
            assertEquals("manual", cost.getSourceSystem());
            assertEquals(projectId, cost.getProjectId());
        }

        @Test
        @DisplayName("不指定预算科目创建实际成本")
        void testCreate_WithoutBudgetItem_Success() {
            ActualCostCreateRequest req = new ActualCostCreateRequest();
            req.setProjectId(projectId);
            req.setCostDate("2026-01-15");
            req.setCostType("LABOR");
            req.setAmount("50000");
            req.setDescription("无预算科目的成本");

            WhPmActualCost cost = actualCostBo.create(req);

            assertNotNull(cost.getId());
            assertNull(cost.getBudgetItemId());
            assertEquals("manual", cost.getSourceSystem());
            assertEquals("50000", cost.getAmount());
        }

        @Test
        @DisplayName("成本类型与预算科目不匹配 - 抛出异常")
        void testCreate_CostTypeMismatch_Throws() {
            ActualCostCreateRequest req = createCostRequest(laborItemId, "TRAVEL", "80000");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> actualCostBo.create(req));
            assertTrue(ex.getMessage().contains("成本类型与预算科目不匹配"));
        }

        @Test
        @DisplayName("预算科目不存在 - 抛出异常")
        void testCreate_NonExistentBudgetItem_Throws() {
            ActualCostCreateRequest req = createCostRequest("non-existent-item-id", "LABOR", "80000");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> actualCostBo.create(req));
            assertTrue(ex.getMessage().contains("预算科目不存在"));
        }

        @Test
        @DisplayName("创建带活动ID和来源引用的实际成本")
        void testCreate_WithActivityAndSourceRef() {
            ActualCostCreateRequest req = new ActualCostCreateRequest();
            req.setProjectId(projectId);
            req.setBudgetItemId(laborItemId);
            req.setActivityId("ACT-001");
            req.setCostDate("2026-02-01");
            req.setCostType("LABOR");
            req.setAmount("30000");
            req.setDescription("活动成本");
            req.setSourceRef("REF-001");

            WhPmActualCost cost = actualCostBo.create(req);

            assertNotNull(cost.getId());
            assertEquals("ACT-001", cost.getActivityId());
            assertEquals("manual", cost.getSourceSystem());
            assertEquals("REF-001", cost.getSourceRef());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  查询实际成本
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("查询实际成本")
    class QueryTests {

        @Test
        @DisplayName("getById - 查询已存在的实际成本")
        void testGetById_Found() {
            ActualCostCreateRequest req = createCostRequest(laborItemId, "LABOR", "80000");
            WhPmActualCost created = actualCostBo.create(req);

            WhPmActualCost found = actualCostBo.getById(created.getId());

            assertNotNull(found);
            assertEquals(created.getId(), found.getId());
            assertEquals("80000", found.getAmount());
        }

        @Test
        @DisplayName("getById - 查询不存在的实际成本返回 404")
        void testGetById_NotFound_Throws404() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> actualCostBo.getById("non-existent-id"));
            assertEquals(404, ex.getCode());
            assertTrue(ex.getMessage().contains("实际成本记录不存在"));
        }

        @Test
        @DisplayName("pageList - 分页查询")
        void testPageList_All() {
            actualCostBo.create(createCostRequest(laborItemId, "LABOR", "80000"));
            actualCostBo.create(createCostRequest(travelItemId, "TRAVEL", "15000"));

            IPage<WhPmActualCost> page = actualCostBo.pageList(1, 10, null, null, null, null, null);

            assertNotNull(page);
            assertTrue(page.getTotal() >= 2);
        }

        @Test
        @DisplayName("pageList - 按项目ID和成本类型筛选")
        void testPageList_WithFilters() {
            actualCostBo.create(createCostRequest(laborItemId, "LABOR", "80000"));

            IPage<WhPmActualCost> page = actualCostBo.pageList(1, 10, projectId, null, null, "LABOR", null);

            assertEquals(1, page.getTotal());
            assertEquals("LABOR", page.getRecords().get(0).getCostType());
        }

        @Test
        @DisplayName("pageList - 按年月筛选")
        void testPageList_ByYearMonth() {
            ActualCostCreateRequest req = createCostRequest(laborItemId, "LABOR", "80000");
            req.setCostDate("2026-01-15");
            actualCostBo.create(req);

            IPage<WhPmActualCost> page = actualCostBo.pageList(1, 10, projectId, null, null, null, "2026-01");

            assertEquals(1, page.getTotal());
        }

        @Test
        @DisplayName("getMonthlyAggregation - 月度汇总")
        void testGetMonthlyAggregation() {
            actualCostBo.create(createCostRequest(laborItemId, "LABOR", "80000"));
            actualCostBo.create(createCostRequest(travelItemId, "TRAVEL", "15000"));

            List<Map<String, Object>> aggregation = actualCostBo.getMonthlyAggregation(projectId);

            assertNotNull(aggregation);
            assertFalse(aggregation.isEmpty());
            Map<String, Object> monthData = aggregation.get(0);
            assertNotNull(monthData.get("yearMonth"));
        }

        @Test
        @DisplayName("getSumByFilter - 按条件求和")
        void testGetSumByFilter() {
            actualCostBo.create(createCostRequest(laborItemId, "LABOR", "80000"));
            actualCostBo.create(createCostRequest(travelItemId, "TRAVEL", "15000"));

            Double sum = actualCostBo.getSumByFilter(projectId, "2026-01", "LABOR,TRAVEL");
            assertNotNull(sum);
            assertEquals(95000.0, sum, 0.001);
        }

        @Test
        @DisplayName("getSumByFilter - 无匹配记录的求和返回 0")
        void testGetSumByFilter_NoMatch_ReturnsZero() {
            Double sum = actualCostBo.getSumByFilter(projectId, "2026-12", "LABOR");
            assertNotNull(sum);
            assertEquals(0.0, sum, 0.001);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  删除实际成本
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("删除实际成本")
    class DeleteTests {

        @Test
        @DisplayName("删除手动录入的实际成本 - 成功")
        void testDelete_Manual_Success() {
            ActualCostCreateRequest req = createCostRequest(laborItemId, "LABOR", "80000");
            WhPmActualCost created = actualCostBo.create(req);

            actualCostBo.delete(created.getId());

            // Should no longer exist
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> actualCostBo.getById(created.getId()));
            assertTrue(ex.getMessage().contains("不存在"));
        }

        @Test
        @DisplayName("删除系统同步的实际成本 - 抛出异常")
        void testDelete_NonManual_Throws() {
            // Create a cost with non-manual source (simulate system import)
            // Since create always sets sourceSystem="manual", we modify it directly in DB for this test
            ActualCostCreateRequest req = createCostRequest(laborItemId, "LABOR", "80000");
            WhPmActualCost created = actualCostBo.create(req);

            // Override sourceSystem to simulate a non-manual entry
            WhPmActualCost entity = actualCostDao.selectById(created.getId());
            entity.setSourceSystem("erp");
            actualCostDao.updateById(entity);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> actualCostBo.delete(created.getId()));
            assertTrue(ex.getMessage().contains("只能删除手动录入"));
        }
    }
}
