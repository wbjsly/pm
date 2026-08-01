package com.wh.approval.pm;

import com.wh.bo.pm.WbsApprovalCallback;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.dao.pm.WhPmWbsVersionDao;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.entity.pm.WhPmWbsVersion;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.engine.runtime.ProcessInstanceQuery;
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
@DisplayName("WbsApprovalCallback 测试")
class WbsApprovalCallbackTest {

    @Autowired
    private WbsApprovalCallback callback;

    @Autowired
    private WhPmWbsElementDao wbsDao;

    @Autowired
    private WhPmWbsVersionDao versionDao;

    @Autowired
    private RuntimeService runtimeService;

    private WhPmWbsElement createElement(String name) {
        WhPmWbsElement e = new WhPmWbsElement();
        e.setProjectId("wbs-callback-test-project");
        e.setWbsCode("WBS-T-" + System.nanoTime());
        e.setLevel(1);
        e.setName(name);
        e.setElementType("TASK");
        e.setStatus("PENDING");
        e.setDelFlag("0");
        e.setVerNo(0);
        e.setLatestPlannedEndDate("2026-12-31");
        wbsDao.insert(e);
        return e;
    }

    @Nested
    @DisplayName("applyChanges 应用变更")
    class ApplyChanges {

        @Test
        @DisplayName("元素存在且无版本时创建 0.2 版本")
        void elementWithoutVersion_createsFirstVersion() {
            WhPmWbsElement e = createElement("无版本节点");
            callback.applyChanges(e.getId());

            List<WhPmWbsVersion> versions = versionDao.selectByWbsId(e.getId());
            assertEquals(1, versions.size());
            assertEquals(0, new BigDecimal("0.2").compareTo(versions.get(0).getVersionNumber()));
            assertEquals("2026-12-31", versions.get(0).getPlannedStartDate());
            assertEquals("2026-12-31", versions.get(0).getPlannedEndDate());
        }

        @Test
        @DisplayName("已有版本时版本号递增 0.1")
        void elementWithVersion_incrementsVersion() {
            WhPmWbsElement e = createElement("已有版本节点");
            callback.applyChanges(e.getId());
            callback.applyChanges(e.getId());
            callback.applyChanges(e.getId());

            List<WhPmWbsVersion> versions = versionDao.selectByWbsId(e.getId());
            assertEquals(3, versions.size());
            assertEquals(0, new BigDecimal("0.4").compareTo(
                    versions.stream().map(WhPmWbsVersion::getVersionNumber)
                            .max(BigDecimal::compareTo).orElse(BigDecimal.ZERO)));
        }

        @Test
        @DisplayName("元素不存在时不抛异常")
        void elementNotFound_doesNotThrow() {
            assertDoesNotThrow(() -> callback.applyChanges("nonexistent-wbs-id"));
        }
    }

    @Nested
    @DisplayName("discardChanges 丢弃变更")
    class DiscardChanges {

        @Test
        @DisplayName("元素存在时静默处理")
        void elementExists_doesNotThrow() {
            WhPmWbsElement e = createElement("丢弃节点");
            assertDoesNotThrow(() -> callback.discardChanges(e.getId(), "驳回原因"));
            // 不应创建任何版本
            assertEquals(0, versionDao.selectByWbsId(e.getId()).size());
        }

        @Test
        @DisplayName("元素不存在时不抛异常")
        void elementNotFound_doesNotThrow() {
            assertDoesNotThrow(() -> callback.discardChanges("nonexistent-wbs-id", "驳回"));
        }
    }

    @Nested
    @DisplayName("startApproval 启动审批")
    class StartApproval {

        @Test
        @DisplayName("元素存在时启动 PM_WBS_MODIFY_APPROVAL 流程")
        void elementExists_startsProcess() {
            WhPmWbsElement e = createElement("审批启动节点");
            assertDoesNotThrow(() -> callback.startApproval(e.getId(), "MODIFY", "变更内容"));

            ProcessInstanceQuery query = runtimeService.createProcessInstanceQuery();
            long count = 0;
            for (ProcessInstance pi : query.list()) {
                Object bizId = runtimeService.getVariable(pi.getId(), "bizId");
                if (e.getId().equals(bizId)) {
                    count++;
                }
            }
            assertEquals(1, count, "应启动一个包含 bizId 变量的流程实例");
        }

        @Test
        @DisplayName("元素不存在时抛出 404")
        void elementNotFound_throws404() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> callback.startApproval("nonexistent-wbs-id", "MODIFY", "内容"));
            assertEquals(404, ex.getCode());
            assertTrue(ex.getMessage().contains("不存在"));
        }
    }
}
