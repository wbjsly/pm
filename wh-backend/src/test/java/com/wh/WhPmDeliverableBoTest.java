package com.wh;

import com.wh.bo.pm.DeliverableCreateRequest;
import com.wh.bo.pm.DeliverableUpdateRequest;
import com.wh.bo.pm.WhPmDeliverableBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmDeliverableDao;
import com.wh.entity.pm.WhPmDeliverable;
import com.wh.fixtures.TestFixtures;
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

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@DisplayName("WhPmDeliverableBo 集成测试")
class WhPmDeliverableBoTest {

    private static final String OTHER_USER = "other_user_001";

    @Autowired
    private WhPmDeliverableBo deliverableBo;

    @Autowired
    private WhPmDeliverableDao deliverableDao;

    @Autowired
    private WhPmCharterDao charterDao;

    @Autowired
    private TestFixtures fixtures;

    private final List<String> createdCharterIds = new ArrayList<>();
    private final List<String> createdDeliverableIds = new ArrayList<>();
    private String approvedProjectId;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(TestFixtures.PM_USER, "password", Collections.emptyList()));

        // 创建一个已审批的项目供成果物测试使用
        var approved = fixtures.createApprovedProject("测试项目-成果物");
        approvedProjectId = approved.getId();
        createdCharterIds.add(approvedProjectId);
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
        createdDeliverableIds.clear();
        createdCharterIds.clear();
    }

    private void setCurrentUser(String userId) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(userId, "password", Collections.emptyList()));
    }

    private WhPmDeliverable createDraftDeliverable(String name) {
        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName(name);
        req.setProjectId(approvedProjectId);
        req.setPlannedDeliveryDate("2026-12-31");
        WhPmDeliverable d = deliverableBo.create(req);
        createdDeliverableIds.add(d.getId());
        return d;
    }

    // ═══════════════════════════════════════════════════════
    //  pageList 分页查询
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("pageList - 无数据时返回空分页")
    void testPageList_Empty() {
        var page = deliverableBo.pageList(1, 10, null, approvedProjectId, null);
        assertEquals(0, page.getTotal());
        assertTrue(page.getRecords().isEmpty());
    }

    @Test
    @DisplayName("pageList - 有数据时返回正确结果")
    void testPageList_WithData() {
        createDraftDeliverable("成果物A");
        createDraftDeliverable("成果物B");

        var page = deliverableBo.pageList(1, 10, null, approvedProjectId, null);
        assertEquals(2, page.getTotal());
    }

    @Test
    @DisplayName("pageList - 按状态 DRAFT 筛选")
    void testPageList_FilterByStatus() {
        createDraftDeliverable("草稿成果物");

        var draftPage = deliverableBo.pageList(1, 10, "DRAFT", approvedProjectId, null);
        assertEquals(1, draftPage.getTotal());

        var approvedPage = deliverableBo.pageList(1, 10, "APPROVED", approvedProjectId, null);
        assertEquals(0, approvedPage.getTotal());
    }

    @Test
    @DisplayName("pageList - 按项目 ID 筛选")
    void testPageList_FilterByProjectId() {
        createDraftDeliverable("项目成果物");

        var page = deliverableBo.pageList(1, 10, null, approvedProjectId, null);
        assertTrue(page.getTotal() >= 1);

        var otherPage = deliverableBo.pageList(1, 10, null, "nonexistent_project", null);
        assertEquals(0, otherPage.getTotal());
    }

    @Test
    @DisplayName("pageList - 按名称关键词筛选")
    void testPageList_FilterByKeyword() {
        createDraftDeliverable("特殊名称成果物");
        createDraftDeliverable("普通成果物");

        var page = deliverableBo.pageList(1, 10, null, null, "特殊名称");
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    @DisplayName("pageList - 按 deliverableCode 关键词筛选")
    void testPageList_FilterByKeyword_MatchCode() {
        WhPmDeliverable d = createDraftDeliverable("编码查询");
        String codePrefix = d.getDeliverableCode().substring(0, 12);

        var page = deliverableBo.pageList(1, 10, null, null, codePrefix);
        assertTrue(page.getTotal() >= 1);
    }

    // ═══════════════════════════════════════════════════════
    //  getById 查询
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getById - 存在的成果物返回实体")
    void testGetById_Success() {
        WhPmDeliverable created = createDraftDeliverable("查找测试");
        WhPmDeliverable found = deliverableBo.getById(created.getId());
        assertEquals(created.getId(), found.getId());
        assertEquals("DRAFT", found.getStatus());
        assertNotNull(found.getDeliverableCode());
        assertEquals("查找测试", found.getName());
    }

    @Test
    @DisplayName("getById - 不存在的 ID 抛出 404")
    void testGetById_NotFound() {
        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getById("nonexistent_id"));
        assertEquals(404, ex.getCode());
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("getById - 已逻辑删除的成果物抛出 404")
    void testGetById_AfterLogicalDelete() {
        WhPmDeliverable d = createDraftDeliverable("待删除");
        String id = d.getId();
        deliverableBo.delete(id);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getById(id));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    // ═══════════════════════════════════════════════════════
    //  create 创建
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("create - PM 在已审批项目上创建成功")
    void testCreate_Success() {
        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("新成果物");
        req.setProjectId(approvedProjectId);
        req.setPlannedDeliveryDate("2026-12-31");

        WhPmDeliverable result = deliverableBo.create(req);
        createdDeliverableIds.add(result.getId());

        assertNotNull(result.getId());
        assertNotNull(result.getDeliverableCode());
        assertTrue(result.getDeliverableCode().startsWith("DELIVERABLE-"));
        assertEquals("DRAFT", result.getStatus());
        assertEquals("新成果物", result.getName());
        assertEquals(approvedProjectId, result.getProjectId());
    }

    @Test
    @DisplayName("create - 项目未审批时创建失败")
    void testCreate_ProjectNotApproved_Throws() {
        var draftCharter = fixtures.createTestProject("未审批项目");
        createdCharterIds.add(draftCharter.getId());

        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("不应创建");
        req.setProjectId(draftCharter.getId());
        req.setPlannedDeliveryDate("2026-12-31");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.create(req));
        assertTrue(ex.getMessage().contains("已审批通过"));
    }

    @Test
    @DisplayName("create - 非项目经理创建失败")
    void testCreate_NotPm_Throws() {
        setCurrentUser(OTHER_USER);

        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("不应创建");
        req.setProjectId(approvedProjectId);
        req.setPlannedDeliveryDate("2026-12-31");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.create(req));
        assertTrue(ex.getMessage().contains("项目经理"));
    }

    @Test
    @DisplayName("create - 已逻辑删除的项目不可创建")
    void testCreate_ProjectLogicalDeleted_Throws() {
        charterDao.deleteById(approvedProjectId);

        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("不应创建");
        req.setProjectId(approvedProjectId);
        req.setPlannedDeliveryDate("2026-12-31");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.create(req));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    // ═══════════════════════════════════════════════════════
    //  update 更新
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("update - DRAFT 状态可更新")
    void testUpdate_Draft_Success() {
        WhPmDeliverable d = createDraftDeliverable("原始名称");

        DeliverableUpdateRequest req = new DeliverableUpdateRequest();
        req.setName("更新名称");
        req.setDescription("更新描述");
        req.setPlannedDeliveryDate("2026-06-30");

        deliverableBo.update(d.getId(), req);

        WhPmDeliverable updated = deliverableBo.getById(d.getId());
        assertEquals("更新名称", updated.getName());
        assertEquals("更新描述", updated.getDescription());
        assertEquals("DRAFT", updated.getStatus());
    }

    @Test
    @DisplayName("update - REJECTED 状态可更新")
    void testUpdate_Rejected_Success() {
        WhPmDeliverable d = createDraftDeliverable("被驳回复改");
        deliverableBo.submit(d.getId());
        deliverableBo.reject(d.getId(), "需修改");

        assertEquals("REJECTED", deliverableBo.getById(d.getId()).getStatus());

        DeliverableUpdateRequest req = new DeliverableUpdateRequest();
        req.setName("驳回后更新");
        req.setDescription("修改完成");

        deliverableBo.update(d.getId(), req);

        WhPmDeliverable updated = deliverableBo.getById(d.getId());
        assertEquals("驳回后更新", updated.getName());
        assertEquals("REJECTED", updated.getStatus());
    }

    @Test
    @DisplayName("update - APPROVED 状态不可更新")
    void testUpdate_Approved_Throws() {
        WhPmDeliverable d = createDraftDeliverable("已审批不可改");
        deliverableBo.submit(d.getId());
        deliverableBo.approve(d.getId(), "通过");

        DeliverableUpdateRequest req = new DeliverableUpdateRequest();
        req.setName("不应成功");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.update(d.getId(), req));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("update - PENDING_APPROVAL 状态不可更新")
    void testUpdate_Pending_Throws() {
        WhPmDeliverable d = createDraftDeliverable("审批中不可改");
        deliverableBo.submit(d.getId());

        DeliverableUpdateRequest req = new DeliverableUpdateRequest();
        req.setName("不应成功");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.update(d.getId(), req));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("update - 非项目经理更新失败")
    void testUpdate_NotPm_Throws() {
        WhPmDeliverable d = createDraftDeliverable("他人不可改");

        setCurrentUser(OTHER_USER);

        DeliverableUpdateRequest req = new DeliverableUpdateRequest();
        req.setName("不应成功");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.update(d.getId(), req));
        assertTrue(ex.getMessage().contains("项目经理"));
    }

    // ═══════════════════════════════════════════════════════
    //  delete 删除
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("delete - DRAFT 状态可删除")
    void testDelete_Draft_Success() {
        WhPmDeliverable d = createDraftDeliverable("待删除");
        String id = d.getId();

        deliverableBo.delete(id);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getById(id));
        assertTrue(ex.getMessage().contains("不存在"));
    }

    @Test
    @DisplayName("delete - PENDING_APPROVAL 不可删除")
    void testDelete_Pending_Throws() {
        WhPmDeliverable d = createDraftDeliverable("审批中不可删");
        deliverableBo.submit(d.getId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.delete(d.getId()));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    @Test
    @DisplayName("delete - APPROVED 不可删除")
    void testDelete_Approved_Throws() {
        WhPmDeliverable d = createDraftDeliverable("已审批不可删");
        deliverableBo.submit(d.getId());
        deliverableBo.approve(d.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.delete(d.getId()));
        assertTrue(ex.getMessage().contains("草稿"));
    }

    @Test
    @DisplayName("delete - 非项目经理删除失败")
    void testDelete_NotPm_Throws() {
        WhPmDeliverable d = createDraftDeliverable("他人不可删");

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.delete(d.getId()));
        assertTrue(ex.getMessage().contains("项目经理"));
    }

    // ═══════════════════════════════════════════════════════
    //  submit 提交审批
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("submit - PM 提交 DRAFT 成果物变为 PENDING_APPROVAL")
    void testSubmit_Draft_Success() {
        WhPmDeliverable d = createDraftDeliverable("提交审批");

        deliverableBo.submit(d.getId());

        WhPmDeliverable updated = deliverableBo.getById(d.getId());
        assertEquals("PENDING_APPROVAL", updated.getStatus());
        assertNotNull(updated.getProcessInstanceId());
    }

    @Test
    @DisplayName("submit - REJECTED 可重新提交")
    void testSubmit_Rejected_Success() {
        WhPmDeliverable d = createDraftDeliverable("驳回重提");
        deliverableBo.submit(d.getId());
        deliverableBo.reject(d.getId(), "驳回");

        deliverableBo.submit(d.getId());

        WhPmDeliverable updated = deliverableBo.getById(d.getId());
        assertEquals("PENDING_APPROVAL", updated.getStatus());
        assertNotNull(updated.getProcessInstanceId());
    }

    @Test
    @DisplayName("submit - APPROVED 状态抛出异常")
    void testSubmit_Approved_Throws() {
        WhPmDeliverable d = createDraftDeliverable("已审批不可提");
        deliverableBo.submit(d.getId());
        deliverableBo.approve(d.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.submit(d.getId()));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("submit - 非 PM 提交失败")
    void testSubmit_NotPm_Throws() {
        WhPmDeliverable d = createDraftDeliverable("他人不可提");

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.submit(d.getId()));
        assertTrue(ex.getMessage().contains("项目经理"));
    }

    // ═══════════════════════════════════════════════════════
    //  approve 审批通过
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("approve - 发起人审批通过 PENDING_APPROVAL 变为 APPROVED")
    void testApprove_Success() {
        WhPmDeliverable d = createDraftDeliverable("审批通过");
        deliverableBo.submit(d.getId());

        deliverableBo.approve(d.getId(), "验收通过");

        WhPmDeliverable approved = deliverableBo.getById(d.getId());
        assertEquals("APPROVED", approved.getStatus());
        assertEquals("验收通过", approved.getApprovalComment());
    }

    @Test
    @DisplayName("approve - DRAFT 状态审批失败")
    void testApprove_Draft_Throws() {
        WhPmDeliverable d = createDraftDeliverable("草稿不可审批");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.approve(d.getId(), "通过"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("approve - REJECTED 状态审批失败")
    void testApprove_Rejected_Throws() {
        WhPmDeliverable d = createDraftDeliverable("驳回不可审批");
        deliverableBo.submit(d.getId());
        deliverableBo.reject(d.getId(), "驳回");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.approve(d.getId(), "通过"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("approve - 非发起人审批失败")
    void testApprove_NotSponsor_Throws() {
        WhPmDeliverable d = createDraftDeliverable("他人不可审批");
        deliverableBo.submit(d.getId());

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.approve(d.getId(), "通过"));
        assertTrue(ex.getMessage().contains("发起人"));
    }

    // ═══════════════════════════════════════════════════════
    //  reject 驳回
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("reject - 发起人驳回 PENDING_APPROVAL 变为 REJECTED")
    void testReject_Success() {
        WhPmDeliverable d = createDraftDeliverable("驳回测试");
        deliverableBo.submit(d.getId());

        deliverableBo.reject(d.getId(), "不符合要求");

        WhPmDeliverable rejected = deliverableBo.getById(d.getId());
        assertEquals("REJECTED", rejected.getStatus());
        assertEquals("不符合要求", rejected.getApprovalComment());
        assertNull(rejected.getProcessInstanceId());
    }

    @Test
    @DisplayName("reject - DRAFT 状态驳回失败")
    void testReject_Draft_Throws() {
        WhPmDeliverable d = createDraftDeliverable("草稿不可驳回");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.reject(d.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("reject - APPROVED 状态驳回失败")
    void testReject_Approved_Throws() {
        WhPmDeliverable d = createDraftDeliverable("已审批不可驳");
        deliverableBo.submit(d.getId());
        deliverableBo.approve(d.getId(), "通过");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.reject(d.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("审批"));
    }

    @Test
    @DisplayName("reject - 非发起人驳回失败")
    void testReject_NotSponsor_Throws() {
        WhPmDeliverable d = createDraftDeliverable("他人不可驳回");
        deliverableBo.submit(d.getId());

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.reject(d.getId(), "驳回"));
        assertTrue(ex.getMessage().contains("发起人"));
    }

    // ═══════════════════════════════════════════════════════
    //  markDelivered 标记交付
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("markDelivered - PM 标记 APPROVED 为 DELIVERED")
    void testMarkDelivered_Success() {
        WhPmDeliverable d = createDraftDeliverable("交付测试");
        deliverableBo.submit(d.getId());
        deliverableBo.approve(d.getId(), "通过");

        deliverableBo.markDelivered(d.getId());

        WhPmDeliverable delivered = deliverableBo.getById(d.getId());
        assertEquals("DELIVERED", delivered.getStatus());
        assertNotNull(delivered.getActualDeliveryDate());
    }

    @Test
    @DisplayName("markDelivered - DRAFT 状态不可标记交付")
    void testMarkDelivered_Draft_Throws() {
        WhPmDeliverable d = createDraftDeliverable("草稿不可交付");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.markDelivered(d.getId()));
        assertTrue(ex.getMessage().contains("已审批通过"));
    }

    @Test
    @DisplayName("markDelivered - PENDING_APPROVAL 不可标记交付")
    void testMarkDelivered_Pending_Throws() {
        WhPmDeliverable d = createDraftDeliverable("审批中不可交付");
        deliverableBo.submit(d.getId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.markDelivered(d.getId()));
        assertTrue(ex.getMessage().contains("已审批通过"));
    }

    @Test
    @DisplayName("markDelivered - 非 PM 标记交付失败")
    void testMarkDelivered_NotPm_Throws() {
        WhPmDeliverable d = createDraftDeliverable("他人不可交付");
        deliverableBo.submit(d.getId());
        deliverableBo.approve(d.getId(), "通过");

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.markDelivered(d.getId()));
        assertTrue(ex.getMessage().contains("项目经理"));
    }
}
