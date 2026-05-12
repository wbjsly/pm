package com.wh;

import com.wh.approval.pm.BudgetApprovalCallback;
import com.wh.approval.pm.CharterApprovalCallback;
import com.wh.approval.pm.DeliverableApprovalCallback;
import com.wh.bo.pm.*;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmDeliverableDao;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmDeliverable;
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
@DisplayName("工作流方法死锁修复验证 - Charter/Budget/Deliverable")
class WhPmWorkflowBoTest {

    private static final String TEST_USER = "test_pm_user_001";

    @Autowired private WhPmCharterBo charterBo;
    @Autowired private WhPmBudgetBo budgetBo;
    @Autowired private WhPmDeliverableBo deliverableBo;
    @Autowired private WhPmCharterDao charterDao;
    @Autowired private WhPmBudgetDao budgetDao;
    @Autowired private WhPmDeliverableDao deliverableDao;
    @Autowired private CharterApprovalCallback charterCallback;
    @Autowired private BudgetApprovalCallback budgetCallback;
    @Autowired private DeliverableApprovalCallback deliverableCallback;

    private final List<String> createdCharterIds = new ArrayList<>();
    private final List<String> createdBudgetIds = new ArrayList<>();
    private final List<String> createdDeliverableIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TEST_USER, "password", Collections.emptyList()));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        // Physical delete in reverse order to respect FK constraints
        for (String id : createdDeliverableIds) {
            try { deliverableDao.deleteById(id); } catch (Exception ignored) {}
        }
        for (String id : createdBudgetIds) {
            try { budgetDao.deleteById(id); } catch (Exception ignored) {}
        }
        for (String id : createdCharterIds) {
            try { charterDao.deleteById(id); } catch (Exception ignored) {}
        }
        createdDeliverableIds.clear();
        createdBudgetIds.clear();
        createdCharterIds.clear();
    }

    // ─── helpers ───

    private WhPmCharter createTestCharter() {
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName("测试项目-" + UUID.randomUUID().toString().substring(0, 6));
        req.setProjectCode("TEST-" + UUID.randomUUID().toString().substring(0, 6));
        req.setProjectShortName("TP");
        req.setSponsorId(TEST_USER);
        req.setPmId(TEST_USER);
        WhPmCharter charter = charterBo.create(req);
        createdCharterIds.add(charter.getId());
        return charter;
    }

    private WhPmCharter createApprovedCharter() {
        WhPmCharter charter = createTestCharter();
        charterBo.submit(charter.getId());
        // reload after submit
        charter = charterDao.selectById(charter.getId());
        charterBo.approve(charter.getId(), "测试审批通过");
        return charterDao.selectById(charter.getId());
    }

    private WhPmBudget createTestBudget(String projectId) {
        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setProjectId(projectId);
        WhPmBudget budget = budgetBo.create(req);
        createdBudgetIds.add(budget.getId());
        return budget;
    }

    private WhPmDeliverable createTestDeliverable(String projectId) {
        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("测试成果物-" + UUID.randomUUID().toString().substring(0, 6));
        req.setProjectId(projectId);
        req.setPlannedDeliveryDate("2026-12-31");
        WhPmDeliverable deliverable = deliverableBo.create(req);
        createdDeliverableIds.add(deliverable.getId());
        return deliverable;
    }

    // ═══════════════════════════════════════════════════════
    //  Charter 工作流测试
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Charter 章程工作流")
    class CharterWorkflowTests {

        @Test
        @DisplayName("submit - 无死锁，状态正确变为 PENDING_APPROVAL")
        void testSubmit_NoDeadlock_StatusUpdated() {
            WhPmCharter charter = createTestCharter();
            assertEquals("DRAFT", charter.getStatus());

            charterBo.submit(charter.getId());

            WhPmCharter updated = charterDao.selectById(charter.getId());
            assertEquals("PENDING_APPROVAL", updated.getStatus());
            assertNotNull(updated.getProcessInstanceId());
        }

        @Test
        @DisplayName("submit - DRAFT 以外状态抛出异常")
        void testSubmit_NonDraftStatus_Throws() {
            WhPmCharter charter = createTestCharter();
            charterBo.submit(charter.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> charterBo.submit(charter.getId()));
            assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
        }

        @Test
        @DisplayName("approve - 状态正确变为 APPROVED，审批意见已保存")
        void testApprove_Success() {
            WhPmCharter charter = createTestCharter();
            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());

            charterBo.approve(pending.getId(), "同意立项");

            WhPmCharter approved = charterDao.selectById(charter.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("同意立项", approved.getApprovalComment());
        }

        @Test
        @DisplayName("approve - 非审批中状态抛出异常")
        void testApprove_NonPendingStatus_Throws() {
            WhPmCharter charter = createTestCharter();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> charterBo.approve(charter.getId(), "同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }

        @Test
        @DisplayName("reject - 状态正确变为 REJECTED，processInstanceId 清空")
        void testReject_Success() {
            WhPmCharter charter = createTestCharter();
            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());

            charterBo.reject(pending.getId(), "需修改预算");

            WhPmCharter rejected = charterDao.selectById(charter.getId());
            assertEquals("REJECTED", rejected.getStatus());
            assertEquals("需修改预算", rejected.getApprovalComment());
            assertNull(rejected.getProcessInstanceId());
        }

        @Test
        @DisplayName("reject - 非审批中状态抛出异常")
        void testReject_NonPendingStatus_Throws() {
            WhPmCharter charter = createTestCharter();

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> charterBo.reject(charter.getId(), "不同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }

        @Test
        @DisplayName("submit→reject→resubmit 完整生命周期")
        void testSubmitRejectResubmit_FullCycle() {
            WhPmCharter charter = createTestCharter();

            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());
            assertEquals("PENDING_APPROVAL", pending.getStatus());

            charterBo.reject(pending.getId(), "修改后重新提交");
            WhPmCharter rejected = charterDao.selectById(charter.getId());
            assertEquals("REJECTED", rejected.getStatus());

            // REJECTED charter can be submitted again
            charterBo.submit(charter.getId());
            WhPmCharter resubmitted = charterDao.selectById(charter.getId());
            assertEquals("PENDING_APPROVAL", resubmitted.getStatus());
            assertNotNull(resubmitted.getProcessInstanceId());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Budget 工作流测试
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Budget 预算工作流")
    class BudgetWorkflowTests {

        @Test
        @DisplayName("submit - 无死锁，版本和状态正确")
        void testSubmit_NoDeadlock_VersionAndStatusUpdated() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());
            assertEquals("DRAFT", budget.getStatus());
            assertEquals("v0.5", budget.getVersion());

            budgetBo.submit(budget.getId());

            WhPmBudget updated = budgetDao.selectById(budget.getId());
            assertEquals("PENDING", updated.getStatus());
            assertEquals("v0.7", updated.getVersion());
            assertNotNull(updated.getProcessInstanceId());
        }

        @Test
        @DisplayName("upgradeSubmit - 无死锁，版本和状态正确")
        void testUpgradeSubmit_NoDeadlock_VersionAndStatusUpdated() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());
            assertEquals("v0.5", budget.getVersion());

            budgetBo.upgradeSubmit(budget.getId());

            WhPmBudget updated = budgetDao.selectById(budget.getId());
            assertEquals("PENDING", updated.getStatus());
            assertEquals("v0.7", updated.getVersion());
            assertNotNull(updated.getProcessInstanceId());
        }

        @Test
        @DisplayName("approve - 版本从 v0.7 升至 v1.0，状态变为 APPROVED")
        void testApprove_VersionBump_Success() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());
            assertEquals("v0.7", pending.getVersion());

            budgetBo.approve(pending.getId(), "预算审批通过");

            WhPmBudget approved = budgetDao.selectById(budget.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("v1.0", approved.getVersion());
            assertEquals("预算审批通过", approved.getApprovalComment());
        }

        @Test
        @DisplayName("reject - 版本从 v0.7 回退到 v0.5，状态变为 DRAFT")
        void testReject_VersionReset_Success() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());
            assertEquals("v0.7", pending.getVersion());

            budgetBo.reject(pending.getId(), "需调整费用");

            WhPmBudget rejected = budgetDao.selectById(budget.getId());
            assertEquals("DRAFT", rejected.getStatus());
            assertEquals("v0.5", rejected.getVersion());
            assertEquals("需调整费用", rejected.getApprovalComment());
            assertNull(rejected.getProcessInstanceId());
        }

        @Test
        @DisplayName("approve - 非 PENDING 状态抛出异常")
        void testApprove_NonPendingStatus_Throws() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.approve(budget.getId(), "同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }

        @Test
        @DisplayName("reject - 非 PENDING 状态抛出异常")
        void testReject_NonPendingStatus_Throws() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> budgetBo.reject(budget.getId(), "不同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Deliverable 工作流测试
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("Deliverable 成果物工作流")
    class DeliverableWorkflowTests {

        @Test
        @DisplayName("submit - 无死锁，状态正确变为 PENDING_APPROVAL")
        void testSubmit_NoDeadlock_StatusUpdated() {
            WhPmCharter charter = createApprovedCharter();
            WhPmDeliverable deliverable = createTestDeliverable(charter.getId());
            assertEquals("DRAFT", deliverable.getStatus());

            deliverableBo.submit(deliverable.getId());

            WhPmDeliverable updated = deliverableDao.selectById(deliverable.getId());
            assertEquals("PENDING_APPROVAL", updated.getStatus());
            assertNotNull(updated.getProcessInstanceId());
        }

        @Test
        @DisplayName("submit - DRAFT/REJECTED 以外状态抛出异常")
        void testSubmit_NonDraftStatus_Throws() {
            WhPmCharter charter = createApprovedCharter();
            WhPmDeliverable deliverable = createTestDeliverable(charter.getId());
            deliverableBo.submit(deliverable.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> deliverableBo.submit(deliverable.getId()));
            assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
        }

        @Test
        @DisplayName("approve - 状态正确变为 APPROVED")
        void testApprove_Success() {
            WhPmCharter charter = createApprovedCharter();
            WhPmDeliverable deliverable = createTestDeliverable(charter.getId());
            deliverableBo.submit(deliverable.getId());
            WhPmDeliverable pending = deliverableDao.selectById(deliverable.getId());

            deliverableBo.approve(pending.getId(), "成果物验收通过");

            WhPmDeliverable approved = deliverableDao.selectById(deliverable.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("成果物验收通过", approved.getApprovalComment());
        }

        @Test
        @DisplayName("reject - 状态正确变为 REJECTED，processInstanceId 清空")
        void testReject_Success() {
            WhPmCharter charter = createApprovedCharter();
            WhPmDeliverable deliverable = createTestDeliverable(charter.getId());
            deliverableBo.submit(deliverable.getId());
            WhPmDeliverable pending = deliverableDao.selectById(deliverable.getId());

            deliverableBo.reject(pending.getId(), "需修改后重新提交");

            WhPmDeliverable rejected = deliverableDao.selectById(deliverable.getId());
            assertEquals("REJECTED", rejected.getStatus());
            assertEquals("需修改后重新提交", rejected.getApprovalComment());
            assertNull(rejected.getProcessInstanceId());
        }

        @Test
        @DisplayName("approve - 非审批中状态抛出异常")
        void testApprove_NonPendingStatus_Throws() {
            WhPmCharter charter = createApprovedCharter();
            WhPmDeliverable deliverable = createTestDeliverable(charter.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> deliverableBo.approve(deliverable.getId(), "同意"));
            assertTrue(ex.getMessage().contains("审批"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  死锁预防测试 (连续操作)
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("死锁预防 - 连续操作验证")
    class DeadlockPreventionTests {

        @Test
        @DisplayName("Charter submit→approve 连续执行无死锁")
        void testCharter_SubmitThenApprove_NoDeadlock() {
            WhPmCharter charter = createTestCharter();
            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());

            long start = System.currentTimeMillis();
            charterBo.approve(pending.getId(), "审批通过");
            long elapsed = System.currentTimeMillis() - start;

            assertTrue(elapsed < 5000, "approve 应在 5 秒内完成（busy_timeout=5s），实际耗时 " + elapsed + "ms");
            WhPmCharter approved = charterDao.selectById(charter.getId());
            assertEquals("APPROVED", approved.getStatus());
        }

        @Test
        @DisplayName("Charter submit→reject→resubmit→approve 连续执行无死锁")
        void testCharter_FullCycle_NoDeadlock() {
            WhPmCharter charter = createTestCharter();

            long start = System.currentTimeMillis();

            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());
            assertEquals("PENDING_APPROVAL", pending.getStatus());

            charterBo.reject(pending.getId(), "需修改");
            WhPmCharter rejected = charterDao.selectById(charter.getId());
            assertEquals("REJECTED", rejected.getStatus());

            charterBo.submit(charter.getId());
            WhPmCharter resubmitted = charterDao.selectById(charter.getId());
            assertEquals("PENDING_APPROVAL", resubmitted.getStatus());

            charterBo.approve(resubmitted.getId(), "最终审批通过");

            long elapsed = System.currentTimeMillis() - start;
            assertTrue(elapsed < 10000, "完整工作流应在 10 秒内完成，实际耗时 " + elapsed + "ms");

            WhPmCharter approved = charterDao.selectById(charter.getId());
            assertEquals("APPROVED", approved.getStatus());
        }

        @Test
        @DisplayName("Budget submit→reject 连续执行无死锁")
        void testBudget_SubmitThenReject_NoDeadlock() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());

            long start = System.currentTimeMillis();
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());
            budgetBo.reject(pending.getId(), "驳回预算");
            long elapsed = System.currentTimeMillis() - start;

            assertTrue(elapsed < 5000, "submit+reject 应在 5 秒内完成，实际耗时 " + elapsed + "ms");
            WhPmBudget rejected = budgetDao.selectById(budget.getId());
            assertEquals("DRAFT", rejected.getStatus());
            assertEquals("v0.5", rejected.getVersion());
        }

        @Test
        @DisplayName("Budget submit→approve 连续执行无死锁")
        void testBudget_SubmitThenApprove_NoDeadlock() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());

            long start = System.currentTimeMillis();
            budgetBo.submit(budget.getId());
            WhPmBudget pending = budgetDao.selectById(budget.getId());
            budgetBo.approve(pending.getId(), "预算审批通过");
            long elapsed = System.currentTimeMillis() - start;

            assertTrue(elapsed < 5000, "submit+approve 应在 5 秒内完成，实际耗时 " + elapsed + "ms");
            WhPmBudget approved = budgetDao.selectById(budget.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("v1.0", approved.getVersion());
        }
    }

    // ═══════════════════════════════════════════════════════
    //  Callback 验证测试
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("回调验证 - 状态由 Bo 方法更新而非回调")
    class CallbackVerificationTests {

        @Test
        @DisplayName("Charter approve 后实体由 Bo 方法直接更新")
        void testCharter_EntityUpdatedByBoMethod() {
            WhPmCharter charter = createTestCharter();
            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());

            charterBo.approve(pending.getId(), "Bo方法更新测试");

            WhPmCharter approved = charterDao.selectById(charter.getId());
            assertEquals("APPROVED", approved.getStatus());
            assertEquals("Bo方法更新测试", approved.getApprovalComment());
        }

        @Test
        @DisplayName("Charter reject 后 processInstanceId 已清空")
        void testCharter_ProcessInstanceIdClearedAfterReject() {
            WhPmCharter charter = createTestCharter();
            charterBo.submit(charter.getId());
            WhPmCharter pending = charterDao.selectById(charter.getId());
            assertNotNull(pending.getProcessInstanceId());

            charterBo.reject(pending.getId(), "驳回");

            WhPmCharter rejected = charterDao.selectById(charter.getId());
            assertNull(rejected.getProcessInstanceId(), "驳回后 processInstanceId 应为 null");
        }

        @Test
        @DisplayName("Budget 连续 submit→reject→submit 版本号正确切换")
        void testBudget_VersionTransitionsCorrectly() {
            WhPmCharter charter = createTestCharter();
            WhPmBudget budget = createTestBudget(charter.getId());
            assertEquals("v0.5", budget.getVersion());

            budgetBo.submit(budget.getId());
            WhPmBudget v07 = budgetDao.selectById(budget.getId());
            assertEquals("v0.7", v07.getVersion());

            budgetBo.reject(v07.getId(), "驳回");
            WhPmBudget v05 = budgetDao.selectById(budget.getId());
            assertEquals("v0.5", v05.getVersion());

            budgetBo.submit(budget.getId());
            WhPmBudget v07again = budgetDao.selectById(budget.getId());
            assertEquals("v0.7", v07again.getVersion());
        }

        @Test
        @DisplayName("Deliverable reject 后可以重新提交")
        void testDeliverable_CanResubmitAfterReject() {
            WhPmCharter charter = createApprovedCharter();
            WhPmDeliverable deliverable = createTestDeliverable(charter.getId());

            deliverableBo.submit(deliverable.getId());
            WhPmDeliverable pending = deliverableDao.selectById(deliverable.getId());
            assertEquals("PENDING_APPROVAL", pending.getStatus());

            deliverableBo.reject(pending.getId(), "需要修改");
            WhPmDeliverable rejected = deliverableDao.selectById(deliverable.getId());
            assertEquals("REJECTED", rejected.getStatus());
            assertNull(rejected.getProcessInstanceId());

            // REJECTED deliverable can be resubmitted
            deliverableBo.submit(deliverable.getId());
            WhPmDeliverable resubmitted = deliverableDao.selectById(deliverable.getId());
            assertEquals("PENDING_APPROVAL", resubmitted.getStatus());
            assertNotNull(resubmitted.getProcessInstanceId());
        }
    }
}
