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
}
