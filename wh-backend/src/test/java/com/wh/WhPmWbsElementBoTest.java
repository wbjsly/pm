package com.wh;

import com.wh.bo.pm.*;
import com.wh.common.ServiceException;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.entity.pm.WhPmWbsVersion;
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
@DisplayName("WBS节点 BO 测试")
class WhPmWbsElementBoTest {

    @Autowired
    private WhPmWbsElementBo wbsBo;

    @Autowired
    private TestFixtures fixtures;

    private String projectId;

    @BeforeEach
    void setUp() {
        WhPmCharter charter = fixtures.createTestProject("WBS测试项目");
        projectId = charter.getId();
    }

    private WhPmWbsElement createWbs(String name) {
        WbsCreateRequest req = new WbsCreateRequest();
        req.setProjectId(projectId);
        req.setName(name);
        req.setElementType("TASK");
        req.setEffortEstimate("40");
        req.setBudgetEstimate("5000");
        req.setPlannedOwnerId(TestFixtures.PM_USER);
        return wbsBo.create(req);
    }

    // ══════════════════════════════════════════════════
    //  创建
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("创建 WBS")
    class CreateTests {

        @Test
        @DisplayName("创建根节点 - 所有字段完整")
        void testCreateRoot_Success() {
            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setName("需求分析");
            req.setDescription("需求调研与分析");
            req.setElementType("TASK");
            req.setEffortEstimate("80");
            req.setBudgetEstimate("10000");
            req.setOwnerId(TestFixtures.PM_USER);
            req.setPlannedOwnerId(TestFixtures.PM_USER);
            req.setPriority("HIGH");
            req.setTechDifficulty("MEDIUM");
            req.setPlannedStartDate("2026-01-01");
            req.setPlannedEndDate("2026-01-31");
            req.setRemarks("备注");
            req.setSortOrder(1);

            WhPmWbsElement element = wbsBo.create(req);

            assertNotNull(element.getId());
            assertEquals("NOT_STARTED", element.getStatus());
            assertEquals("TASK", element.getElementType());
            assertEquals(1, element.getLevel());
            assertNotNull(element.getWbsCode());
        }

        @Test
        @DisplayName("创建子节点 - 层级和编码正确")
        void testCreateChild_Success() {
            WhPmWbsElement parent = createWbs("父节点");

            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setParentId(parent.getId());
            req.setName("子节点");
            req.setElementType("TASK");

            WhPmWbsElement child = wbsBo.create(req);

            assertNotNull(child.getId());
            assertEquals(parent.getLevel() + 1, child.getLevel());
            assertTrue(child.getWbsCode().startsWith(parent.getWbsCode()));
        }

        @Test
        @DisplayName("创建时自动生成版本 0.1")
        void testCreate_CreatesInitialVersion() {
            WhPmWbsElement element = createWbs("版本测试");

            List<WhPmWbsVersion> versions = wbsBo.getVersionHistory(element.getId());
            assertEquals(1, versions.size());
            assertEquals(0, BigDecimal.valueOf(0.1).compareTo(versions.get(0).getVersionNumber()));
        }

        @Test
        @DisplayName("创建时默认 elementType 为 TASK")
        void testCreate_DefaultElementType() {
            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setName("默认类型");

            WhPmWbsElement element = wbsBo.create(req);
            assertEquals("TASK", element.getElementType());
        }
    }

    // ══════════════════════════════════════════════════
    //  查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("查询 WBS")
    class QueryTests {

        @Test
        @DisplayName("getTreeByProjectId - 返回树形结构")
        void testGetTree_HappyPath() {
            WhPmWbsElement parent = createWbs("父节点");
            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setParentId(parent.getId());
            req.setName("子节点");
            req.setElementType("TASK");
            wbsBo.create(req);

            List<WhPmWbsElement> tree = wbsBo.getTreeByProjectId(projectId, null, null);

            assertEquals(1, tree.size());
            assertEquals("父节点", tree.get(0).getName());
            assertNotNull(tree.get(0).getChildren());
            assertEquals(1, tree.get(0).getChildren().size());
            assertEquals("子节点", tree.get(0).getChildren().get(0).getName());
        }

        @Test
        @DisplayName("getTreeByProjectId - 按状态过滤")
        void testGetTree_FilterByStatus() {
            createWbs("未开始节点");

            List<WhPmWbsElement> matched = wbsBo.getTreeByProjectId(projectId, "NOT_STARTED", null);
            assertFalse(matched.isEmpty());

            List<WhPmWbsElement> empty = wbsBo.getTreeByProjectId(projectId, "COMPLETED", null);
            assertTrue(empty.isEmpty());
        }

        @Test
        @DisplayName("getTreeByProjectId - 按关键词搜索")
        void testGetTree_FilterByKeyword() {
            createWbs("需求分析");
            createWbs("概要设计");

            List<WhPmWbsElement> result = wbsBo.getTreeByProjectId(projectId, null, "需求");
            assertFalse(result.isEmpty());
            assertTrue(result.stream().anyMatch(e -> e.getName().contains("需求")));
        }

        @Test
        @DisplayName("getDetail - 存在时返回详情")
        void testGetDetail_Success() {
            WhPmWbsElement element = createWbs("详情测试");

            WhPmWbsElement detail = wbsBo.getDetail(element.getId());
            assertNotNull(detail);
            assertEquals(element.getId(), detail.getId());
            assertEquals("详情测试", detail.getName());
        }

        @Test
        @DisplayName("getDetail - 不存在抛出 404")
        void testGetDetail_NotFound() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.getDetail("non-existent-id"));
            assertEquals(404, ex.getCode());
        }
    }

    // ══════════════════════════════════════════════════
    //  更新
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("更新 WBS")
    class UpdateTests {

        @Test
        @DisplayName("NOT_STARTED 状态更新 - 成功")
        void testUpdate_NotStarted_Success() {
            WhPmWbsElement element = createWbs("待更新");

            WbsUpdateRequest req = new WbsUpdateRequest();
            req.setName("已更新");
            req.setDescription("新描述");
            req.setEffortEstimate("60");

            wbsBo.update(element.getId(), req);

            WhPmWbsElement updated = wbsBo.getDetail(element.getId());
            assertEquals("已更新", updated.getName());
            assertEquals("新描述", updated.getDescription());
        }

        @Test
        @DisplayName("SUSPENDED 状态更新 - 成功")
        void testUpdate_Suspended_Success() {
            WhPmWbsElement element = createWbs("暂停更新");
            wbsBo.start(element.getId());
            wbsBo.suspend(element.getId());

            WbsUpdateRequest req = new WbsUpdateRequest();
            req.setName("暂停时更新");

            wbsBo.update(element.getId(), req);

            WhPmWbsElement updated = wbsBo.getDetail(element.getId());
            assertEquals("暂停时更新", updated.getName());
        }

        @Test
        @DisplayName("COMPLETED 状态更新 - 抛出异常")
        void testUpdate_Completed_Throws() {
            WhPmWbsElement element = createWbs("已完成");
            wbsBo.start(element.getId());
            wbsBo.test(element.getId());
            wbsBo.complete(element.getId());

            WbsUpdateRequest req = new WbsUpdateRequest();
            req.setName("不应能更新");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.update(element.getId(), req));
            assertTrue(ex.getMessage().contains("不可修改"));
        }

        @Test
        @DisplayName("更新时日期变化 - 更新成功不报错")
        void testUpdate_WithDates_DoesNotThrow() {
            WhPmWbsElement element = createWbs("日期更新");

            WbsUpdateRequest req = new WbsUpdateRequest();
            req.setName(element.getName());
            req.setDescription(element.getDescription());
            req.setElementType(element.getElementType());
            req.setEffortEstimate(element.getEffortEstimate());
            req.setBudgetEstimate(element.getBudgetEstimate());
            req.setPlannedStartDate("2026-02-01");
            req.setPlannedEndDate("2026-02-28");

            assertDoesNotThrow(() -> wbsBo.update(element.getId(), req));
        }
    }

    // ══════════════════════════════════════════════════
    //  状态流转
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("WBS 状态流转")
    class StatusTransitionTests {

        @Test
        @DisplayName("正向流转: NOT_STARTED → IN_DEVELOPMENT → TESTING → COMPLETED")
        void testFullForwardFlow() {
            WhPmWbsElement element = createWbs("正向流转");

            wbsBo.start(element.getId());
            WhPmWbsElement started = wbsBo.getDetail(element.getId());
            assertEquals("IN_DEVELOPMENT", started.getStatus());
            assertNotNull(started.getActualStartDate());

            wbsBo.test(element.getId());
            assertEquals("TESTING", wbsBo.getDetail(element.getId()).getStatus());

            wbsBo.complete(element.getId());
            WhPmWbsElement completed = wbsBo.getDetail(element.getId());
            assertEquals("COMPLETED", completed.getStatus());
            assertNotNull(completed.getActualEndDate());
        }

        @Test
        @DisplayName("暂停恢复: IN_DEVELOPMENT → SUSPENDED → IN_DEVELOPMENT")
        void testSuspendAndResume() {
            WhPmWbsElement element = createWbs("暂停恢复");
            wbsBo.start(element.getId());

            wbsBo.suspend(element.getId());
            assertEquals("SUSPENDED", wbsBo.getDetail(element.getId()).getStatus());

            wbsBo.resume(element.getId());
            assertEquals("IN_DEVELOPMENT", wbsBo.getDetail(element.getId()).getStatus());
        }

        @Test
        @DisplayName("取消: NOT_STARTED → CANCELLED")
        void testCancel_NotStarted() {
            WhPmWbsElement element = createWbs("取消");
            wbsBo.cancel(element.getId());
            assertEquals("CANCELLED", wbsBo.getDetail(element.getId()).getStatus());
        }

        @Test
        @DisplayName("取消: IN_DEVELOPMENT → CANCELLED")
        void testCancel_InDevelopment() {
            WhPmWbsElement element = createWbs("开发中取消");
            wbsBo.start(element.getId());
            wbsBo.cancel(element.getId());
            assertEquals("CANCELLED", wbsBo.getDetail(element.getId()).getStatus());
        }

        @Test
        @DisplayName("取消: SUSPENDED → CANCELLED")
        void testCancel_Suspended() {
            WhPmWbsElement element = createWbs("暂停取消");
            wbsBo.start(element.getId());
            wbsBo.suspend(element.getId());
            wbsBo.cancel(element.getId());
            assertEquals("CANCELLED", wbsBo.getDetail(element.getId()).getStatus());
        }

        @Test
        @DisplayName("重新打开: COMPLETED → NOT_STARTED, actualEndDate 清空")
        void testReopen_Completed() {
            WhPmWbsElement element = createWbs("重开");
            wbsBo.start(element.getId());
            wbsBo.test(element.getId());
            wbsBo.complete(element.getId());

            wbsBo.reopen(element.getId());
            WhPmWbsElement reopened = wbsBo.getDetail(element.getId());
            assertEquals("NOT_STARTED", reopened.getStatus());
            assertNull(reopened.getActualEndDate());
        }

        @Test
        @DisplayName("重新打开: CANCELLED → NOT_STARTED")
        void testReopen_Cancelled() {
            WhPmWbsElement element = createWbs("取消重开");
            wbsBo.cancel(element.getId());
            wbsBo.reopen(element.getId());
            assertEquals("NOT_STARTED", wbsBo.getDetail(element.getId()).getStatus());
        }
    }

    // ══════════════════════════════════════════════════
    //  状态流转 - 异常场景
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("状态流转异常")
    class StatusTransitionErrorTests {

        @Test
        @DisplayName("start - NOT_STARTED 以外状态抛出异常")
        void testStart_NonNotStarted_Throws() {
            WhPmWbsElement element = createWbs("start错误");
            wbsBo.start(element.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.start(element.getId()));
            assertTrue(ex.getMessage().contains("未开始"));
        }

        @Test
        @DisplayName("test - IN_DEVELOPMENT 以外状态抛出异常")
        void testTest_NonInDevelopment_Throws() {
            WhPmWbsElement element = createWbs("test错误");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.test(element.getId()));
            assertTrue(ex.getMessage().contains("开发中"));
        }

        @Test
        @DisplayName("complete - TESTING 以外状态抛出异常")
        void testComplete_NonTesting_Throws() {
            WhPmWbsElement element = createWbs("complete错误");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.complete(element.getId()));
            assertTrue(ex.getMessage().contains("提测"));
        }

        @Test
        @DisplayName("suspend - IN_DEVELOPMENT 以外状态抛出异常")
        void testSuspend_NonInDevelopment_Throws() {
            WhPmWbsElement element = createWbs("suspend错误");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.suspend(element.getId()));
            assertTrue(ex.getMessage().contains("开发中"));
        }

        @Test
        @DisplayName("resume - SUSPENDED 以外状态抛出异常")
        void testResume_NonSuspended_Throws() {
            WhPmWbsElement element = createWbs("resume错误");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.resume(element.getId()));
            assertTrue(ex.getMessage().contains("暂停"));
        }

        @Test
        @DisplayName("reopen - COMPLETED/CANCELLED 以外状态抛出异常")
        void testReopen_InvalidStatus_Throws() {
            WhPmWbsElement element = createWbs("reopen错误");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.reopen(element.getId()));
            assertTrue(ex.getMessage().contains("已完成") || ex.getMessage().contains("已取消"));
        }

        @Test
        @DisplayName("cancel - TESTING/COMPLETED 状态下抛出异常")
        void testCancel_InvalidStatus_Throws() {
            WhPmWbsElement element = createWbs("cancel错误");
            wbsBo.start(element.getId());
            wbsBo.test(element.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.cancel(element.getId()));
            assertTrue(ex.getMessage().contains("未开始") || ex.getMessage().contains("开发中")
                    || ex.getMessage().contains("暂停"));
        }
    }

    // ══════════════════════════════════════════════════
    //  删除
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("删除 WBS")
    class DeleteTests {

        @Test
        @DisplayName("NOT_STARTED 状态删除 - 成功")
        void testDelete_NotStarted_Success() {
            WhPmWbsElement element = createWbs("可删除");

            wbsBo.delete(element.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.getDetail(element.getId()));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("SUSPENDED 状态删除 - 成功")
        void testDelete_Suspended_Success() {
            WhPmWbsElement element = createWbs("暂停可删");
            wbsBo.start(element.getId());
            wbsBo.suspend(element.getId());

            wbsBo.delete(element.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.getDetail(element.getId()));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("有子节点时删除 - 抛出异常")
        void testDelete_HasChildren_Throws() {
            WhPmWbsElement parent = createWbs("父节点");
            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setParentId(parent.getId());
            req.setName("子节点");
            req.setElementType("TASK");
            wbsBo.create(req);

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.delete(parent.getId()));
            assertTrue(ex.getMessage().contains("子节点"));
        }

        @Test
        @DisplayName("IN_DEVELOPMENT 状态删除 - 抛出异常")
        void testDelete_InDevelopment_Throws() {
            WhPmWbsElement element = createWbs("开发中");
            wbsBo.start(element.getId());

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.delete(element.getId()));
            assertTrue(ex.getMessage().contains("未开始") || ex.getMessage().contains("暂停"));
        }
    }

    // ══════════════════════════════════════════════════
    //  版本历史
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("版本历史")
    class VersionHistoryTests {

        @Test
        @DisplayName("初始版本为 0.1")
        void testGetVersionHistory_Initial() {
            WhPmWbsElement element = createWbs("版本");

            List<WhPmWbsVersion> versions = wbsBo.getVersionHistory(element.getId());
            assertEquals(1, versions.size());
            assertEquals(0, BigDecimal.valueOf(0.1).compareTo(versions.get(0).getVersionNumber()));
        }

        @Test
        @DisplayName("不存在时抛出 404")
        void testGetVersionHistory_NotFound() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> wbsBo.getVersionHistory("non-existent-id"));
            assertEquals(404, ex.getCode());
        }
    }
}
