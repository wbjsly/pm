package com.wh;

import com.wh.bo.pm.*;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWorkLog;
import com.wh.entity.pm.WhPmWbsElement;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
class WhPmWorkLogBoTest {

    @Autowired
    private WhPmWorkLogBo workLogBo;

    @Autowired
    private WhPmCharterBo charterBo;

    @Autowired
    private WhSysWorkCalendarBo calendarBo;

    @Autowired
    private WhPmWbsElementDao wbsElementDao;

    @Autowired
    private com.wh.dao.pm.WhPmWorkLogDao workLogDao;

    private String pmUserId = "test_pm_user_001";
    private String regularUserId = "test_regular_user_001";
    private String projectId;

    @BeforeEach
    void setUp() {
        // Create test calendar for 2026
        calendarBo.generateYear("2026", "system");

        // Create a test project with PM
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName("测试项目-工时");
        req.setProjectCode("TEST-PROJECT-CODE");
        req.setProjectShortName("TEST-PM");
        req.setSponsorId(pmUserId);
        req.setPmId(pmUserId);
        WhPmCharter charter = charterBo.create(req);
        projectId = charter.getId();
    }

    @Test
    void testCreate_AsPM_ShouldSucceed() {
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate("2026-01-05");
        req.setHoursWorked("8");
        req.setWorkDescription("测试工时");

        WhPmWorkLog log = workLogBo.create(pmUserId, req);
        assertNotNull(log.getId());
        assertEquals("DRAFT", log.getStatus());
        assertEquals("8", log.getHoursWorked());
        assertEquals(pmUserId, log.getCreateBy());
    }

    @Test
    void testCreate_AsWbsOwner_ShouldSucceed() {
        // Regular user should be able to create if they own a WBS element
        WhPmWbsElement wbs = new WhPmWbsElement();
        wbs.setProjectId(projectId);
        wbs.setWbsCode("TEST-001");
        wbs.setName("测试WBS");
        wbs.setElementType("TASK");
        wbs.setLevel(1);
        wbs.setOwnerId(regularUserId);
        wbs.setPlannedOwnerId(regularUserId);
        wbs.setStatus("PLANNED");
        wbs.setCreateBy("system");
        wbs.setDelFlag("0");
        wbs.setVerNo(0);
        wbsElementDao.insert(wbs);

        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate("2026-01-05");
        req.setHoursWorked("6");
        req.setWorkDescription("WBS owner 录入");

        WhPmWorkLog log = workLogBo.create(regularUserId, req);
        assertNotNull(log.getId());
        assertEquals("DRAFT", log.getStatus());
    }

    @Test
    void testCreate_NoPermission_ShouldFail() {
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate("2026-01-05");
        req.setHoursWorked("8");
        req.setWorkDescription("无权限录入");

        Exception ex = assertThrows(Exception.class, () -> workLogBo.create("random_user_001", req));
        assertTrue(ex.getMessage().contains("没有该项目的工时录入权限"));
    }

    @Test
    void testCreate_FutureDate_ShouldFail() {
        String futureDate = LocalDate.now().plusDays(1).toString();
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate(futureDate);
        req.setHoursWorked("8");
        req.setWorkDescription("未来日期");

        Exception ex = assertThrows(Exception.class, () -> workLogBo.create(pmUserId, req));
        assertTrue(ex.getMessage().contains("不能录入未来日期的工时"));
    }

    @Test
    void testCreate_ExceedDailyHours_ShouldFail() {
        // First entry: 18 hours
        WorkLogCreateRequest req1 = new WorkLogCreateRequest();
        req1.setProjectId(projectId);
        req1.setLogDate("2026-01-05");
        req1.setHoursWorked("18");
        req1.setWorkDescription("第一批工时");
        workLogBo.create(pmUserId, req1);

        // Second entry: 7 hours (total would be 25, exceeds 24)
        WorkLogCreateRequest req2 = new WorkLogCreateRequest();
        req2.setProjectId(projectId);
        req2.setLogDate("2026-01-05");
        req2.setHoursWorked("7");
        req2.setWorkDescription("第二批工时");

        Exception ex = assertThrows(Exception.class, () -> workLogBo.create(pmUserId, req2));
        assertTrue(ex.getMessage().contains("当日累计工时不得超过24小时"));
    }

    @Test
    void testUpdate_DraftLog_ShouldSucceed() {
        // Create a draft log
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("6");
        createReq.setWorkDescription("原始描述");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);

        // Update it
        WorkLogUpdateRequest updateReq = new WorkLogUpdateRequest();
        updateReq.setId(created.getId());
        updateReq.setProjectId(projectId);
        updateReq.setLogDate("2026-01-06");
        updateReq.setHoursWorked("7");
        updateReq.setWorkDescription("更新后的描述");

        WhPmWorkLog updated = workLogBo.update(pmUserId, updateReq);
        assertEquals("7", updated.getHoursWorked());
        assertEquals("2026-01-06", updated.getLogDate());
        assertEquals("更新后的描述", updated.getWorkDescription());
    }

    @Test
    void testUpdate_ApprovedLog_ShouldFail() {
        // Create and approve
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("8");
        createReq.setWorkDescription("待审批");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);

        workLogBo.approve(pmUserId, created.getId(), null);

        // Try to update
        WorkLogUpdateRequest updateReq = new WorkLogUpdateRequest();
        updateReq.setId(created.getId());
        updateReq.setProjectId(projectId);
        updateReq.setLogDate("2026-01-06");
        updateReq.setHoursWorked("7");
        updateReq.setWorkDescription("不应能更新");

        Exception ex = assertThrows(Exception.class, () -> workLogBo.update(pmUserId, updateReq));
        assertTrue(ex.getMessage().contains("已审批的工时不可修改"));
    }

    @Test
    void testDelete_DraftLog_ShouldSucceed() {
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("8");
        createReq.setWorkDescription("待删除");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);

        workLogBo.delete(pmUserId, created.getId());

        Exception ex = assertThrows(Exception.class, () -> workLogBo.getById(created.getId()));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    void testDelete_ApprovedLog_ShouldFail() {
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("8");
        createReq.setWorkDescription("已审批不可删除");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);

        workLogBo.approve(pmUserId, created.getId(), null);

        Exception ex = assertThrows(Exception.class, () -> workLogBo.delete(pmUserId, created.getId()));
        assertTrue(ex.getMessage().contains("已审批的工时不可删除"));
    }

    @Test
    void testApprove_Single() {
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("8");
        createReq.setWorkDescription("待审批");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);

        workLogBo.approve(pmUserId, created.getId(), null);

        WhPmWorkLog approved = workLogBo.getById(created.getId());
        assertEquals("APPROVED", approved.getStatus());
    }

    @Test
    void testReject_WithReason() {
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("8");
        createReq.setWorkDescription("待驳回");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);

        workLogBo.reject(pmUserId, created.getId(), "描述不清晰");

        WhPmWorkLog rejected = workLogBo.getById(created.getId());
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("描述不清晰", rejected.getBlockerReason());
    }

    @Test
    void testResubmit_RejectedToDraft() {
        WorkLogCreateRequest createReq = new WorkLogCreateRequest();
        createReq.setProjectId(projectId);
        createReq.setLogDate("2026-01-05");
        createReq.setHoursWorked("8");
        createReq.setWorkDescription("被驳回");
        WhPmWorkLog created = workLogBo.create(pmUserId, createReq);
        workLogBo.reject(pmUserId, created.getId(), "不合格");

        workLogBo.resubmit(pmUserId, created.getId());

        WhPmWorkLog resubmitted = workLogBo.getById(created.getId());
        assertEquals("DRAFT", resubmitted.getStatus());
    }

    @Test
    void testBatchApprove() {
        for (int i = 5; i <= 9; i++) {
            String date = String.format("2026-01-0%d", i);
            WorkLogCreateRequest createReq = new WorkLogCreateRequest();
            createReq.setProjectId(projectId);
            createReq.setLogDate(date);
            createReq.setHoursWorked("8");
            createReq.setWorkDescription("批量测试" + i);
            workLogBo.create(pmUserId, createReq);
        }

        List<WhPmWorkLog> pending = workLogBo.getByUserAndMonth(pmUserId, "2026", 1);
        List<String> ids = pending.stream().map(WhPmWorkLog::getId).collect(Collectors.toList());

        Map<String, Object> result = workLogBo.batchApprove(pmUserId, ids);
        assertTrue((int) result.get("success") >= 1);
    }

    @Test
    void testStats_MonthWithNoLogs() {
        // Generate calendar but no logs
        var stats = workLogBo.getStats(regularUserId, "2026", 1);
        assertTrue(stats.getWorkDays() > 0, "Should have work days from calendar");
        assertEquals(0, stats.getFilledDays());
        assertEquals(stats.getWorkDays(), stats.getUnfilledDays());
        assertTrue(stats.getGapHours().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testStats_MonthWithFullLogs() {
        // Fill all workdays in January 2026 with 8h each
        List<String> workDays = calendarBo.getWorkDays("2026", 1);
        for (String date : workDays) {
            WorkLogCreateRequest createReq = new WorkLogCreateRequest();
            createReq.setProjectId(projectId);
            createReq.setLogDate(date);
            createReq.setHoursWorked("8");
            createReq.setWorkDescription("满额工时");
            workLogBo.create(pmUserId, createReq);
        }

        var stats = workLogBo.getStats(pmUserId, "2026", 1);
        assertEquals(workDays.size(), stats.getFilledDays());
        assertEquals(0, stats.getUnfilledDays());
        assertEquals(0, stats.getGapHours().compareTo(BigDecimal.ZERO));
    }

    // ═══════════════════════════════════════════════════════
    //  补充：权限与状态分支
    // ═══════════════════════════════════════════════════════

    private WhPmWorkLog createLog(String date, String hours, String desc) {
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate(date);
        req.setHoursWorked(hours);
        req.setWorkDescription(desc);
        return workLogBo.create(pmUserId, req);
    }

    @Test
    void testUpdate_NotOwner_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-03", "8", "他人修改");
        WorkLogUpdateRequest req = new WorkLogUpdateRequest();
        req.setId(log.getId());
        req.setProjectId(projectId);
        req.setLogDate("2026-02-03");
        req.setHoursWorked("6");

        Exception ex = assertThrows(Exception.class, () -> workLogBo.update(regularUserId, req));
        assertTrue(ex.getMessage().contains("只能修改自己的工时"));
    }

    @Test
    void testDelete_NotOwner_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-04", "8", "他人删除");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.delete(regularUserId, log.getId()));
        assertTrue(ex.getMessage().contains("只能删除自己的工时"));
    }

    @Test
    void testResubmit_NotOwner_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-05", "8", "他人重提");
        workLogBo.reject(pmUserId, log.getId(), "驳回");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.resubmit(regularUserId, log.getId()));
        assertTrue(ex.getMessage().contains("只能重新提交自己的工时"));
    }

    @Test
    void testResubmit_NotRejected_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-06", "8", "草稿重提");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.resubmit(pmUserId, log.getId()));
        assertTrue(ex.getMessage().contains("只能重新提交被驳回"));
    }

    @Test
    void testApprove_NotPm_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-07", "8", "非PM审批");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.approve(regularUserId, log.getId(), "同意"));
        assertTrue(ex.getMessage().contains("不是该项目的经理"));
    }

    @Test
    void testReject_NotPm_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-08", "8", "非PM驳回");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.reject(regularUserId, log.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("不是该项目的经理"));
    }

    @Test
    void testApprove_AlreadyApproved_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-09", "8", "重复审批");
        workLogBo.approve(pmUserId, log.getId(), "通过");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.approve(pmUserId, log.getId(), "再通过"));
        assertTrue(ex.getMessage().contains("只能审批待审批"));
    }

    @Test
    void testBatchApprove_EmptyList_ReturnsZero() {
        Map<String, Object> result = workLogBo.batchApprove(pmUserId, List.of());
        assertEquals(0, result.get("success"));
        assertEquals(0, result.get("skipped"));
    }

    @Test
    void testBatchReject_EmptyList_ReturnsZero() {
        Map<String, Object> result = workLogBo.batchReject(pmUserId, List.of(), "批量驳回");
        assertEquals(0, result.get("success"));
        assertEquals(0, result.get("skipped"));
    }

    @Test
    void testGetById_AfterLogicalDelete_ShouldFail() {
        WhPmWorkLog log = createLog("2026-02-10", "8", "逻辑删除");
        workLogBo.delete(pmUserId, log.getId());
        Exception ex = assertThrows(Exception.class, () -> workLogBo.getById(log.getId()));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    void testStats_PartialHours_ComputesGap() {
        // 只填 4 小时（标准 8 小时），应产生缺口
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId(projectId);
        req.setLogDate("2026-02-11");
        req.setHoursWorked("4");
        req.setWorkDescription("部分工时");
        workLogBo.create(pmUserId, req);

        var stats = workLogBo.getStats(pmUserId, "2026", 2);
        // 4 小时 < 标准 8 小时 → 计入 partialDays 并产生缺口
        assertTrue(stats.getPartialDays() >= 1);
        assertTrue(stats.getGapHours().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void testUpdate_Rejected_ShouldSucceed() {
        WhPmWorkLog log = createLog("2026-02-12", "8", "驳回后可改");
        workLogBo.reject(pmUserId, log.getId(), "驳回");
        WorkLogUpdateRequest req = new WorkLogUpdateRequest();
        req.setId(log.getId());
        req.setProjectId(projectId);
        req.setLogDate("2026-02-12");
        req.setHoursWorked("6");
        req.setWorkDescription("修改后");

        WhPmWorkLog updated = workLogBo.update(pmUserId, req);
        assertEquals("6", updated.getHoursWorked());
    }

    @Test
    void testEnrichLogs_NullFields_Tolerated() {
        // 正常记录（供对照）
        createLog("2026-03-05", "8", "对照记录");

        // 直接插入 createBy 为 null、updateBy 为不存在用户的记录
        WhPmWorkLog ghost = new WhPmWorkLog();
        ghost.setProjectId(projectId);
        ghost.setLogDate("2026-03-06");
        ghost.setHoursWorked("8");
        ghost.setWorkDescription("ghost记录");
        ghost.setStatus("APPROVED");
        ghost.setUpdateBy("ghost_user");
        ghost.setDelFlag("0");
        ghost.setVerNo(0);
        workLogDao.insert(ghost);

        List<WhPmWorkLog> logs = workLogBo.getByProjectAndMonth(projectId, "2026", 3);
        WhPmWorkLog found = logs.stream().filter(l -> ghost.getId().equals(l.getId())).findFirst().orElse(null);
        assertNotNull(found);
        // createBy 为 null：createByName 应为 null
        assertNull(found.getCreateByName());
        // updateBy 无对应用户：approverName 应为 null
        assertNull(found.getApproverName());
    }

    // ═══════════════════════════════════════════════════════
    //  补充：项目缺失 / 状态边界 / enrichLogs project null
    // ═══════════════════════════════════════════════════════

    @Test
    void testGetById_NotFound_Throws() {
        Exception ex = assertThrows(Exception.class, () -> workLogBo.getById("nonexistent-log"));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    void testApprove_ProjectMissing_ShouldFail() {
        WhPmWorkLog log = createLog("2026-03-10", "8", "无项目审批");
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WhPmWorkLog> w =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        w.eq(WhPmWorkLog::getId, log.getId()).set(WhPmWorkLog::getProjectId, "missing-project");
        workLogDao.update(null, w);

        Exception ex = assertThrows(Exception.class, () -> workLogBo.approve(pmUserId, log.getId(), "同意"));
        assertTrue(ex.getMessage().contains("不是该项目的经理"));
    }

    @Test
    void testReject_ProjectMissing_ShouldFail() {
        WhPmWorkLog log = createLog("2026-03-11", "8", "无项目驳回");
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WhPmWorkLog> w =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        w.eq(WhPmWorkLog::getId, log.getId()).set(WhPmWorkLog::getProjectId, "missing-project");
        workLogDao.update(null, w);

        Exception ex = assertThrows(Exception.class, () -> workLogBo.reject(pmUserId, log.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("不是该项目的经理"));
    }

    @Test
    void testReject_AlreadyApproved_ShouldFail() {
        WhPmWorkLog log = createLog("2026-03-12", "8", "已审批驳回");
        workLogBo.approve(pmUserId, log.getId(), "通过");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.reject(pmUserId, log.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("只能审批待审批"));
    }

    @Test
    void testCreate_ProjectMissing_ShouldFail() {
        WorkLogCreateRequest req = new WorkLogCreateRequest();
        req.setProjectId("missing-project");
        req.setLogDate("2026-03-13");
        req.setHoursWorked("8");
        req.setWorkDescription("无项目");
        Exception ex = assertThrows(Exception.class, () -> workLogBo.create(pmUserId, req));
        assertTrue(ex.getMessage().contains("没有该项目的工时录入权限"));
    }

    @Test
    void testStats_HoursWorkedNull_Tolerated() {
        // 插入 hoursWorked 为 null 的工时记录
        WhPmWorkLog ghost = new WhPmWorkLog();
        ghost.setProjectId(projectId);
        ghost.setLogDate("2026-03-14");
        ghost.setHoursWorked(null);
        ghost.setWorkDescription("空工时");
        ghost.setStatus("DRAFT");
        ghost.setCreateBy(pmUserId);
        ghost.setDelFlag("0");
        ghost.setVerNo(0);
        workLogDao.insert(ghost);

        var stats = workLogBo.getStats(pmUserId, "2026", 3);
        assertNotNull(stats);
    }

    @Test
    void testEnrichLogs_ProjectMissing_Tolerated() {
        // 记录 A：createBy 正常但 projectId 不存在（覆盖 project == null 分支）
        WhPmWorkLog a = new WhPmWorkLog();
        a.setProjectId("missing-project");
        a.setLogDate("2026-03-15");
        a.setHoursWorked("8");
        a.setWorkDescription("无项目记录");
        a.setStatus("APPROVED");
        a.setCreateBy(pmUserId);
        a.setUpdateBy("ghost_user");
        a.setDelFlag("0");
        a.setVerNo(0);
        workLogDao.insert(a);

        List<WhPmWorkLog> logs = workLogBo.getByUserAndMonth(pmUserId, "2026", 3);
        WhPmWorkLog found = logs.stream().filter(l -> a.getId().equals(l.getId())).findFirst().orElse(null);
        assertNotNull(found);
        assertNull(found.getProjectName());
        assertNull(found.getApproverName());
    }
}
