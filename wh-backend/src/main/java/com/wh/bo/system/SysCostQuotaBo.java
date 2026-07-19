package com.wh.bo.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.ServiceException;
import com.wh.dao.system.SysCostQuotaDao;
import com.wh.dao.system.SysCostYearDao;
import com.wh.dao.system.SysPositionDao;
import com.wh.entity.system.SysCostQuota;
import com.wh.entity.system.SysCostYear;
import com.wh.entity.system.SysPosition;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * 费用定额业务逻辑：岗位、定额年份、定额版本管理。
 */
@Service
public class SysCostQuotaBo {

    private final SysPositionDao positionDao;
    private final SysCostYearDao yearDao;
    private final SysCostQuotaDao quotaDao;
    private final SysUserBo sysUserBo;

    public SysCostQuotaBo(SysPositionDao positionDao,
                          SysCostYearDao yearDao,
                          SysCostQuotaDao quotaDao,
                          SysUserBo sysUserBo) {
        this.positionDao = positionDao;
        this.yearDao = yearDao;
        this.quotaDao = quotaDao;
        this.sysUserBo = sysUserBo;
    }

    // ==================== 岗位管理 ====================

    public List<SysPosition> listPositions() {
        LambdaQueryWrapper<SysPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPosition::getDelFlag, "0")
               .orderByAsc(SysPosition::getSortOrder);
        return positionDao.selectList(wrapper);
    }

    public SysPosition createPosition(SysPosition position) {
        position.setId(null);
        position.setIsDefault("0");
        positionDao.insert(position);
        return position;
    }

    public SysPosition updatePosition(String id, SysPosition position) {
        position.setId(id);
        positionDao.updateById(position);
        return position;
    }

    public void deletePosition(String id) {
        SysPosition pos = positionDao.selectById(id);
        if (pos == null || "1".equals(pos.getDelFlag())) {
            throw new ServiceException(404, "岗位不存在");
        }
        LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
        quotaWrapper.eq(SysCostQuota::getPositionId, id)
                    .eq(SysCostQuota::getDelFlag, "0");
        if (quotaDao.selectCount(quotaWrapper) > 0) {
            throw new ServiceException(400, "该岗位已有定额记录，不可删除");
        }
        if (sysUserBo.countUsersByPositionId(id) > 0) {
            throw new ServiceException(400, "该岗位下已有用户，不可删除");
        }
        positionDao.deleteById(id);
    }

    // ==================== 年份管理 ====================

    public List<SysCostYear> listYears() {
        LambdaQueryWrapper<SysCostYear> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCostYear::getDelFlag, "0")
               .orderByDesc(SysCostYear::getStartDate);
        return yearDao.selectList(wrapper);
    }

    @Transactional
    public SysCostYear createYear(SysCostYear year) {
        year.setId(null);
        yearDao.insert(year);
        // 复制上一年的最新版本定额
        LambdaQueryWrapper<SysCostYear> prevWrapper = new LambdaQueryWrapper<>();
        prevWrapper.eq(SysCostYear::getDelFlag, "0")
                   .lt(SysCostYear::getStartDate, year.getStartDate())
                   .orderByDesc(SysCostYear::getStartDate)
                   .last("LIMIT 1");
        SysCostYear prevYear = yearDao.selectOne(prevWrapper);
        if (prevYear != null) {
            copyLatestQuotas(prevYear.getId(), year.getId());
        }
        return year;
    }

    public SysCostYear updateYear(String id, SysCostYear year) {
        year.setId(id);
        yearDao.updateById(year);
        return year;
    }

    public void deleteYear(String id) {
        SysCostYear year = yearDao.selectById(id);
        if (year == null || "1".equals(year.getDelFlag())) {
            throw new ServiceException(404, "年份不存在");
        }
        LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
        quotaWrapper.eq(SysCostQuota::getYearId, id)
                    .eq(SysCostQuota::getDelFlag, "0");
        if (quotaDao.selectCount(quotaWrapper) > 0) {
            throw new ServiceException(400, "该年份已有定额记录，不可删除");
        }
        yearDao.deleteById(id);
    }

    public Map<String, String> getDefaultStartDate() {
        LambdaQueryWrapper<SysCostYear> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCostYear::getDelFlag, "0")
               .orderByDesc(SysCostYear::getEndDate)
               .last("LIMIT 1");
        SysCostYear latest = yearDao.selectOne(wrapper);
        Map<String, String> result = new HashMap<>();
        if (latest != null) {
            result.put("defaultStartDate", latest.getEndDate());
        } else {
            result.put("defaultStartDate", LocalDate.now().toString());
        }
        return result;
    }

    // ==================== 定额管理 ====================

    public List<Map<String, Object>> listQuotas(String yearId) {
        List<SysPosition> positions = listPositions();

        // 批量加载该年份全部定额，按岗位分组取最新版本，避免逐岗位查询
        LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
        quotaWrapper.eq(SysCostQuota::getYearId, yearId)
                    .eq(SysCostQuota::getDelFlag, "0");
        Map<String, SysCostQuota> latestByPosition = latestVersionByPosition(
                quotaDao.selectList(quotaWrapper));

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPosition pos : positions) {
            SysCostQuota latest = latestByPosition.get(pos.getId());
            Map<String, Object> item = new HashMap<>();
            item.put("positionId", pos.getId());
            item.put("positionName", pos.getName());
            item.put("isDefault", pos.getIsDefault());
            if (latest != null) {
                item.put("quotaId", latest.getId());
                item.put("dailyRate", latest.getDailyRate());
                item.put("versionNo", latest.getVersionNo());
                item.put("effectiveDate", latest.getEffectiveDate());
                item.put("updateDate", latest.getUpdateDate());
            } else {
                item.put("quotaId", null);
                item.put("dailyRate", null);
                item.put("versionNo", null);
                item.put("effectiveDate", null);
                item.put("updateDate", null);
            }
            result.add(item);
        }
        return result;
    }

    public SysCostQuota adjustQuota(Map<String, Object> body) {
        String positionId = (String) body.get("positionId");
        String yearId = (String) body.get("yearId");
        BigDecimal newRate = new BigDecimal(body.get("dailyRate").toString());
        String effectiveDate = (String) body.getOrDefault("effectiveDate", LocalDate.now().toString());
        String changeReason = (String) body.get("changeReason");

        // 校验同一岗位同一天只有一个生效定额
        LambdaQueryWrapper<SysCostQuota> conflictWrapper = new LambdaQueryWrapper<>();
        conflictWrapper.eq(SysCostQuota::getPositionId, positionId)
                       .eq(SysCostQuota::getEffectiveDate, effectiveDate)
                       .eq(SysCostQuota::getDelFlag, "0");
        if (quotaDao.selectCount(conflictWrapper) > 0) {
            throw new ServiceException(400, "该岗位在该日期已有生效定额");
        }

        // 获取当前最大版本号
        LambdaQueryWrapper<SysCostQuota> maxVersionWrapper = new LambdaQueryWrapper<>();
        maxVersionWrapper.eq(SysCostQuota::getPositionId, positionId)
                         .eq(SysCostQuota::getYearId, yearId)
                         .eq(SysCostQuota::getDelFlag, "0")
                         .orderByDesc(SysCostQuota::getVersionNo)
                         .last("LIMIT 1");
        SysCostQuota latest = quotaDao.selectOne(maxVersionWrapper);
        int newVersion = (latest != null) ? latest.getVersionNo() + 1 : 1;

        SysCostQuota quota = new SysCostQuota();
        quota.setPositionId(positionId);
        quota.setYearId(yearId);
        quota.setVersionNo(newVersion);
        quota.setDailyRate(newRate);
        quota.setEffectiveDate(effectiveDate);
        quota.setChangeReason(changeReason);
        quotaDao.insert(quota);

        return quota;
    }

    public Map<String, Object> getCurrentRate(String positionId) {
        LocalDate today = LocalDate.now();
        Map<String, Object> result = new LinkedHashMap<>();

        // Find year where today falls within startDate~endDate
        LambdaQueryWrapper<SysCostYear> yearWrapper = new LambdaQueryWrapper<>();
        yearWrapper.eq(SysCostYear::getDelFlag, "0")
                   .le(SysCostYear::getStartDate, today.toString())
                   .ge(SysCostYear::getEndDate, today.toString())
                   .orderByDesc(SysCostYear::getStartDate)
                   .last("LIMIT 1");
        SysCostYear matchedYear = yearDao.selectOne(yearWrapper);

        if (matchedYear == null) {
            return result; // no matching year
        }

        // Find latest version quota for this position+year where effectiveDate <= today
        LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
        quotaWrapper.eq(SysCostQuota::getPositionId, positionId)
                    .eq(SysCostQuota::getYearId, matchedYear.getId())
                    .eq(SysCostQuota::getDelFlag, "0")
                    .le(SysCostQuota::getEffectiveDate, today.toString())
                    .orderByDesc(SysCostQuota::getVersionNo)
                    .last("LIMIT 1");
        SysCostQuota matchedQuota = quotaDao.selectOne(quotaWrapper);

        if (matchedQuota == null) {
            return result; // no matching quota
        }

        SysPosition position = positionDao.selectById(positionId);
        BigDecimal costRate = matchedQuota.getDailyRate().divide(
                new BigDecimal("8"), 2, RoundingMode.HALF_UP);

        result.put("positionId", positionId);
        result.put("positionName", position != null ? position.getName() : null);
        result.put("yearId", matchedYear.getId());
        result.put("yearName", matchedYear.getName());
        result.put("dailyRate", matchedQuota.getDailyRate());
        result.put("costRate", costRate);
        result.put("effectiveDate", matchedQuota.getEffectiveDate());
        result.put("versionNo", matchedQuota.getVersionNo());
        return result;
    }

    public List<Map<String, Object>> getHistory(String positionId, String yearId) {
        LambdaQueryWrapper<SysCostQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCostQuota::getPositionId, positionId)
               .eq(SysCostQuota::getYearId, yearId)
               .eq(SysCostQuota::getDelFlag, "0")
               .orderByDesc(SysCostQuota::getVersionNo);
        List<SysCostQuota> quotas = quotaDao.selectList(wrapper);

        // 批量加载创建人姓名，避免逐条查询
        Set<String> creatorIds = new HashSet<>();
        for (SysCostQuota q : quotas) {
            creatorIds.add(q.getCreateBy());
        }
        Map<String, String> realNameMap = sysUserBo.getRealNameMap(creatorIds);

        List<Map<String, Object>> result = new ArrayList<>();
        for (SysCostQuota q : quotas) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("id", q.getId());
            map.put("positionId", q.getPositionId());
            map.put("yearId", q.getYearId());
            map.put("versionNo", q.getVersionNo());
            map.put("dailyRate", q.getDailyRate());
            map.put("effectiveDate", q.getEffectiveDate());
            map.put("changeReason", q.getChangeReason());
            map.put("createDate", q.getCreateDate());
            map.put("createBy", q.getCreateBy());
            map.put("createByName", realNameMap.getOrDefault(q.getCreateBy(), q.getCreateBy()));
            result.add(map);
        }
        return result;
    }

    public Map<String, Object> compareVersions(String quotaId1, String quotaId2) {
        SysCostQuota q1 = quotaDao.selectById(quotaId1);
        SysCostQuota q2 = quotaDao.selectById(quotaId2);
        Map<String, Object> result = new HashMap<>();
        result.put("version1", q1);
        result.put("version2", q2);
        result.put("rateDiff", q1.getDailyRate().subtract(q2.getDailyRate()));
        return result;
    }

    private void copyLatestQuotas(String fromYearId, String toYearId) {
        List<SysPosition> positions = listPositions();

        // 批量加载来源年份全部定额，按岗位取最新版本
        LambdaQueryWrapper<SysCostQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCostQuota::getYearId, fromYearId)
               .eq(SysCostQuota::getDelFlag, "0");
        Map<String, SysCostQuota> latestByPosition = latestVersionByPosition(
                quotaDao.selectList(wrapper));

        for (SysPosition pos : positions) {
            SysCostQuota latest = latestByPosition.get(pos.getId());
            if (latest != null) {
                SysCostQuota copy = new SysCostQuota();
                copy.setPositionId(pos.getId());
                copy.setYearId(toYearId);
                copy.setVersionNo(1);
                copy.setDailyRate(latest.getDailyRate());
                copy.setEffectiveDate(LocalDate.now().toString());
                copy.setChangeReason("从上年复制");
                quotaDao.insert(copy);
            }
        }
    }

    /**
     * 按岗位分组取 versionNo 最大的定额记录。
     */
    private Map<String, SysCostQuota> latestVersionByPosition(List<SysCostQuota> quotas) {
        Map<String, SysCostQuota> result = new HashMap<>();
        for (SysCostQuota q : quotas) {
            SysCostQuota existing = result.get(q.getPositionId());
            if (existing == null
                    || (q.getVersionNo() != null && (existing.getVersionNo() == null
                        || q.getVersionNo() > existing.getVersionNo()))) {
                result.put(q.getPositionId(), q);
            }
        }
        return result;
    }
}
