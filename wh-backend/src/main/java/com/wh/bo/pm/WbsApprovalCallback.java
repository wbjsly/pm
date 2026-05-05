package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.dao.pm.WhPmWbsVersionDao;
import com.wh.entity.pm.WhPmWbsVersion;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.common.ServiceException;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Handles WBS approval callback logic: apply or discard pending changes.
 */
@Slf4j
@Service
public class WbsApprovalCallback {

    private final WhPmWbsElementDao wbsDao;
    private final WhPmWbsVersionDao versionDao;
    private final RuntimeService runtimeService;

    public WbsApprovalCallback(WhPmWbsElementDao wbsDao, WhPmWbsVersionDao versionDao,
                               RuntimeService runtimeService) {
        this.wbsDao = wbsDao;
        this.versionDao = versionDao;
        this.runtimeService = runtimeService;
    }

    @Transactional
    public void applyChanges(String wbsId) {
        WhPmWbsElement element = wbsDao.selectById(wbsId);
        if (element == null) {
            log.error("WBS node not found: {}", wbsId);
            return;
        }

        // Create new version record (version_number + 0.1)
        BigDecimal maxVer = versionDao.selectMaxVersionNumber(wbsId);
        BigDecimal newVer = (maxVer != null ? maxVer : new BigDecimal("0.1")).add(new BigDecimal("0.1"));

        WhPmWbsVersion version = new WhPmWbsVersion();
        version.setWbsId(wbsId);
        version.setVersionNumber(newVer);
        version.setPlannedStartDate(element.getLatestPlannedEndDate());
        version.setPlannedEndDate(element.getLatestPlannedEndDate());
        version.setDelFlag("0");
        version.setVerNo(0);
        versionDao.insert(version);

        // Sync latestPlannedEndDate from new version
        element.setLatestPlannedEndDate(element.getLatestPlannedEndDate());
        wbsDao.updateById(element);

        log.info("WBS {} changes applied, new version: {}", wbsId, newVer);
    }

    @Transactional
    public void discardChanges(String wbsId, String rejectReason) {
        WhPmWbsElement element = wbsDao.selectById(wbsId);
        if (element == null) {
            log.error("WBS node not found: {}", wbsId);
            return;
        }
        log.info("WBS {} changes discarded, reason: {}", wbsId, rejectReason);
    }

    /**
     * Start the approval workflow for a WBS modification.
     */
    @Transactional
    public void startApproval(String wbsId, String approvalType, String content) {
        WhPmWbsElement element = wbsDao.selectById(wbsId);
        if (element == null) {
            throw new ServiceException(404, "WBS节点不存在");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("flowCode", "PM_WBS_MODIFY_APPROVAL");
        variables.put("bizId", wbsId);
        variables.put("approvalType", approvalType);

        // Auto-skip BA if submitter is BA
        boolean isBa = false; // TODO: check user role
        variables.put("skipBaApproval", isBa);

        org.flowable.engine.runtime.ProcessInstance instance = runtimeService.startProcessInstanceByKey(
                "PM_WBS_MODIFY_APPROVAL", variables);

        log.info("WBS modification approval started for {}, processId: {}", wbsId, instance.getId());
    }
}
