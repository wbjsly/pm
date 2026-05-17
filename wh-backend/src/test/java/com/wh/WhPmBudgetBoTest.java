package com.wh;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.BudgetCreateRequest;
import com.wh.bo.pm.BudgetItemRequest;
import com.wh.bo.pm.BudgetUpdateRequest;
import com.wh.bo.pm.CharterCreateRequest;
import com.wh.bo.pm.WhPmBudgetBo;
import com.wh.bo.pm.WhPmCharterBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCharter;
import com.wh.fixtures.TestFixtures;
import com.wh.vo.pm.BudgetComparisonVO;
import com.wh.vo.pm.BudgetDetailVO;
import com.wh.vo.pm.BudgetVersionVO;
import com.wh.vo.pm.ProjectBudgetVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("预算BO 集成测试")
class WhPmBudgetBoTest {

    private static final String TEST_USER = "pm_001";

    @Autowired
    private WhPmBudgetBo budgetBo;

    @Autowired
    private WhPmCharterBo charterBo;

    @Autowired
    private WhPmBudgetDao budgetDao;

    @Autowired
    private WhPmCharterDao charterDao;

    @Autowired
    private TestFixtures fixtures;

    private final List<String> projectIds = new ArrayList<>();
    private final List<String> budgetIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USER, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        // Clean up budgets first (items cascade via logical delete)
        for (String id : budgetIds) {
            try {
                budgetDao.deleteById(id);
            } catch (Exception ignored) {
            }
        }
        // Clean up projects
        for (String id : projectIds) {
            try {
                charterDao.deleteById(id);
            } catch (Exception ignored) {
            }
        }
        budgetIds.clear();
        projectIds.clear();
    }

    // ========== helpers ==========

    private WhPmCharter createProject() {
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName("预算测试-" + UUID.randomUUID().toString().substring(0, 6));
        req.setProjectCode("BT-" + System.nanoTime());
        req.setProjectShortName("BT");
        req.setSponsorId(TEST_USER);
        req.setPmId(TEST_USER);
        WhPmCharter charter = charterBo.create(req);
        projectIds.add(charter.getId());
        return charter;
    }

    private WhPmCharter createApprovedProject() {
        WhPmCharter charter = createProject();
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "项目审批通过");
        return charterDao.selectById(charter.getId());
    }

    private WhPmBudget createDraftBudget(String projectId) {
        return createDraftBudget(projectId, null);
    }

    private WhPmBudget createDraftBudget(String projectId, String managementReserve) {
        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setProjectId(projectId);
        if (managementReserve != null) {
            req.setManagementReserve(managementReserve);
        }
        WhPmBudget budget = budgetBo.create(req);
        budgetIds.add(budget.getId());
        return budget;
    }

    private BudgetItemRequest createLaborItem(String amount, String roleCode, String hours, String costRate) {
        BudgetItemRequest item = new BudgetItemRequest();
        item.setCategory("LABOR");
        item.setAmount(amount);
        item.setLevel(1);
        item.setRoleCode(roleCode);
        item.setHours(hours);
        item.setCostRate(costRate);
        return item;
    }

    private BudgetItemRequest createProcurementItem(String amount, String bomItem, String qty, String unitPrice) {
        BudgetItemRequest item = new BudgetItemRequest();
        item.setCategory("PROCUREMENT");
        item.setAmount(amount);
        item.setLevel(1);
        item.setBomItem(bomItem);
        item.setQty(qty);
        item.setUnitPrice(unitPrice);
        return item;
    }

    private BudgetItemRequest createOtherItem(String category, String amount, String description) {
        BudgetItemRequest item = new BudgetItemRequest();
        item.setCategory(category);
        item.setAmount(amount);
        item.setLevel(1);
        item.setDescription(description);
        return item;
    }

    // ═══════════════════════════════════════════════════════
    //  创建预算
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("创建预算")
    class CreateTests {

        @Test
        @DisplayName("创建空预算 - 默认 DRAFT、v0.5，无费用项")
        void testCreate_Basic() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            assertNotNull(budget.getId());
            assertEquals("DRAFT", budget.getStatus());
            assertEquals("v0.5", budget.getVersion());
            assertEquals("0", budget.getCostBaseline());
            assertEquals("0", budget.getTotalBudget());
            assertNotNull(budget.getBudgetCode());
            assertTrue(budget.getBudgetCode().startsWith("BUDGET-"));
        }

        @Test
        @DisplayName("创建含管理储备的预算")
        void testCreate_WithManagementReserve() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId(), "50000");

            assertEquals("50000", budget.getManagementReserve());
            assertEquals("50000", budget.getTotalBudget());
        }

        @Test
        @DisplayName("创建含人工费用项的预算")
        void testCreate_WithLaborItem() {
            WhPmCharter project = createProject();
            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(project.getId());
            req.setManagementReserve("10000");
            req.setItems(List.of(
                    createLaborItem("160000", "DEV", "1600", "100")
            ));

            WhPmBudget budget = budgetBo.create(req);
            budgetIds.add(budget.getId());

            assertEquals("DRAFT", budget.getStatus());
            assertEquals("160000", budget.getCostBaseline());
            assertEquals("170000", budget.getTotalBudget());
        }

        @Test
        @DisplayName("创建含多种费用项的预算")
        void testCreate_WithMultipleItems() {
            WhPmCharter project = createProject();
            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(project.getId());
            req.setManagementReserve("20000");
            req.setItems(List.of(
                    createLaborItem("160000", "DEV", "1600", "100"),
                    createProcurementItem("50000", "Server", "2", "25000"),
                    createOtherItem("TRAVEL", "30000", "出差费用")
            ));

            WhPmBudget budget = budgetBo.create(req);
            budgetIds.add(budget.getId());

            // costBaseline = 160000 + 50000 + 30000 = 240000
            assertEquals("240000", budget.getCostBaseline());
            // totalBudget = 240000 + 20000 = 260000
            assertEquals("260000", budget.getTotalBudget());
        }

        @Test
        @DisplayName("重复为同一项目创建预算 - 抛出异常")
        void testCreate_DuplicateProject_Throws() {
            WhPmCharter project = createProject();
            createDraftBudget(project.getId());

            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(project.getId());
            ServiceException ex = assertThrows(ServiceException.class, () -> budgetBo.create(req));
            assertTrue(ex.getMessage().contains("已编制预算"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  查询预算
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("查询预算")
    class QueryTests {

        @Test
        @DisplayName("getById - 查询已存在的预算")
        void testGetById_Found() {
            WhPmCharter project = createProject();
            WhPmBudget created = createDraftBudget(project.getId());

            WhPmBudget found = budgetBo.getById(created.getId());
            assertNotNull(found);
            assertEquals(created.getId(), found.getId());
            assertEquals("DRAFT", found.getStatus());
        }

        @Test
        @DisplayName("getById - 查询不存在的预算返回 404")
        void testGetById_NotFound_Throws404() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.getById("non-existent-id"));
            assertEquals(404, ex.getCode());
            assertTrue(ex.getMessage().contains("预算不存在"));
        }

        @Test
        @DisplayName("getDetailWithItems - 含费用项的预算明细")
        void testGetDetailWithItems_WithItems() {
            WhPmCharter project = createProject();
            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(project.getId());
            req.setItems(List.of(
                    createLaborItem("160000", "DEV", "1600", "100"),
                    createProcurementItem("50000", "Server", "2", "25000")
            ));
            WhPmBudget budget = budgetBo.create(req);
            budgetIds.add(budget.getId());

            BudgetDetailVO detail = budgetBo.getDetailWithItems(budget.getId());
            assertNotNull(detail);
            assertNotNull(detail.getBudget());
            assertEquals(budget.getId(), detail.getBudget().getId());
            assertNotNull(detail.getItems());
            assertEquals(2, detail.getItems().size());
        }

        @Test
        @DisplayName("getDetailWithItems - 无费用项的预算明细")
        void testGetDetailWithItems_NoItems() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            BudgetDetailVO detail = budgetBo.getDetailWithItems(budget.getId());
            assertNotNull(detail);
            assertNotNull(detail.getBudget());
            assertNotNull(detail.getItems());
            assertTrue(detail.getItems().isEmpty());
        }

        @Test
        @DisplayName("pageList - 分页查询所有预算")
        void testPageList_All() {
            WhPmCharter project = createProject();
            createDraftBudget(project.getId());

            IPage<WhPmBudget> page = budgetBo.pageList(1, 10, null, null, null);
            assertNotNull(page);
            assertTrue(page.getTotal() >= 1);
            assertTrue(page.getRecords().size() >= 1);
        }

        @Test
        @DisplayName("pageList - 按项目 ID 筛选")
        void testPageList_ByProjectId() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            IPage<WhPmBudget> page = budgetBo.pageList(1, 10, project.getId(), null, null);
            assertEquals(1, page.getTotal());
            assertEquals(budget.getId(), page.getRecords().get(0).getId());
        }

        @Test
        @DisplayName("pageList - 按状态筛选")
        void testPageList_ByStatus() {
            WhPmCharter project = createProject();
            createDraftBudget(project.getId());

            IPage<WhPmBudget> page = budgetBo.pageList(1, 10, null, null, "DRAFT");
            assertTrue(page.getTotal() >= 1);
            page.getRecords().forEach(b -> assertEquals("DRAFT", b.getStatus()));
        }

        @Test
        @DisplayName("pageProjectBudgets - 项目级预算视图")
        void testPageProjectBudgets() {
            WhPmCharter project = createApprovedProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            IPage<ProjectBudgetVO> page = budgetBo.pageProjectBudgets(1, 10, null, null, null);
            assertNotNull(page);
            // Should contain the project with budget info
            boolean found = page.getRecords().stream()
                    .anyMatch(vo -> project.getId().equals(vo.getProjectId()));
            assertTrue(found);
        }
    }

    // ═══════════════════════════════════════════════════════
    //  更新预算
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("更新预算")
    class UpdateTests {

        @Test
        @DisplayName("更新草稿预算 - 成功")
        void testUpdate_Draft_Success() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId(), "10000");

            BudgetUpdateRequest updateReq = new BudgetUpdateRequest();
            updateReq.setManagementReserve("20000");

            budgetBo.update(budget.getId(), updateReq);

            WhPmBudget updated = budgetDao.selectById(budget.getId());
            assertEquals("20000", updated.getManagementReserve());
            assertEquals("20000", updated.getTotalBudget());
        }

        @Test
        @DisplayName("更新草稿预算 - 替换费用项")
        void testUpdate_Draft_ReplaceItems() {
            WhPmCharter project = createProject();
            BudgetCreateRequest createReq = new BudgetCreateRequest();
            createReq.setProjectId(project.getId());
            createReq.setItems(List.of(
                    createLaborItem("160000", "DEV", "1600", "100")
            ));
            WhPmBudget budget = budgetBo.create(createReq);
            budgetIds.add(budget.getId());

            // Update with different items
            BudgetUpdateRequest updateReq = new BudgetUpdateRequest();
            updateReq.setItems(List.of(
                    createLaborItem("80000", "DEV", "800", "100"),
                    createOtherItem("TRAVEL", "20000", "差旅")
            ));

            budgetBo.update(budget.getId(), updateReq);

            WhPmBudget updated = budgetDao.selectById(budget.getId());
            assertEquals("100000", updated.getCostBaseline()); // 80000 + 20000
        }

        @Test
        @DisplayName("更新已提交的预算 - 抛出异常")
        void testUpdate_NonDraft_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());

            BudgetUpdateRequest updateReq = new BudgetUpdateRequest();
            updateReq.setManagementReserve("20000");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.update(budget.getId(), updateReq));
            assertTrue(ex.getMessage().contains("草稿"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  升级预算
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("升级预算")
    class UpgradeTests {

        @Test
        @DisplayName("升级已审批的预算 - 创建草稿版本")
        void testUpgradeCreate_Approved_Success() {
            WhPmCharter project = createApprovedProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());
            budgetBo.approve(budget.getId(), "审批通过");
            WhPmBudget approved = budgetDao.selectById(budget.getId());
            assertEquals("v1.0", approved.getVersion());

            // Upgrade from approved
            BudgetUpdateRequest upgradeReq = new BudgetUpdateRequest();
            upgradeReq.setManagementReserve("30000");
            WhPmBudget upgraded = budgetBo.upgradeCreate(approved.getId(), upgradeReq);
            budgetIds.add(upgraded.getId());

            assertNotNull(upgraded.getId());
            assertEquals("DRAFT", upgraded.getStatus());
            assertEquals("v1.5", upgraded.getVersion());
            assertEquals("30000", upgraded.getManagementReserve());
            assertNotEquals(approved.getId(), upgraded.getId());
        }

        @Test
        @DisplayName("升级非已审批的预算 - 抛出异常")
        void testUpgradeCreate_NotApproved_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            BudgetUpdateRequest upgradeReq = new BudgetUpdateRequest();
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.upgradeCreate(budget.getId(), upgradeReq));
            assertTrue(ex.getMessage().contains("已审批通过"));
        }

        @Test
        @DisplayName("升级提交 - DRAFT 状态提交至 PENDING")
        void testUpgradeSubmit_Draft_Success() {
            WhPmCharter project = createApprovedProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());
            budgetBo.approve(budget.getId(), "审批通过");
            WhPmBudget approved = budgetDao.selectById(budget.getId());

            // Upgrade and submit
            BudgetUpdateRequest upgradeReq = new BudgetUpdateRequest();
            upgradeReq.setItems(List.of(
                    createLaborItem("200000", "DEV", "2000", "100")
            ));
            WhPmBudget upgraded = budgetBo.upgradeCreate(approved.getId(), upgradeReq);
            budgetIds.add(upgraded.getId());

            budgetBo.upgradeSubmit(upgraded.getId());

            WhPmBudget submitted = budgetDao.selectById(upgraded.getId());
            assertEquals("PENDING", submitted.getStatus());
            assertEquals("v1.7", submitted.getVersion());
            assertNotNull(submitted.getProcessInstanceId());
        }

        @Test
        @DisplayName("升级提交 - 非 DRAFT 状态抛出异常")
        void testUpgradeSubmit_NonDraft_Throws() {
            WhPmCharter project = createApprovedProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.upgradeSubmit(budget.getId()));
            assertTrue(ex.getMessage().contains("草稿"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  删除预算
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("删除预算")
    class DeleteTests {

        @Test
        @DisplayName("删除草稿预算 - 成功")
        void testDelete_Draft_Success() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            budgetBo.delete(budget.getId());

            // Budget should no longer exist - physical delete + items soft deleted
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.getById(budget.getId()));
            assertTrue(ex.getMessage().contains("预算不存在"));
        }

        @Test
        @DisplayName("删除已提交的预算 - 抛出异常")
        void testDelete_NonDraft_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.delete(budget.getId()));
            assertTrue(ex.getMessage().contains("草稿"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  预算工作流
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("预算工作流")
    class WorkflowTests {

        @Test
        @DisplayName("提交草稿预算 - 状态变为 PENDING，版本升至 v0.7")
        void testSubmit_Draft_Success() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            assertEquals("v0.5", budget.getVersion());

            budgetBo.submit(budget.getId());

            WhPmBudget submitted = budgetDao.selectById(budget.getId());
            assertEquals("PENDING", submitted.getStatus());
            assertEquals("v0.7", submitted.getVersion());
            assertNotNull(submitted.getProcessInstanceId());
        }

        @Test
        @DisplayName("提交非草稿预算 - 抛出异常")
        void testSubmit_NonDraft_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.submit(budget.getId()));
            assertTrue(ex.getMessage().contains("草稿"));
        }

        @Test
        @DisplayName("审批通过 - 版本升至 v1.0，状态 APPROVED")
        void testApprove_Pending_Success() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());

            budgetBo.approve(pending.getId(), "预算审批通过");

            WhPmBudget approved = budgetDao.selectById(budget.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("v1.0", approved.getVersion());
            assertEquals("预算审批通过", approved.getApprovalComment());
        }

        @Test
        @DisplayName("审批拒绝 - 版本回退至 v0.5，状态 DRAFT")
        void testReject_Pending_Success() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());

            budgetBo.reject(pending.getId(), "需调整预算");

            WhPmBudget rejected = budgetDao.selectById(budget.getId());
            assertEquals("DRAFT", rejected.getStatus());
            assertEquals("v0.5", rejected.getVersion());
            assertEquals("需调整预算", rejected.getApprovalComment());
            assertNull(rejected.getProcessInstanceId());
        }

        @Test
        @DisplayName("审批非审批中的预算 - 抛出异常")
        void testApprove_NonPending_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.approve(budget.getId(), "同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }

        @Test
        @DisplayName("驳回非审批中的预算 - 抛出异常")
        void testReject_NonPending_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.reject(budget.getId(), "不同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }

        @Test
        @DisplayName("完整生命周期：创建 → 提交 → 审批通过")
        void testFullLifecycle() {
            WhPmCharter project = createProject();
            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(project.getId());
            req.setItems(List.of(
                    createLaborItem("160000", "DEV", "1600", "100")
            ));
            WhPmBudget budget = budgetBo.create(req);
            budgetIds.add(budget.getId());
            assertEquals("DRAFT", budget.getStatus());

            // Submit
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());
            assertEquals("PENDING", pending.getStatus());
            assertEquals("v0.7", pending.getVersion());

            // Approve
            budgetBo.approve(pending.getId(), "同意预算");
            WhPmBudget approved = budgetDao.selectById(budget.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("v1.0", approved.getVersion());
            assertEquals("同意预算", approved.getApprovalComment());
        }

        @Test
        @DisplayName("审批拒绝后重新提交")
        void testRejectThenResubmit() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());
            budgetBo.submit(budget.getId());

            // Reject
            WhPmBudget pending = budgetDao.selectById(budget.getId());
            budgetBo.reject(pending.getId(), "驳回原因");
            WhPmBudget rejected = budgetDao.selectById(budget.getId());
            assertEquals("DRAFT", rejected.getStatus());

            // Resubmit
            budgetBo.submit(budget.getId());
            WhPmBudget resubmitted = budgetDao.selectById(budget.getId());
            assertEquals("PENDING", resubmitted.getStatus());
            assertEquals("v0.7", resubmitted.getVersion());
            assertNotNull(resubmitted.getProcessInstanceId());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  预实对比
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("预实对比")
    class ComparisonTests {

        @Test
        @DisplayName("获取已审批预算的预实对比")
        void testGetComparison_WithApprovedBudget() {
            WhPmCharter project = createApprovedProject();
            BudgetCreateRequest req = new BudgetCreateRequest();
            req.setProjectId(project.getId());
            req.setItems(List.of(
                    createLaborItem("160000", "DEV", "1600", "100")
            ));
            WhPmBudget budget = budgetBo.create(req);
            budgetIds.add(budget.getId());

            budgetBo.submit(budget.getId());
            budgetBo.approve(budget.getId(), "审批通过");
            WhPmBudget approved = budgetDao.selectById(budget.getId());
            assertEquals("v1.0", approved.getVersion());

            BudgetComparisonVO comparison = budgetBo.getComparison(approved.getId(), null);
            assertNotNull(comparison);
            assertNotNull(comparison.getProjectName());
            assertNotNull(comparison.getItems());
            assertEquals("v1.0", comparison.getVersion());
        }

        @Test
        @DisplayName("获取无已审批预算的预实对比 - 抛出异常")
        void testGetComparison_NoApprovedBudget_Throws() {
            WhPmCharter project = createProject();
            WhPmBudget budget = createDraftBudget(project.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.getComparison(budget.getId(), null));
            assertTrue(ex.getMessage().contains("没有已审批通过"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  版本历史
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("版本历史")
    class VersionHistoryTests {

        @Test
        @DisplayName("获取项目预算版本历史")
        void testGetVersionHistory_WithBudgets() {
            WhPmCharter project = createApprovedProject();
            WhPmBudget v1 = createDraftBudget(project.getId());
            budgetBo.submit(v1.getId());
            budgetBo.approve(v1.getId(), "审批通过 v1");

            // Upgrade to v2 draft
            BudgetUpdateRequest upgradeReq = new BudgetUpdateRequest();
            WhPmBudget v2 = budgetBo.upgradeCreate(v1.getId(), upgradeReq);
            budgetIds.add(v2.getId());

            List<BudgetVersionVO> history = budgetBo.getVersionHistory(project.getId());
            assertNotNull(history);
            assertTrue(history.size() >= 2);
            BudgetVersionVO latest = history.get(0);
            assertNotNull(latest.getId());
            assertNotNull(latest.getVersion());
            assertNotNull(latest.getStatus());
        }

        @Test
        @DisplayName("获取无预算项目的版本历史 - 返回空列表")
        void testGetVersionHistory_NoBudgets() {
            WhPmCharter project = createProject();

            List<BudgetVersionVO> history = budgetBo.getVersionHistory(project.getId());
            assertNotNull(history);
            assertTrue(history.isEmpty());
        }
    }
}
