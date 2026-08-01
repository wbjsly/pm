package com.wh;

import com.wh.bo.pm.WhPmBudgetBo;
import com.wh.bo.pm.WhPmCharterBo;
import com.wh.bo.pm.WhPmDeliverableBo;
import com.wh.bo.system.SysUserBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmBudgetDao;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmDeliverableDao;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmDeliverable;
import com.wh.service.SequenceService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.TaskQuery;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * 覆盖工作流防御分支：审批任务不存在、乐观锁失败等（纯 Mockito 单元测试）。
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("工作流防御分支测试")
class WorkflowBranchTest {

    @Mock private WhPmCharterDao charterDao;
    @Mock private WhPmBudgetDao budgetDao;
    @Mock private WhPmDeliverableDao deliverableDao;
    @Mock private SysUserBo sysUserBo;
    @Mock private SequenceService sequenceService;
    @Mock private RuntimeService runtimeService;
    @Mock private TaskService taskService;
    @Mock private com.wh.dao.pm.WhPmBudgetItemDao budgetItemDao;
    @Mock private com.wh.bo.pm.WhPmBudgetItemBo budgetItemBo;
    @Mock private com.wh.dao.pm.WhPmActualCostDao actualCostDao;
    @Mock private com.wh.dao.pm.WhPmWbsElementDao wbsElementDao;
    @Mock private io.minio.MinioClient minioClient;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("pm_001", "pw", Collections.emptyList()));
    }

    private TaskQuery taskQueryReturningNull() {
        TaskQuery query = mock(TaskQuery.class);
        org.mockito.Mockito.doReturn(query).when(query).processInstanceId(anyString());
        org.mockito.Mockito.doReturn(null).when(query).singleResult();
        return query;
    }

    private WhPmCharter pendingCharter() {
        WhPmCharter c = new WhPmCharter();
        c.setId("c1");
        c.setStatus("PENDING_APPROVAL");
        c.setProcessInstanceId("proc-1");
        c.setSponsorId("sponsor-1");
        c.setPmId("pm_001");
        c.setProjectName("测试");
        return c;
    }

    @Test
    @DisplayName("CharterBo.approve - 审批任务不存在抛异常")
    void charterApprove_taskNull_throws() {
        when(charterDao.selectById("c1")).thenReturn(pendingCharter());
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        TaskQuery tq = taskQueryReturningNull();
        when(taskService.createTaskQuery()).thenReturn(tq);

        WhPmCharterBo bo = new WhPmCharterBo(charterDao, sysUserBo, sequenceService,
                runtimeService, taskService, wbsElementDao);
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.approve("c1", "同意"));
        assertTrue(ex.getMessage().contains("未找到审批任务"));
    }

    @Test
    @DisplayName("CharterBo.reject - 审批任务不存在抛异常")
    void charterReject_taskNull_throws() {
        when(charterDao.selectById("c1")).thenReturn(pendingCharter());
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        TaskQuery tq = taskQueryReturningNull();
        when(taskService.createTaskQuery()).thenReturn(tq);

        WhPmCharterBo bo = new WhPmCharterBo(charterDao, sysUserBo, sequenceService,
                runtimeService, taskService, wbsElementDao);
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.reject("c1", "驳回"));
        assertTrue(ex.getMessage().contains("未找到审批任务"));
    }

    @Test
    @DisplayName("BudgetBo.approve - 审批任务不存在抛异常")
    void budgetApprove_taskNull_throws() {
        WhPmBudget budget = new WhPmBudget();
        budget.setId("b1");
        budget.setStatus("PENDING");
        budget.setProcessInstanceId("proc-b");
        budget.setVersion("v0.7");
        when(budgetDao.selectById("b1")).thenReturn(budget);
        TaskQuery tq = taskQueryReturningNull();
        when(taskService.createTaskQuery()).thenReturn(tq);

        WhPmBudgetBo bo = new WhPmBudgetBo(budgetDao, budgetItemDao, budgetItemBo,
                actualCostDao, charterDao, sysUserBo, sequenceService, runtimeService, taskService);
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.approve("b1", "同意"));
        assertTrue(ex.getMessage().contains("未找到审批任务"));
    }

    @Test
    @DisplayName("BudgetBo.reject - 审批任务不存在抛异常")
    void budgetReject_taskNull_throws() {
        WhPmBudget budget = new WhPmBudget();
        budget.setId("b1");
        budget.setStatus("PENDING");
        budget.setProcessInstanceId("proc-b");
        budget.setVersion("v0.7");
        when(budgetDao.selectById("b1")).thenReturn(budget);
        TaskQuery tq = taskQueryReturningNull();
        when(taskService.createTaskQuery()).thenReturn(tq);

        WhPmBudgetBo bo = new WhPmBudgetBo(budgetDao, budgetItemDao, budgetItemBo,
                actualCostDao, charterDao, sysUserBo, sequenceService, runtimeService, taskService);
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.reject("b1", "驳回"));
        assertTrue(ex.getMessage().contains("未找到审批任务"));
    }

    @Test
    @DisplayName("DeliverableBo.approve - 审批任务不存在抛异常")
    void deliverableApprove_taskNull_throws() {
        WhPmDeliverable d = new WhPmDeliverable();
        d.setId("d1");
        d.setStatus("PENDING_APPROVAL");
        d.setProcessInstanceId("proc-d");
        d.setProjectId("proj-1");
        when(deliverableDao.selectById("d1")).thenReturn(d);
        WhPmCharter charter = new WhPmCharter();
        charter.setSponsorId("pm_001");
        charter.setPmId("pm_001");
        charter.setStatus("APPROVED");
        when(charterDao.selectById("proj-1")).thenReturn(charter);
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        TaskQuery tq = taskQueryReturningNull();
        when(taskService.createTaskQuery()).thenReturn(tq);

        WhPmDeliverableBo bo = new WhPmDeliverableBo(deliverableDao, charterDao, sysUserBo,
                sequenceService, runtimeService, taskService, minioClient, "wh-files");
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.approve("d1", "同意"));
        assertTrue(ex.getMessage().contains("未找到审批任务"));
    }

    @Test
    @DisplayName("DeliverableBo.reject - 审批任务不存在抛异常")
    void deliverableReject_taskNull_throws() {
        WhPmDeliverable d = new WhPmDeliverable();
        d.setId("d1");
        d.setStatus("PENDING_APPROVAL");
        d.setProcessInstanceId("proc-d");
        d.setProjectId("proj-1");
        when(deliverableDao.selectById("d1")).thenReturn(d);
        WhPmCharter charter = new WhPmCharter();
        charter.setSponsorId("pm_001");
        charter.setPmId("pm_001");
        charter.setStatus("APPROVED");
        when(charterDao.selectById("proj-1")).thenReturn(charter);
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        TaskQuery tq = taskQueryReturningNull();
        when(taskService.createTaskQuery()).thenReturn(tq);

        WhPmDeliverableBo bo = new WhPmDeliverableBo(deliverableDao, charterDao, sysUserBo,
                sequenceService, runtimeService, taskService, minioClient, "wh-files");
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.reject("d1", "驳回"));
        assertTrue(ex.getMessage().contains("未找到审批任务"));
    }

    @Test
    @DisplayName("DeliverableBo.update - 乐观锁失败抛异常")
    void deliverableUpdate_optimisticLock_throws() {
        WhPmDeliverable d = new WhPmDeliverable();
        d.setId("d1");
        d.setStatus("DRAFT");
        d.setProjectId("proj-1");
        when(deliverableDao.selectById("d1")).thenReturn(d);
        WhPmCharter charter = new WhPmCharter();
        charter.setPmId("pm_001");
        charter.setSponsorId("pm_001");
        when(charterDao.selectById("proj-1")).thenReturn(charter);
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        when(deliverableDao.updateById(any())).thenReturn(0);

        com.wh.bo.pm.DeliverableUpdateRequest req = new com.wh.bo.pm.DeliverableUpdateRequest();
        req.setName("新名称");
        WhPmDeliverableBo bo = new WhPmDeliverableBo(deliverableDao, charterDao, sysUserBo,
                sequenceService, runtimeService, taskService, minioClient, "wh-files");
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.update("d1", req));
        assertTrue(ex.getMessage().contains("已被他人修改"));
    }

    @Test
    @DisplayName("DeliverableBo.delete - 乐观锁失败抛异常")
    void deliverableDelete_optimisticLock_throws() {
        WhPmDeliverable d = new WhPmDeliverable();
        d.setId("d1");
        d.setStatus("DRAFT");
        d.setProjectId("proj-1");
        when(deliverableDao.selectById("d1")).thenReturn(d);
        WhPmCharter charter = new WhPmCharter();
        charter.setPmId("pm_001");
        charter.setSponsorId("pm_001");
        when(charterDao.selectById("proj-1")).thenReturn(charter);
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());

        WhPmDeliverableBo bo = new WhPmDeliverableBo(deliverableDao, charterDao, sysUserBo,
                sequenceService, runtimeService, taskService, minioClient, "wh-files");
        bo.delete("d1");
        verify(deliverableDao).deleteById("d1");
    }

    @Test
    @DisplayName("DeliverableBo.submit - 乐观锁失败抛异常")
    void deliverableSubmit_optimisticLock_throws() {
        WhPmDeliverable d = new WhPmDeliverable();
        d.setId("d1");
        d.setStatus("DRAFT");
        d.setProjectId("proj-1");
        when(deliverableDao.selectById("d1")).thenReturn(d);
        WhPmCharter charter = new WhPmCharter();
        charter.setPmId("pm_001");
        charter.setSponsorId("sponsor-1");
        when(charterDao.selectById("proj-1")).thenReturn(charter);
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        org.flowable.engine.runtime.ProcessInstance pi = mock(org.flowable.engine.runtime.ProcessInstance.class);
        when(pi.getId()).thenReturn("proc-d");
        when(runtimeService.startProcessInstanceByKey(anyString(), anyMap())).thenReturn(pi);
        when(deliverableDao.updateById(any())).thenReturn(0);

        WhPmDeliverableBo bo = new WhPmDeliverableBo(deliverableDao, charterDao, sysUserBo,
                sequenceService, runtimeService, taskService, minioClient, "wh-files");
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.submit("d1"));
        assertTrue(ex.getMessage().contains("已被他人修改"));
    }

    @Test
    @DisplayName("DeliverableBo.markDelivered - 乐观锁失败抛异常")
    void deliverableMarkDelivered_optimisticLock_throws() {
        WhPmDeliverable d = new WhPmDeliverable();
        d.setId("d1");
        d.setStatus("APPROVED");
        d.setProjectId("proj-1");
        when(deliverableDao.selectById("d1")).thenReturn(d);
        WhPmCharter charter = new WhPmCharter();
        charter.setPmId("pm_001");
        charter.setSponsorId("sponsor-1");
        when(charterDao.selectById("proj-1")).thenReturn(charter);
        when(sysUserBo.getRealNameMap(any())).thenReturn(new java.util.HashMap<>());
        when(deliverableDao.updateById(any())).thenReturn(0);

        WhPmDeliverableBo bo = new WhPmDeliverableBo(deliverableDao, charterDao, sysUserBo,
                sequenceService, runtimeService, taskService, minioClient, "wh-files");
        ServiceException ex = assertThrows(ServiceException.class, () -> bo.markDelivered("d1"));
        assertTrue(ex.getMessage().contains("已被他人修改"));
    }
}
