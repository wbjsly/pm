package com.wh;

import com.wh.bo.pm.CharterCreateRequest;
import com.wh.bo.pm.CharterUpdateRequest;
import com.wh.bo.pm.WhPmCharterBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.fixtures.TestFixtures;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.vo.pm.CharterStatsVO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
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
@DisplayName("WhPmCharterBo 集成测试")
class WhPmCharterBoTest {

    @Autowired
    private WhPmCharterBo charterBo;

    @Autowired
    private WhPmCharterDao charterDao;

    @Autowired
    private TestFixtures fixtures;

    @org.springframework.beans.factory.annotation.Autowired
    private WhPmWbsElementDao wbsElementDao;

    // static 列表：JUnit 5 PER_METHOD 生命周期下各测试方法共享同一列表
    private static final List<String> createdCharterIds = new ArrayList<>();

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestFixtures.PM_USER, "password", Collections.emptyList()));
        // 清理上一测试的残留数据（避免 @AfterEach 清理与 Spring 上下文关闭冲突导致 SQLITE_BUSY）
        for (String id : createdCharterIds) {
            try { charterDao.deleteById(id); } catch (Exception ignored) {}
        }
        createdCharterIds.clear();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    // 用于 pageList / stats 测试的唯一 PM ID，避免存量数据干扰
    private static final String TEST_PM = "pm_test_" + UUID.randomUUID().toString().substring(0, 8);

    private WhPmCharter createDraftCharter(String name) {
        WhPmCharter charter = fixtures.createTestProject(name);
        createdCharterIds.add(charter.getId());
        return charter;
    }

    private WhPmCharter createTestCharter(String name) {
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName(name);
        req.setProjectCode("T-" + System.nanoTime());
        req.setProjectShortName("T");
        req.setSponsorId(TEST_PM);
        req.setPmId(TEST_PM);
        WhPmCharter charter = charterBo.create(req);
        createdCharterIds.add(charter.getId());
        return charter;
    }

    // ═══════════════════════════════════════════════════════
    //  pageList 分页查询
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("pageList - 无数据时返回空分页结果（使用唯一PM隔离）")
    void testPageList_Empty() {
        var page = charterBo.pageList(1, 10, null, TEST_PM, null, null);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    @DisplayName("pageList - 有数据时返回正确结果集")
    void testPageList_WithData() {
        createTestCharter("项目A");
        createTestCharter("项目B");

        var page = charterBo.pageList(1, 10, null, TEST_PM, null, null);
        assertEquals(2, page.getTotal());
        assertEquals(2, page.getRecords().size());
    }

    @Test
    @DisplayName("pageList - 按状态 DRAFT 筛选")
    void testPageList_FilterByStatus_Draft() {
        createTestCharter("草稿项目");

        var draftPage = charterBo.pageList(1, 10, "DRAFT", TEST_PM, null, null);
        assertEquals(1, draftPage.getTotal());

        var approvedPage = charterBo.pageList(1, 10, "APPROVED", TEST_PM, null, null);
        assertEquals(0, approvedPage.getTotal());
    }

    @Test
    @DisplayName("pageList - 按状态 PENDING_APPROVAL 筛选")
    void testPageList_FilterByStatus_Pending() {
        WhPmCharter charter = createTestCharter("审批中项目");
        charterBo.submit(charter.getId());

        var pendingPage = charterBo.pageList(1, 10, "PENDING_APPROVAL", TEST_PM, null, null);
        assertEquals(1, pendingPage.getTotal());
    }

    @Test
    @DisplayName("pageList - 按关键词筛选项目名称")
    void testPageList_FilterByKeyword() {
        createTestCharter("特殊名称项目");
        createTestCharter("普通项目");

        var page = charterBo.pageList(1, 10, null, TEST_PM, "特殊名称", null);
        assertEquals(1, page.getTotal());
    }

    @Test
    @DisplayName("pageList - 按关键词筛选 charterCode")
    void testPageList_FilterByKeyword_MatchCode() {
        WhPmCharter charter = createTestCharter("编码查询");
        String codePrefix = charter.getCharterCode().substring(0, 10);

        var page = charterBo.pageList(1, 10, null, TEST_PM, codePrefix, null);
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    @DisplayName("pageList - 按 PM 筛选")
    void testPageList_FilterByPmId() {
        createTestCharter("PM项目");

        var page = charterBo.pageList(1, 10, null, TEST_PM, null, null);
        assertEquals(1, page.getTotal());

        var otherPage = charterBo.pageList(1, 10, null, "nonexistent_pm", null, null);
        assertEquals(0, otherPage.getTotal());
    }

    @Test
    @DisplayName("pageList - 按进度筛选（多值逗号分隔）")
    void testPageList_FilterByProgress() {
        CharterCreateRequest req1 = new CharterCreateRequest();
        req1.setProjectName("进行中项目");
        req1.setProjectCode("P1-" + System.nanoTime());
        req1.setProjectShortName("IP");
        req1.setSponsorId(TEST_PM);
        req1.setPmId(TEST_PM);
        WhPmCharter c1 = charterBo.create(req1);
        createdCharterIds.add(c1.getId());

        CharterCreateRequest req2 = new CharterCreateRequest();
        req2.setProjectName("已完成项目");
        req2.setProjectCode("P2-" + System.nanoTime());
        req2.setProjectShortName("CP");
        req2.setSponsorId(TEST_PM);
        req2.setPmId(TEST_PM);
        req2.setProgress("COMPLETED");
        WhPmCharter c2 = charterBo.create(req2);
        createdCharterIds.add(c2.getId());

        var page = charterBo.pageList(1, 10, null, TEST_PM, null, "IN_PROGRESS,COMPLETED");
        assertEquals(2, page.getTotal());

        var single = charterBo.pageList(1, 10, null, TEST_PM, null, "COMPLETED");
        assertEquals(1, single.getTotal());
    }

    @Test
    @DisplayName("pageList - 分页参数正确生效")
    void testPageList_Pagination() {
        for (int i = 0; i < 5; i++) {
            createTestCharter("分页项目" + i);
        }

        var page1 = charterBo.pageList(1, 2, null, TEST_PM, null, null);
        assertEquals(5, page1.getTotal());
        assertEquals(2, page1.getRecords().size());

        var page3 = charterBo.pageList(3, 2, null, TEST_PM, null, null);
        assertEquals(5, page3.getTotal());
        assertEquals(1, page3.getRecords().size());
    }

    // ═══════════════════════════════════════════════════════
    //  getStatsByPmId 统计
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getStatsByPmId - 无项目时各项统计均为 0")
    void testStats_NoData() {
        CharterStatsVO stats = charterBo.getStatsByPmId(TEST_PM);
        assertEquals(0, stats.getTotal());
        assertEquals(0, stats.getDraft());
        assertEquals(0, stats.getPending());
        assertEquals(0, stats.getApproved());
        assertEquals(0, stats.getRejected());
    }

    @Test
    @DisplayName("getStatsByPmId - 仅有 DRAFT 时统计正确")
    void testStats_AllDraft() {
        createTestCharter("项目1");
        createTestCharter("项目2");

        CharterStatsVO stats = charterBo.getStatsByPmId(TEST_PM);
        assertEquals(2, stats.getTotal());
        assertEquals(2, stats.getDraft());
        assertEquals(0, stats.getPending());
        assertEquals(0, stats.getApproved());
        assertEquals(0, stats.getRejected());
    }

    @Test
    @DisplayName("getStatsByPmId - 多种状态混合时统计正确")
    void testStats_MixedStatuses() {
        createTestCharter("草稿项目");

        WhPmCharter approved = createTestCharter("已批准");
        charterBo.submit(approved.getId());
        charterBo.approve(approved.getId(), "通过");

        WhPmCharter rejected = createTestCharter("已驳回");
        charterBo.submit(rejected.getId());
        charterBo.reject(rejected.getId(), "驳回");

        CharterStatsVO stats = charterBo.getStatsByPmId(TEST_PM);
        assertEquals(3, stats.getTotal());
        assertEquals(1, stats.getDraft());
        assertEquals(1, stats.getApproved());
        assertEquals(1, stats.getRejected());
        assertEquals(0, stats.getPending());
    }

    @Test
    @DisplayName("getStatsByPmId - 不存在的 PM 返回全零统计")
    void testStats_UnknownPm() {
        CharterStatsVO stats = charterBo.getStatsByPmId("nonexistent_user");
        assertEquals(0, stats.getTotal());
    }

    // ═══════════════════════════════════════════════════════
    //  getById 查询
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getById - 存在的章程返回实体")
    void testGetById_Success() {
        WhPmCharter created = createDraftCharter("查找测试");
        WhPmCharter found = charterBo.getById(created.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("DRAFT", found.getStatus());
        assertNotNull(found.getCharterCode());
    }

    @Test
    @DisplayName("getById - 不存在的 ID 抛出 404")
    void testGetById_NotFound() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.getById("nonexistent_id"));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("getById - 已物理删除的章程抛出 404")
    void testGetById_AfterPhysicalDelete() {
        WhPmCharter charter = createDraftCharter("待删除");
        String id = charter.getId();
        charterBo.delete(id);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.getById(id));
        assertEquals(404, ex.getCode());
    }

    // ═══════════════════════════════════════════════════════
    //  getByIdSilent 静默查询
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getByIdSilent - 存在的章程返回实体")
    void testGetByIdSilent_Success() {
        WhPmCharter created = createDraftCharter("静默查询");
        WhPmCharter found = charterBo.getByIdSilent(created.getId());
        assertNotNull(found);
        assertEquals(created.getId(), found.getId());
    }

    @Test
    @DisplayName("getByIdSilent - 不存在的 ID 返回 null")
    void testGetByIdSilent_NotFound() {
        assertNull(charterBo.getByIdSilent("nonexistent_id"));
    }

    @Test
    @DisplayName("getByIdSilent - 已物理删除的返回 null")
    void testGetByIdSilent_AfterPhysicalDelete() {
        WhPmCharter charter = createDraftCharter("物理删除");
        String id = charter.getId();
        charterBo.delete(id);

        assertNull(charterBo.getByIdSilent(id));
    }

    @Test
    @DisplayName("getByIdSilent - null 入参返回 null")
    void testGetByIdSilent_NullId() {
        assertNull(charterBo.getByIdSilent(null));
    }

    @Test
    @DisplayName("getByIdSilent - 空字符串入参返回 null")
    void testGetByIdSilent_EmptyId() {
        assertNull(charterBo.getByIdSilent(""));
    }

    // ═══════════════════════════════════════════════════════
    //  create 创建
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("create - 默认进度创建成功，自动生成编码，状态 DRAFT")
    void testCreate_DefaultProgress() {
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName("默认进度项目");
        req.setProjectCode("DEF-" + System.currentTimeMillis());
        req.setProjectShortName("DEF");
        req.setSponsorId("sponsor_1");
        req.setPmId("pm_1");

        WhPmCharter result = charterBo.create(req);
        createdCharterIds.add(result.getId());

        assertNotNull(result.getId());
        assertNotNull(result.getCharterCode());
        assertTrue(result.getCharterCode().startsWith("CHARTER-"));
        assertEquals("DRAFT", result.getStatus());
        assertEquals("IN_PROGRESS", result.getProgress());
        assertEquals("默认进度项目", result.getProjectName());

        // 通过 DAO 验证持久化
        WhPmCharter saved = charterDao.selectById(result.getId());
        assertNotNull(saved);
        assertEquals("DRAFT", saved.getStatus());
    }

    @Test
    @DisplayName("create - 指定进度值创建成功")
    void testCreate_WithSpecificProgress() {
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName("指定进度项目");
        req.setProjectCode("SPC-" + System.currentTimeMillis());
        req.setProjectShortName("SPC");
        req.setSponsorId("sponsor_2");
        req.setPmId("pm_2");
        req.setProgress("COMPLETED");

        WhPmCharter result = charterBo.create(req);
        createdCharterIds.add(result.getId());

        assertEquals("COMPLETED", result.getProgress());
    }

    @Test
    @DisplayName("create - 设置全部可选字段")
    void testCreate_WithAllFields() {
        CharterCreateRequest req = new CharterCreateRequest();
        req.setProjectName("完整字段项目");
        req.setProjectCode("ALL-" + System.currentTimeMillis());
        req.setProjectShortName("ALL");
        req.setDescription("测试描述");
        req.setObjectives("测试目标");
        req.setScopeSummary("测试范围");
        req.setSponsorId("sponsor_3");
        req.setPmId("pm_3");
        req.setBudgetCap("500000");
        req.setStartDate("2026-01-01");
        req.setEndDate("2026-12-31");
        req.setKeyStakeholders("张三,李四");
        req.setRemarks("测试备注");
        req.setContractNo("CON-001");
        req.setProjectCategory("IT");
        req.setOutputValueTaxable("100000");
        req.setOutputValueExcludingTax("90000");
        req.setTaxRate("0.1");
        req.setTaxAmount("10000");
        req.setProgress("IN_PROGRESS");

        WhPmCharter result = charterBo.create(req);
        createdCharterIds.add(result.getId());

        assertEquals("测试描述", result.getDescription());
        assertEquals("500000", result.getBudgetCap());
        assertEquals("2026-01-01", result.getStartDate());
        assertEquals("2026-12-31", result.getEndDate());
        assertEquals("CON-001", result.getContractNo());
        assertEquals("IT", result.getProjectCategory());
    }

    // ═══════════════════════════════════════════════════════
    //  update 更新
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("update - DRAFT 状态可更新")
    void testUpdate_Draft_Success() {
        WhPmCharter charter = createDraftCharter("原始名称");

        CharterUpdateRequest req = new CharterUpdateRequest();
        req.setProjectName("更新后名称");
        req.setProjectCode("UPD-" + System.currentTimeMillis());
        req.setProjectShortName("UPD");
        req.setSponsorId(TestFixtures.PM_USER);
        req.setPmId(TestFixtures.PM_USER);

        charterBo.update(charter.getId(), req);

        WhPmCharter updated = charterBo.getById(charter.getId());
        assertEquals("更新后名称", updated.getProjectName());
        assertEquals("DRAFT", updated.getStatus());
    }

    @Test
    @DisplayName("update - REJECTED 状态可更新")
    void testUpdate_Rejected_Success() {
        WhPmCharter charter = createDraftCharter("驳回后更新");
        charterBo.submit(charter.getId());
        charterBo.reject(charter.getId(), "请修改");

        CharterUpdateRequest req = new CharterUpdateRequest();
        req.setProjectName("已驳回更新");
        req.setProjectCode("REJ-" + System.currentTimeMillis());
        req.setProjectShortName("REJ");
        req.setSponsorId(TestFixtures.PM_USER);
        req.setPmId(TestFixtures.PM_USER);

        charterBo.update(charter.getId(), req);

        WhPmCharter updated = charterBo.getById(charter.getId());
        assertEquals("已驳回更新", updated.getProjectName());
        assertEquals("REJECTED", updated.getStatus());
    }

    @Test
    @DisplayName("update - APPROVED 状态不可更新")
    void testUpdate_Approved_Throws() {
        WhPmCharter charter = createDraftCharter("已审批不可改");
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "审批通过");

        CharterUpdateRequest req = new CharterUpdateRequest();
        req.setProjectName("不应成功");
        req.setProjectCode("FAIL");
        req.setProjectShortName("F");
        req.setSponsorId(TestFixtures.PM_USER);
        req.setPmId(TestFixtures.PM_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.update(charter.getId(), req));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("update - PENDING_APPROVAL 状态不可更新")
    void testUpdate_Pending_Throws() {
        WhPmCharter charter = createDraftCharter("审批中不可改");
        charterBo.submit(charter.getId());

        CharterUpdateRequest req = new CharterUpdateRequest();
        req.setProjectName("不应成功");
        req.setProjectCode("FAIL");
        req.setProjectShortName("F");
        req.setSponsorId(TestFixtures.PM_USER);
        req.setPmId(TestFixtures.PM_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.update(charter.getId(), req));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    // ═══════════════════════════════════════════════════════
    //  delete 删除
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("delete - DRAFT 状态可物理删除")
    void testDelete_Draft_Success() {
        WhPmCharter charter = createDraftCharter("待删除");
        String id = charter.getId();

        charterBo.delete(id);

        // 物理删除后 selectById 返回 null
        assertNull(charterDao.selectById(id));
    }

    @Test
    @DisplayName("delete - PENDING_APPROVAL 不可删除")
    void testDelete_Pending_Throws() {
        WhPmCharter charter = createDraftCharter("审批中不可删");
        charterBo.submit(charter.getId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.delete(charter.getId()));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    @Test
    @DisplayName("delete - APPROVED 不可删除")
    void testDelete_Approved_Throws() {
        WhPmCharter charter = createDraftCharter("已审批不可删");
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.delete(charter.getId()));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    // ═══════════════════════════════════════════════════════
    //  submit 提交审批
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("submit - DRAFT 提交后变为 PENDING_APPROVAL")
    void testSubmit_Draft_Success() {
        WhPmCharter charter = createDraftCharter("提交审批");
        charterBo.submit(charter.getId());

        WhPmCharter updated = charterBo.getById(charter.getId());
        assertEquals("PENDING_APPROVAL", updated.getStatus());
        assertNotNull(updated.getProcessInstanceId());
    }

    @Test
    @DisplayName("submit - REJECTED 可重新提交")
    void testSubmit_Rejected_Success() {
        WhPmCharter charter = createDraftCharter("驳回重提");
        charterBo.submit(charter.getId());
        charterBo.reject(charter.getId(), "驳回");

        charterBo.submit(charter.getId());

        WhPmCharter updated = charterBo.getById(charter.getId());
        assertEquals("PENDING_APPROVAL", updated.getStatus());
        assertNotNull(updated.getProcessInstanceId());
    }

    @Test
    @DisplayName("submit - APPROVED 状态抛出异常")
    void testSubmit_Approved_Throws() {
        WhPmCharter charter = createDraftCharter("已审批不可提");
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.submit(charter.getId()));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("submit - PENDING_APPROVAL 重复提交异常")
    void testSubmit_Pending_Throws() {
        WhPmCharter charter = createDraftCharter("审批中不可再提");
        charterBo.submit(charter.getId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.submit(charter.getId()));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    // ═══════════════════════════════════════════════════════
    //  approve 审批通过
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("approve - PENDING_APPROVAL 通过后变为 APPROVED")
    void testApprove_Success() {
        WhPmCharter charter = createDraftCharter("审批通过测试");
        charterBo.submit(charter.getId());

        charterBo.approve(charter.getId(), "同意立项");

        WhPmCharter approved = charterBo.getById(charter.getId());
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("同意立项", approved.getApprovalComment());
        assertNotNull(approved.getProcessInstanceId());
    }

    @Test
    @DisplayName("approve - DRAFT 状态抛出异常")
    void testApprove_Draft_Throws() {
        WhPmCharter charter = createDraftCharter("草稿不可审批");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.approve(charter.getId(), "通过"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("approve - APPROVED 状态抛出异常")
    void testApprove_Approved_Throws() {
        WhPmCharter charter = createDraftCharter("已审批不可再批");
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.approve(charter.getId(), "再次审批"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("approve - REJECTED 状态抛出异常")
    void testApprove_Rejected_Throws() {
        WhPmCharter charter = createDraftCharter("已驳回不可批");
        charterBo.submit(charter.getId());
        charterBo.reject(charter.getId(), "驳回");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.approve(charter.getId(), "通过"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    // ═══════════════════════════════════════════════════════
    //  reject 驳回
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("reject - PENDING_APPROVAL 驳回后变为 REJECTED")
    void testReject_Success() {
        WhPmCharter charter = createDraftCharter("驳回测试");
        charterBo.submit(charter.getId());

        charterBo.reject(charter.getId(), "预算需调整");

        WhPmCharter rejected = charterBo.getById(charter.getId());
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("预算需调整", rejected.getApprovalComment());
        assertNull(rejected.getProcessInstanceId());
    }

    @Test
    @DisplayName("reject - DRAFT 状态抛出异常")
    void testReject_Draft_Throws() {
        WhPmCharter charter = createDraftCharter("草稿不可驳回");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.reject(charter.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("reject - APPROVED 状态抛出异常")
    void testReject_Approved_Throws() {
        WhPmCharter charter = createDraftCharter("已审批不可驳");
        charterBo.submit(charter.getId());
        charterBo.approve(charter.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> charterBo.reject(charter.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    // ═══════════════════════════════════════════════════════
    //  完整生命周期
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("submit → reject → update → submit → approve 完整生命周期")
    void testFullLifecycle() {
        WhPmCharter charter = createDraftCharter("完整生命周期");

        // 第一步：提交审批
        charterBo.submit(charter.getId());
        assertEquals("PENDING_APPROVAL", charterBo.getById(charter.getId()).getStatus());

        // 第二步：驳回
        charterBo.reject(charter.getId(), "需要修改预算");
        WhPmCharter rejected = charterBo.getById(charter.getId());
        assertEquals("REJECTED", rejected.getStatus());
        assertNull(rejected.getProcessInstanceId());

        // 第三步：修改
        CharterUpdateRequest updateReq = new CharterUpdateRequest();
        updateReq.setProjectName("修改后重提");
        updateReq.setProjectCode("CYCLE-" + System.currentTimeMillis());
        updateReq.setProjectShortName("CYC");
        updateReq.setSponsorId(TestFixtures.PM_USER);
        updateReq.setPmId(TestFixtures.PM_USER);
        charterBo.update(charter.getId(), updateReq);

        // 第四步：重新提交
        charterBo.submit(charter.getId());
        assertEquals("PENDING_APPROVAL", charterBo.getById(charter.getId()).getStatus());

        // 第五步：审批通过
        charterBo.approve(charter.getId(), "最终审批通过");
        WhPmCharter approved = charterBo.getById(charter.getId());
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("最终审批通过", approved.getApprovalComment());
    }

    // ═══════════════════════════════════════════════════════
    //  补充：WBS 工作量非数字容错 / 空字符串过滤 / 统计边界
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("pageList - WBS effortEstimate 非数字时容错统计")
    void testPageList_NonNumericEffort() {
        WhPmCharter charter = createDraftCharter("非数字工作量");

        com.wh.entity.pm.WhPmWbsElement e = new com.wh.entity.pm.WhPmWbsElement();
        e.setProjectId(charter.getId());
        e.setWbsCode("WBS-BAD-" + System.nanoTime());
        e.setLevel(1);
        e.setName("非法工作量");
        e.setElementType("TASK");
        e.setEffortEstimate("abc");
        e.setStatus("NOT_STARTED");
        e.setDelFlag("0");
        e.setVerNo(0);
        wbsElementDao.insert(e);

        var page = charterBo.pageList(1, 10, null, null, null, null);
        assertNotNull(page);
        com.wh.entity.pm.WhPmCharter found = page.getRecords().stream()
                .filter(c -> charter.getId().equals(c.getId())).findFirst().orElse(null);
        assertNotNull(found);
    }

    @Test
    @DisplayName("pageList - 空字符串过滤参数视为无条件")
    void testPageList_EmptyStrings() {
        WhPmCharter charter = createDraftCharter("空字符串过滤");
        var page = charterBo.pageList(1, 10, "", "", "", "");
        assertNotNull(page);
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    @DisplayName("pageList - WBS 节点 effort 为空且 actualEndDate 有值时统计")
    void testPageList_WbsNullEffortWithActualEnd() {
        WhPmCharter charter = createDraftCharter("WBS统计边界");

        com.wh.entity.pm.WhPmWbsElement e = new com.wh.entity.pm.WhPmWbsElement();
        e.setProjectId(charter.getId());
        e.setWbsCode("WBS-END-" + System.nanoTime());
        e.setLevel(1);
        e.setName("有实际完成日期");
        e.setElementType("TASK");
        e.setEffortEstimate(null);
        e.setLatestPlannedEndDate(null);
        e.setActualEndDate("2026-06-30");
        e.setStatus("COMPLETED");
        e.setDelFlag("0");
        e.setVerNo(0);
        wbsElementDao.insert(e);

        var page = charterBo.pageList(1, 10, null, null, null, null);
        com.wh.entity.pm.WhPmCharter found = page.getRecords().stream()
                .filter(c -> charter.getId().equals(c.getId())).findFirst().orElse(null);
        assertNotNull(found);
        assertEquals("2026-06-30", found.getWbsLatestEndDate());
        assertNull(found.getWbsTotalEffort());
    }

    @Test
    @DisplayName("getStatsByPmId - 包含 REJECTED 状态统计")
    void testStats_WithRejected() {
        WhPmCharter charter = createTestCharter("驳回统计");
        charterBo.submit(charter.getId());
        charterBo.reject(charter.getId(), "驳回");

        var stats = charterBo.getStatsByPmId(TEST_PM);
        assertTrue(stats.getRejected() >= 1);
    }

    @Test
    @DisplayName("pageList - 多个 WBS 取最新结束日期")
    void testPageList_MultipleWbs_LatestEndDate() {
        WhPmCharter charter = createDraftCharter("多WBS日期");
        String projectId = charter.getId();

        com.wh.entity.pm.WhPmWbsElement e1 = new com.wh.entity.pm.WhPmWbsElement();
        e1.setProjectId(projectId);
        e1.setWbsCode("WBS-L1-" + System.nanoTime());
        e1.setLevel(1);
        e1.setName("较早");
        e1.setElementType("TASK");
        e1.setEffortEstimate("10");
        e1.setLatestPlannedEndDate("2026-03-01");
        e1.setStatus("NOT_STARTED");
        e1.setDelFlag("0");
        e1.setVerNo(0);
        wbsElementDao.insert(e1);

        com.wh.entity.pm.WhPmWbsElement e2 = new com.wh.entity.pm.WhPmWbsElement();
        e2.setProjectId(projectId);
        e2.setWbsCode("WBS-L2-" + System.nanoTime());
        e2.setLevel(1);
        e2.setName("较晚");
        e2.setElementType("TASK");
        e2.setEffortEstimate("20");
        e2.setLatestPlannedEndDate("2026-09-15");
        e2.setStatus("NOT_STARTED");
        e2.setDelFlag("0");
        e2.setVerNo(0);
        wbsElementDao.insert(e2);

        var page = charterBo.pageList(1, 10, null, null, null, null);
        com.wh.entity.pm.WhPmCharter found = page.getRecords().stream()
                .filter(c -> charter.getId().equals(c.getId())).findFirst().orElse(null);
        assertNotNull(found);
        assertEquals("2026-09-15", found.getWbsLatestEndDate());
        assertEquals(30.0, found.getWbsTotalEffort().doubleValue(), 0.001);
    }
}
