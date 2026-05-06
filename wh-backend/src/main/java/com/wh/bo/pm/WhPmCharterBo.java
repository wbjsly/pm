package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.R;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.entity.system.SysUser;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class WhPmCharterBo {

    private final WhPmCharterDao charterDao;
    private final SysUserDao sysUserDao;
    private final WhPmWbsElementDao wbsElementDao;
    private final SequenceService sequenceService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;

    public WhPmCharterBo(WhPmCharterDao charterDao, SysUserDao sysUserDao, SequenceService sequenceService,
                         RuntimeService runtimeService, TaskService taskService, WhPmWbsElementDao wbsElementDao) {
        this.charterDao = charterDao;
        this.sysUserDao = sysUserDao;
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
            wrapper.like(WhPmCharter::getProjectName, keyword)
                   .or()
                   .like(WhPmCharter::getCharterCode, keyword);
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
        Map<String, String> userNameMap = new HashMap<>();
        for (WhPmCharter c : charters) {
            collectUserId(c.getSponsorId(), userNameMap);
            collectUserId(c.getPmId(), userNameMap);
        }
        for (WhPmCharter c : charters) {
            c.setSponsorName(userNameMap.get(c.getSponsorId()));
            c.setPmName(userNameMap.get(c.getPmId()));
        }
    }

    private void collectUserId(String userId, Map<String, String> map) {
        if (userId != null && !userId.isEmpty() && !map.containsKey(userId)) {
            SysUser user = sysUserDao.selectById(userId);
            if (user != null) {
                map.put(userId, user.getRealName());
            }
        }
    }

    private void fillWbsStats(List<WhPmCharter> charters) {
        for (WhPmCharter c : charters) {
            LambdaQueryWrapper<WhPmWbsElement> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(WhPmWbsElement::getProjectId, c.getId())
                   .eq(WhPmWbsElement::getDelFlag, "0")
                   .eq(WhPmWbsElement::getLevel, 1);
            List<WhPmWbsElement> elements = wbsElementDao.selectList(wrapper);

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

    @Transactional
    public void submit(String id) {
        WhPmCharter charter = getById(id);
        if (!"DRAFT".equals(charter.getStatus()) && !"REJECTED".equals(charter.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的章程可以提交审批");
        }

        // Start Flowable process
        java.util.Map<String, Object> variables = new java.util.HashMap<>();
        variables.put("flowCode", "PM_CHARTER_APPROVAL");
        variables.put("bizId", id);
        variables.put("assignee", charter.getSponsorId());

        org.flowable.engine.runtime.ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "PM_CHARTER_APPROVAL", variables);

        charter.setStatus("PENDING_APPROVAL");
        charter.setProcessInstanceId(instance.getId());
        charterDao.updateById(charter);

        // Complete the user task as submit
        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        if (task != null) {
            // Task assigned to SPONSOR, just log it
            log.info("Charter {} submitted for approval, task: {}", id, task.getId());
        }
    }

    @Transactional
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
    }

    @Transactional
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
    }
}
