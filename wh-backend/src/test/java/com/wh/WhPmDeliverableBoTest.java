package com.wh;

import com.wh.bo.pm.DeliverableCreateRequest;
import com.wh.bo.pm.DeliverableUpdateRequest;
import com.wh.bo.pm.WhPmDeliverableBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmDeliverableDao;
import com.wh.entity.pm.WhPmCharter;
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

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.minio.GetObjectArgs;
import io.minio.GetObjectResponse;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
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

    @MockBean
    private MinioClient minioClient;

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

    // ═══════════════════════════════════════════════════════
    //  uploadAttachment 上传附件
    // ═══════════════════════════════════════════════════════

    private MockMultipartFile file(String name) {
        return new MockMultipartFile("file", name, "text/plain", "hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("uploadAttachment - PM 在草稿状态上传成功")
    void testUploadAttachment_Success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("附件上传");

        List<Map<String, Object>> attachments = deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        assertEquals(1, attachments.size());
        assertEquals("a.txt", attachments.get(0).get("fileName"));
        WhPmDeliverable updated = deliverableBo.getById(d.getId());
        assertTrue(updated.getAttachments().contains("a.txt"));
    }

    @Test
    @DisplayName("uploadAttachment - 超过 10 个上限抛异常")
    void testUploadAttachment_OverLimit_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("附件超限");
        for (int i = 0; i < 10; i++) {
            deliverableBo.uploadAttachment(d.getId(), file("file" + i + ".txt"));
        }

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.uploadAttachment(d.getId(), file("overflow.txt")));
        assertTrue(ex.getMessage().contains("上限"));
    }

    @Test
    @DisplayName("uploadAttachment - PENDING_APPROVAL 状态不可上传")
    void testUploadAttachment_Pending_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("审批中不可上传");
        deliverableBo.submit(d.getId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.uploadAttachment(d.getId(), file("a.txt")));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("uploadAttachment - 非项目经理上传失败")
    void testUploadAttachment_NotPm_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("他人不可上传");

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.uploadAttachment(d.getId(), file("a.txt")));
        assertTrue(ex.getMessage().contains("项目经理"));
    }

    @Test
    @DisplayName("uploadAttachment - MinIO 上传失败抛异常")
    void testUploadAttachment_MinioFailure_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class)))
                .thenThrow(new RuntimeException("minio down"));
        WhPmDeliverable d = createDraftDeliverable("上传失败");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.uploadAttachment(d.getId(), file("a.txt")));
        assertTrue(ex.getMessage().contains("上传失败"));
    }

    // ═══════════════════════════════════════════════════════
    //  deleteAttachment 删除附件
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("deleteAttachment - 删除后返回剩余附件")
    void testDeleteAttachment_Success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("删除附件");
        deliverableBo.uploadAttachment(d.getId(), file("b.txt"));
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        // 附件按文件名排序后索引: a.txt=0, b.txt=1
        List<Map<String, Object>> remaining = deliverableBo.deleteAttachment(d.getId(), 0);

        assertEquals(1, remaining.size());
        assertEquals("b.txt", remaining.get(0).get("fileName"));
        // 再次删除索引 0 应删除 b.txt，列表为空
        List<Map<String, Object>> empty = deliverableBo.deleteAttachment(d.getId(), 0);
        assertTrue(empty.isEmpty());
    }

    @Test
    @DisplayName("deleteAttachment - 索引无效抛异常")
    void testDeleteAttachment_InvalidIndex_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("无效索引");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.deleteAttachment(d.getId(), 5));
        assertTrue(ex.getMessage().contains("索引"));
    }

    @Test
    @DisplayName("deleteAttachment - 非草稿状态不可删除")
    void testDeleteAttachment_NotDraft_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("审批中不可删附件");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));
        deliverableBo.submit(d.getId());

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.deleteAttachment(d.getId(), 0));
        assertTrue(ex.getMessage().contains("草稿") || ex.getMessage().contains("驳回"));
    }

    @Test
    @DisplayName("deleteAttachment - 非项目经理删除失败")
    void testDeleteAttachment_NotPm_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("他人不可删附件");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        setCurrentUser(OTHER_USER);

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.deleteAttachment(d.getId(), 0));
        assertTrue(ex.getMessage().contains("项目经理"));
    }

    // ═══════════════════════════════════════════════════════
    //  getAttachmentBytes / getAttachmentFilename
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getAttachmentBytes - 成功读取 MinIO 内容")
    void testGetAttachmentBytes_Success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        GetObjectResponse response = new GetObjectResponse(
                null, null, null, "obj",
                new ByteArrayInputStream("file-content".getBytes(StandardCharsets.UTF_8)));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        WhPmDeliverable d = createDraftDeliverable("读取附件");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        byte[] bytes = deliverableBo.getAttachmentBytes(d.getId(), 0);
        assertEquals("file-content", new String(bytes, StandardCharsets.UTF_8));
        assertEquals("a.txt", deliverableBo.getAttachmentFilename(d.getId(), 0));
    }

    @Test
    @DisplayName("getAttachmentBytes - 索引无效抛异常")
    void testGetAttachmentBytes_InvalidIndex_Throws() {
        WhPmDeliverable d = createDraftDeliverable("无附件索引");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getAttachmentBytes(d.getId(), 0));
        assertTrue(ex.getMessage().contains("索引"));
    }

    @Test
    @DisplayName("getAttachmentBytes - MinIO 读取失败抛异常")
    void testGetAttachmentBytes_MinioFailure_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        WhPmDeliverable d = createDraftDeliverable("读取失败");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getAttachmentBytes(d.getId(), 0));
        assertTrue(ex.getMessage().contains("下载"));
    }

    @Test
    @DisplayName("getAttachmentFilename - 索引无效抛异常")
    void testGetAttachmentFilename_InvalidIndex_Throws() {
        WhPmDeliverable d = createDraftDeliverable("文件名索引");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getAttachmentFilename(d.getId(), 0));
        assertTrue(ex.getMessage().contains("索引"));
    }

    // ═══════════════════════════════════════════════════════
    //  getAttachmentDownloadZip / getZipDownloadFilename
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getAttachmentDownloadZip - 有附件时打包成功")
    void testGetAttachmentDownloadZip_Success() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        GetObjectResponse response = new GetObjectResponse(
                null, null, null, "obj",
                new ByteArrayInputStream("zip-content".getBytes(StandardCharsets.UTF_8)));
        when(minioClient.getObject(any(GetObjectArgs.class))).thenReturn(response);

        WhPmDeliverable d = createDraftDeliverable("打包下载");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        byte[] zip = deliverableBo.getAttachmentDownloadZip(d.getId());
        assertTrue(zip.length > 0);
        // ZIP 魔数 PK
        assertEquals('P', zip[0]);
        assertEquals('K', zip[1]);
    }

    @Test
    @DisplayName("getAttachmentDownloadZip - 无附件抛异常")
    void testGetAttachmentDownloadZip_Empty_Throws() {
        WhPmDeliverable d = createDraftDeliverable("无附件打包");

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getAttachmentDownloadZip(d.getId()));
        assertTrue(ex.getMessage().contains("没有可下载"));
    }

    @Test
    @DisplayName("getAttachmentDownloadZip - MinIO 读取失败抛异常")
    void testGetAttachmentDownloadZip_MinioFailure_Throws() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        when(minioClient.getObject(any(GetObjectArgs.class)))
                .thenThrow(new RuntimeException("minio down"));

        WhPmDeliverable d = createDraftDeliverable("打包失败");
        deliverableBo.uploadAttachment(d.getId(), file("a.txt"));

        ServiceException ex = assertThrows(ServiceException.class,
                () -> deliverableBo.getAttachmentDownloadZip(d.getId()));
        assertTrue(ex.getMessage().contains("读取文件失败"));
    }

    @Test
    @DisplayName("getZipDownloadFilename - 项目有短名时使用短名")
    void testGetZipDownloadFilename_WithShortName() {
        WhPmDeliverable d = createDraftDeliverable("短名项目");

        String filename = deliverableBo.getZipDownloadFilename(d.getId());

        assertTrue(filename.startsWith("T-短名项目-"), "应包含项目短名: " + filename);
        assertTrue(filename.endsWith(".zip"));
    }

    @Test
    @DisplayName("getZipDownloadFilename - 项目不存在时使用 unknown")
    void testGetZipDownloadFilename_NoProject() {
        WhPmDeliverable d = createDraftDeliverable("无项目成果物");
        LambdaUpdateWrapper<WhPmDeliverable> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WhPmDeliverable::getId, d.getId()).set(WhPmDeliverable::getProjectId, "nonexistent-project");
        deliverableDao.update(null, wrapper);

        String filename = deliverableBo.getZipDownloadFilename(d.getId());

        assertTrue(filename.startsWith("unknown-无项目成果物-"), "应使用 unknown: " + filename);
    }

    // ═══════════════════════════════════════════════════════
    //  填充逻辑与附件过滤
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("getById - 项目存在且发起人为真实用户时填充 sponsorName")
    void testGetById_FillsSponsorName() {
        LambdaUpdateWrapper<WhPmCharter> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(WhPmCharter::getId, approvedProjectId)
                .set(WhPmCharter::getSponsorId, "user00000000000000000000000000002");
        charterDao.update(null, wrapper);

        WhPmDeliverable d = createDraftDeliverable("发起人姓名");

        WhPmDeliverable found = deliverableBo.getById(d.getId());
        assertEquals("张伟", found.getSponsorName());
    }

    @Test
    @DisplayName("pageList - 已删除附件从 attachments JSON 中过滤")
    void testPageList_FiltersDeletedAttachments() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("附件过滤");
        deliverableBo.uploadAttachment(d.getId(), file("keep.txt"));
        deliverableBo.uploadAttachment(d.getId(), file("drop.txt"));
        deliverableBo.deleteAttachment(d.getId(), 0); // 删除排序后的 index 0 = drop.txt

        var page = deliverableBo.pageList(1, 10, null, approvedProjectId, null);
        WhPmDeliverable result = page.getRecords().stream()
                .filter(r -> r.getId().equals(d.getId())).findFirst().orElse(null);
        assertNotNull(result);
        assertTrue(result.getAttachments().contains("keep.txt"));
        assertFalse(result.getAttachments().contains("drop.txt"));
    }

    @Test
    @DisplayName("filterActiveAttachments - null 或空 JSON 返回空列表")
    void testFilterActiveAttachments_NullOrEmpty() {
        assertTrue(deliverableBo.filterActiveAttachments(null).isEmpty());
        assertTrue(deliverableBo.filterActiveAttachments("").isEmpty());
        assertTrue(deliverableBo.filterActiveAttachments("not-json").isEmpty());
    }

    // ═══════════════════════════════════════════════════════
    //  补充：空字符串过滤 / 项目缺失 / contentType null / 未登录
    // ═══════════════════════════════════════════════════════

    @Test
    @DisplayName("pageList - 空字符串过滤参数")
    void testPageList_EmptyStrings() {
        createDraftDeliverable("空串过滤");
        var page = deliverableBo.pageList(1, 10, "", "", "");
        assertTrue(page.getTotal() >= 1);
    }

    @Test
    @DisplayName("create - 项目不存在时抛异常")
    void testCreate_ProjectMissing_Throws() {
        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("无项目成果物");
        req.setProjectId("nonexistent-project");
        req.setPlannedDeliveryDate("2026-12-31");

        ServiceException ex = assertThrows(ServiceException.class, () -> deliverableBo.create(req));
        assertTrue(ex.getMessage().contains("项目不存在"));
    }

    @Test
    @DisplayName("create - 未登录时抛 401")
    void testCreate_NotLoggedIn_Throws401() {
        SecurityContextHolder.clearContext();
        DeliverableCreateRequest req = new DeliverableCreateRequest();
        req.setName("未登录成果物");
        req.setProjectId(approvedProjectId);
        req.setPlannedDeliveryDate("2026-12-31");

        ServiceException ex = assertThrows(ServiceException.class, () -> deliverableBo.create(req));
        assertEquals(401, ex.getCode());
        // 恢复登录态，避免影响后续测试
        setCurrentUser(TestFixtures.PM_USER);
    }

    @Test
    @DisplayName("submit - 关联项目被删除时抛异常")
    void testSubmit_ProjectMissing_Throws() {
        WhPmDeliverable d = createDraftDeliverable("项目缺失提交");
        // 将成果物关联到不存在的项目
        com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<WhPmDeliverable> w =
                new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<>();
        w.eq(WhPmDeliverable::getId, d.getId()).set(WhPmDeliverable::getProjectId, "missing-project");
        deliverableDao.update(null, w);

        ServiceException ex = assertThrows(ServiceException.class, () -> deliverableBo.submit(d.getId()));
        assertTrue(ex.getMessage().contains("关联项目不存在"));
    }

    @Test
    @DisplayName("uploadAttachment - 文件 content-type 为空时默认 octet-stream")
    void testUploadAttachment_NullContentType() throws Exception {
        when(minioClient.putObject(any(PutObjectArgs.class))).thenReturn(null);
        WhPmDeliverable d = createDraftDeliverable("无类型附件");
        MockMultipartFile file = new MockMultipartFile(
                "file", "binary.dat", null, new byte[]{1, 2, 3});

        List<Map<String, Object>> attachments = deliverableBo.uploadAttachment(d.getId(), file);
        assertEquals(1, attachments.size());
        assertEquals("binary.dat", attachments.get(0).get("fileName"));
    }
}
