package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.R;
import com.wh.common.ServiceException;
import com.wh.bo.system.SysUserBo;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.service.SequenceService;
import com.wh.util.SecurityUtils;
import com.wh.vo.pm.CharterStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhPmCharterBo {

    private final WhPmCharterDao charterDao;
    private final SysUserBo sysUserBo;
    private final WhPmWbsElementDao wbsElementDao;
    private final SequenceService sequenceService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WhPmCharterBo(WhPmCharterDao charterDao, SysUserBo sysUserBo, SequenceService sequenceService,
                          RuntimeService runtimeService, TaskService taskService, WhPmWbsElementDao wbsElementDao) {
        this.charterDao = charterDao;
        this.sysUserBo = sysUserBo;
        this.wbsElementDao = wbsElementDao;
        this.sequenceService = sequenceService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
    }

    public IPage<WhPmCharter> pageList(int pageNum, int pageSize, String status, String pmId, String keyword, String progress) {
        Page<WhPmCharter> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WhPmCharter> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmCharter::getDelFlag, "0");
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WhPmCharter::getStatus, status);
        }
        if (pmId != null && !pmId.isEmpty()) {
            wrapper.eq(WhPmCharter::getPmId, pmId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            // 必须用 and(...) 嵌套，否则 OR 会绕过 del_flag 等前置条件
            wrapper.and(w -> w.like(WhPmCharter::getProjectName, keyword)
                    .or()
                    .like(WhPmCharter::getCharterCode, keyword));
        }
        if (progress != null && !progress.isEmpty()) {
            List<String> progressList = java.util.Arrays.asList(progress.split(","));
            wrapper.in(WhPmCharter::getProgress, progressList);
        }
        wrapper.orderByDesc(WhPmCharter::getCreateDate);
        IPage<WhPmCharter> result = charterDao.selectPage(page, wrapper);
        fillUserNames(result.getRecords());
        fillWbsStats(result.getRecords());
        return result;
    }

    private void fillUserNames(List<WhPmCharter> charters) {
        Set<String> userIds = new HashSet<>();
        for (WhPmCharter c : charters) {
            if (c.getSponsorId() != null) userIds.add(c.getSponsorId());
            if (c.getPmId() != null) userIds.add(c.getPmId());
        }
        Map<String, String> userNameMap = sysUserBo.getRealNameMap(userIds);
        for (WhPmCharter c : charters) {
            c.setSponsorName(userNameMap.get(c.getSponsorId()));
            c.setPmName(userNameMap.get(c.getPmId()));
        }
    }

    private void fillWbsStats(List<WhPmCharter> charters) {
        Set<String> projectIds = charters.stream().map(WhPmCharter::getId).collect(Collectors.toSet());
        if (projectIds.isEmpty()) {
            return;
        }
        // 一次性批量加载所有项目的 level=1 WBS 元素，避免逐项目查询
        LambdaQueryWrapper<WhPmWbsElement> wrapper = new LambdaQueryWrapper<>();
        wrapper.in(WhPmWbsElement::getProjectId, projectIds)
               .eq(WhPmWbsElement::getDelFlag, "0")
               .eq(WhPmWbsElement::getLevel, 1);
        Map<String, List<WhPmWbsElement>> elementsByProject = wbsElementDao.selectList(wrapper).stream()
                .collect(Collectors.groupingBy(WhPmWbsElement::getProjectId));

        for (WhPmCharter c : charters) {
            List<WhPmWbsElement> elements = elementsByProject.getOrDefault(c.getId(), List.of());

            double totalEffort = 0;
            String latestDate = null;
            for (WhPmWbsElement e : elements) {
                if (e.getEffortEstimate() != null) {
                    try {
                        totalEffort += Double.parseDouble(e.getEffortEstimate());
                    } catch (NumberFormatException ignored) {
                    }
                }
                String endDate = e.getLatestPlannedEndDate() != null ? e.getLatestPlannedEndDate() : e.getActualEndDate();
                if (endDate != null && (latestDate == null || endDate.compareTo(latestDate) > 0)) {
                    latestDate = endDate;
                }
            }
            c.setWbsTotalEffort(totalEffort > 0 ? totalEffort : null);
            c.setWbsLatestEndDate(latestDate);
        }
    }

    public CharterStatsVO getStatsByPmId(String pmId) {
        CharterStatsVO stats = new CharterStatsVO();
        List<Map<String, Object>> results = charterDao.selectStatsByPmId(pmId);
        for (Map<String, Object> row : results) {
            String status = (String) row.get("status");
            Number count = (Number) row.get("cnt");
            int c = count != null ? count.intValue() : 0;
            if ("DRAFT".equals(status)) stats.setDraft(c);
            else if ("PENDING_APPROVAL".equals(status)) stats.setPending(c);
            else if ("APPROVED".equals(status)) stats.setApproved(c);
            else if ("REJECTED".equals(status)) stats.setRejected(c);
        }
        stats.setTotal(stats.getDraft() + stats.getPending() + stats.getApproved() + stats.getRejected());
        return stats;
    }

    public WhPmCharter getById(String id) {
        WhPmCharter charter = charterDao.selectById(id);
        if (charter == null || "1".equals(charter.getDelFlag())) {
            throw new ServiceException(404, "章程不存在");
        }
        fillUserNames(List.of(charter));
        return charter;
    }

    /**
     * Get charter by ID without throwing exception (for internal use)
     */
    public WhPmCharter getByIdSilent(String id) {
        if (id == null || id.isEmpty()) return null;
        WhPmCharter charter = charterDao.selectById(id);
        if (charter == null || "1".equals(charter.getDelFlag())) {
            return null;
        }
        return charter;
    }

    /**
     * 批量获取未删除的章程，返回 ID → 章程映射（供其他 BO 避免 N+1 查询）。
     */
    public Map<String, WhPmCharter> getByIds(Collection<String> ids) {
        Set<String> filtered = ids == null ? Set.of() : ids.stream()
                .filter(Objects::nonNull)
                .filter(id -> !id.isEmpty())
                .collect(Collectors.toSet());
        if (filtered.isEmpty()) {
            return new HashMap<>();
        }
        return charterDao.selectBatchIds(filtered).stream()
                .filter(c -> !"1".equals(c.getDelFlag()))
                .collect(Collectors.toMap(WhPmCharter::getId, c -> c, (a, b) -> a, HashMap::new));
    }

    @Transactional
    public WhPmCharter create(CharterCreateRequest req) {
        WhPmCharter charter = new WhPmCharter();
        charter.setCharterCode(sequenceService.generateCode("PM_CHARTER", "CHARTER-"));
        charter.setProjectName(req.getProjectName());
        charter.setProjectCode(req.getProjectCode());
        charter.setProjectShortName(req.getProjectShortName());
        charter.setDescription(req.getDescription());
        charter.setObjectives(req.getObjectives());
        charter.setScopeSummary(req.getScopeSummary());
        charter.setSponsorId(req.getSponsorId());
        charter.setPmId(req.getPmId());
        charter.setBudgetCap(req.getBudgetCap());
        charter.setStartDate(req.getStartDate());
        charter.setEndDate(req.getEndDate());
        charter.setKeyStakeholders(req.getKeyStakeholders());
        charter.setRemarks(req.getRemarks());
        charter.setContractNo(req.getContractNo());
        charter.setProjectCategory(req.getProjectCategory());
        charter.setOutputValueTaxable(req.getOutputValueTaxable());
        charter.setOutputValueExcludingTax(req.getOutputValueExcludingTax());
        charter.setTaxRate(req.getTaxRate());
        charter.setTaxAmount(req.getTaxAmount());
        charter.setProgress(req.getProgress() != null ? req.getProgress() : "IN_PROGRESS");
        charter.setStatus("DRAFT");
        charter.setDelFlag("0");
        charter.setVerNo(0);
        charterDao.insert(charter);
        return charter;
    }

    @Transactional
    public void update(String id, CharterUpdateRequest req) {
        WhPmCharter charter = getById(id);
        if (!"DRAFT".equals(charter.getStatus()) && !"REJECTED".equals(charter.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的章程可以修改");
        }
        charter.setProjectName(req.getProjectName());
        charter.setProjectCode(req.getProjectCode());
        charter.setProjectShortName(req.getProjectShortName());
        charter.setDescription(req.getDescription());
        charter.setObjectives(req.getObjectives());
        charter.setScopeSummary(req.getScopeSummary());
        charter.setSponsorId(req.getSponsorId());
        charter.setPmId(req.getPmId());
        charter.setBudgetCap(req.getBudgetCap());
        charter.setStartDate(req.getStartDate());
        charter.setEndDate(req.getEndDate());
        charter.setKeyStakeholders(req.getKeyStakeholders());
        charter.setRemarks(req.getRemarks());
        charter.setContractNo(req.getContractNo());
        charter.setProjectCategory(req.getProjectCategory());
        charter.setOutputValueTaxable(req.getOutputValueTaxable());
        charter.setOutputValueExcludingTax(req.getOutputValueExcludingTax());
        charter.setTaxRate(req.getTaxRate());
        charter.setTaxAmount(req.getTaxAmount());
        charter.setProgress(req.getProgress());
        charterDao.updateById(charter);
    }

    @Transactional
    public void delete(String id) {
        WhPmCharter charter = getById(id);
        if (!"DRAFT".equals(charter.getStatus())) {
            throw new ServiceException("只有草稿状态的章程可以删除");
        }
        charterDao.physicalDeleteById(id);
    }

    public void submit(String id) {
        WhPmCharter charter = getById(id);
        if (!"DRAFT".equals(charter.getStatus()) && !"REJECTED".equals(charter.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的章程可以提交审批");
        }

        String currentUserId = SecurityUtils.getCurrentUserId();

        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("flowCode", "PM_CHARTER_APPROVAL");
        variables.put("bizId", id);
        variables.put("submitter", currentUserId);
        variables.put("assignee", charter.getSponsorId());

        org.flowable.engine.runtime.ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "PM_CHARTER_APPROVAL", variables);

        charter.setStatus("PENDING_APPROVAL");
        charter.setProcessInstanceId(instance.getId());
        charterDao.updateById(charter);

        // Auto-complete submitConfirm task to forward to sponsor approval
        Task task = taskService.createTaskQuery()
                .processInstanceId(instance.getId())
                .taskDefinitionKey("submitConfirm")
                .singleResult();
        if (task != null) {
            taskService.complete(task.getId());
            log.info("Charter {} submitted, submitConfirm completed, forwarded to sponsor approval", id);
        }
    }

    public void approve(String id, String comment) {
        WhPmCharter charter = getById(id);
        if (!"PENDING_APPROVAL".equals(charter.getStatus())) {
            throw new ServiceException("章程不在审批中");
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(charter.getProcessInstanceId())
                .singleResult();
        if (task == null) {
            throw new ServiceException("未找到审批任务");
        }

        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("approvalResult", "APPROVED");
        variables.put("comment", comment);
        taskService.complete(task.getId(), variables);

        charter.setStatus("APPROVED");
        charter.setApprovalComment(comment);
        charterDao.updateById(charter);
    }

    public void reject(String id, String comment) {
        WhPmCharter charter = getById(id);
        if (!"PENDING_APPROVAL".equals(charter.getStatus())) {
            throw new ServiceException("章程不在审批中");
        }

        Task task = taskService.createTaskQuery()
                .processInstanceId(charter.getProcessInstanceId())
                .singleResult();
        if (task == null) {
            throw new ServiceException("未找到审批任务");
        }

        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("approvalResult", "REJECTED");
        variables.put("rejectReason", comment);
        taskService.complete(task.getId(), variables);

        charter.setStatus("REJECTED");
        charter.setApprovalComment(comment);
        charterDao.updateById(charter);
        charterDao.update(null, new com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper<com.wh.entity.pm.WhPmCharter>()
                .eq(com.wh.entity.pm.WhPmCharter::getId, charter.getId())
                .set(com.wh.entity.pm.WhPmCharter::getProcessInstanceId, null));
    }
}
