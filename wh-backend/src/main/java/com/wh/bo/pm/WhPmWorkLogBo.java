package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.ServiceException;
import com.wh.bo.system.SysUserBo;
import com.wh.dao.pm.WhPmWorkLogDao;
import com.wh.dao.pm.WhPmWbsElementDao;
import com.wh.dao.pm.WhSysWorkCalendarDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWorkLog;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.entity.pm.WhSysWorkCalendar;
import com.wh.vo.pm.WorkHoursStatsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class WhPmWorkLogBo {

    private final WhPmWorkLogDao workLogDao;
    private final WhPmCharterBo charterBo;
    private final WhPmWbsElementDao wbsElementDao;
    private final WhSysWorkCalendarDao calendarDao;
    private final WhSysWorkCalendarBo calendarBo;
    private final SysUserBo sysUserBo;

    public WhPmWorkLogBo(WhPmWorkLogDao workLogDao, WhPmCharterBo charterBo,
                           WhPmWbsElementDao wbsElementDao, WhSysWorkCalendarDao calendarDao,
                           WhSysWorkCalendarBo calendarBo, SysUserBo sysUserBo) {
        this.workLogDao = workLogDao;
        this.charterBo = charterBo;
        this.wbsElementDao = wbsElementDao;
        this.calendarDao = calendarDao;
        this.calendarBo = calendarBo;
        this.sysUserBo = sysUserBo;
    }

    /**
     * Get work logs by user and month
     */
    public List<WhPmWorkLog> getByUserAndMonth(String userId, String year, int month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), month);
        String startDate = ym.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = ym.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<WhPmWorkLog> logs = workLogDao.selectByUserAndMonth(userId, startDate, endDate);
        enrichLogs(logs);
        return logs;
    }

    /**
     * Get work logs by project and month (for project detail page)
     */
    public List<WhPmWorkLog> getByProjectAndMonth(String projectId, String year, int month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), month);
        String startDate = ym.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = ym.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        LambdaQueryWrapper<WhPmWorkLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmWorkLog::getProjectId, projectId)
               .ge(WhPmWorkLog::getLogDate, startDate)
               .le(WhPmWorkLog::getLogDate, endDate)
               .eq(WhPmWorkLog::getDelFlag, "0");
        List<WhPmWorkLog> logs = workLogDao.selectList(wrapper);
        enrichLogs(logs);
        return logs;
    }

    /**
     * Get pending work logs for a PM
     */
    public List<WhPmWorkLog> getPendingByPm(String pmId, String year, int month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), month);
        String startDate = ym.atDay(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        String endDate = ym.atEndOfMonth().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));

        List<WhPmWorkLog> logs = workLogDao.selectPendingByPm(pmId, startDate, endDate);
        enrichLogs(logs);
        return logs;
    }

    private void enrichLogs(List<WhPmWorkLog> logs) {
        // 批量加载项目与用户信息，避免逐条 N+1 查询
        Set<String> projectIds = new HashSet<>();
        Set<String> userIds = new HashSet<>();
        for (WhPmWorkLog log : logs) {
            projectIds.add(log.getProjectId());
            if (log.getCreateBy() != null) userIds.add(log.getCreateBy());
            if (log.getUpdateBy() != null && !"DRAFT".equals(log.getStatus())) {
                userIds.add(log.getUpdateBy());
            }
        }
        Map<String, WhPmCharter> projectMap = charterBo.getByIds(projectIds);
        Map<String, String> userNameMap = sysUserBo.getRealNameMap(userIds);

        for (WhPmWorkLog log : logs) {
            WhPmCharter project = projectMap.get(log.getProjectId());
            if (project != null) {
                log.setProjectName(project.getProjectName());
                log.setProjectShortName(project.getProjectShortName());
            }
            String realName = userNameMap.get(log.getCreateBy());
            if (realName != null) {
                log.setCreateByName(realName);
            }
            // Resolve approver name from updateBy
            String updateBy = log.getUpdateBy();
            if (updateBy != null && !"DRAFT".equals(log.getStatus())) {
                String approverName = userNameMap.get(updateBy);
                if (approverName != null) {
                    log.setApproverName(approverName);
                }
            }
        }
    }

    /**
     * Get work log by ID
     */
    public WhPmWorkLog getById(String id) {
        WhPmWorkLog log = workLogDao.selectById(id);
        if (log == null || "1".equals(log.getDelFlag())) {
            throw new ServiceException(404, "工时记录不存在");
        }
        return log;
    }

    /**
     * Create a new work log entry
     */
    @Transactional
    public WhPmWorkLog create(String userId, WorkLogCreateRequest req) {
        // Validate: not future date
        validateNotFuture(req.getLogDate());

        // Validate: user has permission for this project
        validateProjectPermission(userId, req.getProjectId());

        // Validate: daily hours <= 24
        validateDailyHours(userId, req.getLogDate(), new BigDecimal(req.getHoursWorked()), null);

        WhPmWorkLog log = new WhPmWorkLog();
        log.setProjectId(req.getProjectId());
        log.setLogDate(req.getLogDate());
        log.setHoursWorked(req.getHoursWorked());
        log.setWorkDescription(req.getWorkDescription());
        log.setStatus("DRAFT");
        log.setCreateBy(userId);
        log.setDelFlag("0");
        log.setVerNo(0);
        workLogDao.insert(log);
        return log;
    }

    /**
     * Update a work log entry
     */
    @Transactional
    public WhPmWorkLog update(String userId, WorkLogUpdateRequest req) {
        WhPmWorkLog existing = getById(req.getId());

        // Only allow updating DRAFT or REJECTED logs
        if (!"DRAFT".equals(existing.getStatus()) && !"REJECTED".equals(existing.getStatus())) {
            throw new ServiceException(400, "已审批的工时不可修改");
        }

        // Only allow updating own logs
        if (!userId.equals(existing.getCreateBy())) {
            throw new ServiceException(403, "只能修改自己的工时记录");
        }

        // Validate: not future date
        validateNotFuture(req.getLogDate());

        // Validate: daily hours <= 24
        validateDailyHours(userId, req.getLogDate(), new BigDecimal(req.getHoursWorked()), req.getId());

        existing.setProjectId(req.getProjectId());
        existing.setLogDate(req.getLogDate());
        existing.setHoursWorked(req.getHoursWorked());
        existing.setWorkDescription(req.getWorkDescription());
        existing.setUpdateBy(userId);
        workLogDao.updateById(existing);
        return existing;
    }

    /**
     * Delete a work log entry (soft delete)
     */
    @Transactional
    public void delete(String userId, String id) {
        WhPmWorkLog existing = getById(id);

        if (!userId.equals(existing.getCreateBy())) {
            throw new ServiceException(403, "只能删除自己的工时记录");
        }

        if (!"DRAFT".equals(existing.getStatus()) && !"REJECTED".equals(existing.getStatus())) {
            throw new ServiceException(400, "已审批的工时不可删除");
        }

        workLogDao.deleteById(id);
    }

    /**
     * Resubmit a rejected work log
     */
    @Transactional
    public void resubmit(String userId, String id) {
        WhPmWorkLog existing = getById(id);

        if (!userId.equals(existing.getCreateBy())) {
            throw new ServiceException(403, "只能重新提交自己的工时记录");
        }

        if (!"REJECTED".equals(existing.getStatus())) {
            throw new ServiceException(400, "只能重新提交被驳回的工时记录");
        }

        existing.setStatus("DRAFT");
        existing.setBlockerReason(null);
        existing.setUpdateBy(userId);
        workLogDao.updateById(existing);
    }

    /**
     * Approve a single work log
     */
    @Transactional
    public void approve(String pmId, String id, String comment) {
        WhPmWorkLog log = getById(id);

        // Verify PM owns this project
        WhPmCharter project = charterBo.getByIdSilent(log.getProjectId());
        if (project == null || !pmId.equals(project.getPmId())) {
            throw new ServiceException(403, "您不是该项目的经理，无权审批");
        }

        if (!"DRAFT".equals(log.getStatus())) {
            throw new ServiceException(400, "只能审批待审批的工时记录");
        }

        log.setStatus("APPROVED");
        log.setUpdateBy(pmId);
        workLogDao.updateById(log);
    }

    /**
     * Reject a single work log
     */
    @Transactional
    public void reject(String pmId, String id, String reason) {
        WhPmWorkLog log = getById(id);

        WhPmCharter project = charterBo.getByIdSilent(log.getProjectId());
        if (project == null || !pmId.equals(project.getPmId())) {
            throw new ServiceException(403, "您不是该项目的经理，无权审批");
        }

        if (!"DRAFT".equals(log.getStatus())) {
            throw new ServiceException(400, "只能审批待审批的工时记录");
        }

        log.setStatus("REJECTED");
        log.setBlockerReason(reason);
        log.setUpdateBy(pmId);
        workLogDao.updateById(log);
    }

    /**
     * Batch approve work logs
     */
    @Transactional
    public Map<String, Object> batchApprove(String pmId, List<String> ids) {
        int success = 0;
        int skipped = 0;
        List<String> skippedIds = new ArrayList<>();

        for (String id : ids) {
            try {
                approve(pmId, id, null);
                success++;
            } catch (ServiceException e) {
                skipped++;
                skippedIds.add(id);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("skipped", skipped);
        result.put("skippedIds", skippedIds);
        return result;
    }

    /**
     * Batch reject work logs
     */
    @Transactional
    public Map<String, Object> batchReject(String pmId, List<String> ids, String reason) {
        int success = 0;
        int skipped = 0;
        List<String> skippedIds = new ArrayList<>();

        for (String id : ids) {
            try {
                reject(pmId, id, reason);
                success++;
            } catch (ServiceException e) {
                skipped++;
                skippedIds.add(id);
            }
        }

        Map<String, Object> result = new HashMap<>();
        result.put("success", success);
        result.put("skipped", skipped);
        result.put("skippedIds", skippedIds);
        return result;
    }

    /**
     * Calculate work hours stats for a user in a month
     */
    public WorkHoursStatsVO getStats(String userId, String year, int month) {
        YearMonth ym = YearMonth.of(Integer.parseInt(year), month);
        LocalDate start = ym.atDay(1);
        LocalDate end = ym.atEndOfMonth();
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        WorkHoursStatsVO stats = new WorkHoursStatsVO();
        List<String> workDays = new ArrayList<>();
        BigDecimal monthTarget = BigDecimal.ZERO;

        // Collect work days and target hours
        LocalDate date = start;
        while (!date.isAfter(end)) {
            String dateStr = date.format(fmt);
            if (calendarBo.isWorkDay(dateStr)) {
                workDays.add(dateStr);
                String hours = calendarBo.getStandardHours(dateStr);
                monthTarget = monthTarget.add(new BigDecimal(hours));
            }
            date = date.plusDays(1);
        }

        stats.setWorkDays(workDays.size());
        stats.setMonthTarget(monthTarget);

        // Query user's work logs for the month (include DRAFT, APPROVED, REJECTED)
        String startDateStr = start.format(fmt);
        String endDateStr = end.format(fmt);

        LambdaQueryWrapper<WhPmWorkLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmWorkLog::getCreateBy, userId)
               .ge(WhPmWorkLog::getLogDate, startDateStr)
               .le(WhPmWorkLog::getLogDate, endDateStr)
               .eq(WhPmWorkLog::getDelFlag, "0");
        List<WhPmWorkLog> logs = workLogDao.selectList(wrapper);

        // Group by date
        Map<String, BigDecimal> dailyHours = new HashMap<>();
        for (WhPmWorkLog log : logs) {
            String d = log.getLogDate();
            BigDecimal h = new BigDecimal(log.getHoursWorked() != null ? log.getHoursWorked() : "0");
            dailyHours.merge(d, h, BigDecimal::add);
        }

        int filledDays = 0;
        int unfilledDays = 0;
        int partialDays = 0;
        BigDecimal actualHours = BigDecimal.ZERO;
        BigDecimal gapHours = BigDecimal.ZERO;

        for (String wd : workDays) {
            BigDecimal dayHours = dailyHours.getOrDefault(wd, BigDecimal.ZERO);
            String stdHours = calendarBo.getStandardHours(wd);
            BigDecimal std = new BigDecimal(stdHours);
            actualHours = actualHours.add(dayHours);

            if (dayHours.compareTo(BigDecimal.ZERO) == 0) {
                unfilledDays++;
                gapHours = gapHours.add(std);
            } else if (dayHours.compareTo(std) < 0) {
                partialDays++;
                gapHours = gapHours.add(std.subtract(dayHours));
            } else {
                filledDays++;
            }
        }

        stats.setFilledDays(filledDays);
        stats.setUnfilledDays(unfilledDays);
        stats.setPartialDays(partialDays);
        stats.setGapHours(gapHours.setScale(2, RoundingMode.HALF_UP));
        stats.setActualHours(actualHours.setScale(2, RoundingMode.HALF_UP));

        return stats;
    }

    // --- Private validation helpers ---

    private void validateNotFuture(String logDate) {
        LocalDate today = LocalDate.now();
        LocalDate date = LocalDate.parse(logDate, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        if (date.isAfter(today)) {
            throw new ServiceException(400, "不能录入未来日期的工时");
        }
    }

    private void validateProjectPermission(String userId, String projectId) {
        // Check if user is PM of the project
        WhPmCharter project = charterBo.getByIdSilent(projectId);
        if (project != null && userId.equals(project.getPmId())) {
            return;
        }

        // Check if user is owner or planned owner of any WBS element in the project
        LambdaQueryWrapper<WhPmWbsElement> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmWbsElement::getProjectId, projectId)
               .eq(WhPmWbsElement::getDelFlag, "0")
               .and(w -> w.eq(WhPmWbsElement::getOwnerId, userId)
                          .or()
                          .eq(WhPmWbsElement::getPlannedOwnerId, userId));
        long count = wbsElementDao.selectCount(wrapper);
        if (count == 0) {
            throw new ServiceException(403, "您没有该项目的工时录入权限");
        }
    }

    private void validateDailyHours(String userId, String logDate, BigDecimal newHours, String excludeId) {
        BigDecimal existingHours = workLogDao.sumHoursByUserAndDate(userId, logDate);
        if (existingHours == null) {
            existingHours = BigDecimal.ZERO;
        }

        // If updating, subtract the old value
        if (excludeId != null) {
            WhPmWorkLog old = workLogDao.selectById(excludeId);
            if (old != null) {
                existingHours = existingHours.subtract(new BigDecimal(old.getHoursWorked()));
            }
        }

        BigDecimal total = existingHours.add(newHours);
        if (total.compareTo(new BigDecimal("24")) > 0) {
            throw new ServiceException(400, "当日累计工时不得超过24小时（当前已录入：" +
                    existingHours.setScale(1, RoundingMode.HALF_UP) + "h，本次：" +
                    newHours.setScale(1, RoundingMode.HALF_UP) + "h）");
        }
    }
}
