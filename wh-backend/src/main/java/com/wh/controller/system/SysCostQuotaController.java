package com.wh.controller.system;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.wh.common.R;
import com.wh.dao.system.SysCostQuotaDao;
import com.wh.dao.system.SysCostYearDao;
import com.wh.dao.system.SysPositionDao;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.system.SysCostQuota;
import com.wh.entity.system.SysCostYear;
import com.wh.entity.system.SysPosition;
import com.wh.entity.system.SysUser;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/system/cost-quota")
public class SysCostQuotaController {

    private final SysPositionDao positionDao;
    private final SysCostYearDao yearDao;
    private final SysCostQuotaDao quotaDao;
    private final SysUserDao userDao;

    public SysCostQuotaController(SysPositionDao positionDao,
                                   SysCostYearDao yearDao,
                                   SysCostQuotaDao quotaDao,
                                   SysUserDao userDao) {
        this.positionDao = positionDao;
        this.yearDao = yearDao;
        this.quotaDao = quotaDao;
        this.userDao = userDao;
    }

    // ==================== 岗位管理 ====================

    @GetMapping("/positions")
    public R<List<SysPosition>> listPositions() {
        LambdaQueryWrapper<SysPosition> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysPosition::getDelFlag, "0")
               .orderByAsc(SysPosition::getSortOrder);
        return R.ok(positionDao.selectList(wrapper));
    }

    @PostMapping("/positions")
    public R<SysPosition> createPosition(@RequestBody SysPosition position) {
        position.setId(null);
        position.setIsDefault("0");
        positionDao.insert(position);
        return R.ok(position);
    }

    @PutMapping("/positions/{id}")
    public R<SysPosition> updatePosition(@PathVariable String id, @RequestBody SysPosition position) {
        position.setId(id);
        positionDao.updateById(position);
        return R.ok(position);
    }

    @DeleteMapping("/positions/{id}")
    public R<Void> deletePosition(@PathVariable String id) {
        SysPosition pos = positionDao.selectById(id);
        if (pos == null || "1".equals(pos.getDelFlag())) {
            return R.fail(404, "岗位不存在");
        }
        LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
        quotaWrapper.eq(SysCostQuota::getPositionId, id)
                    .eq(SysCostQuota::getDelFlag, "0");
        if (quotaDao.selectCount(quotaWrapper) > 0) {
            return R.fail(400, "该岗位已有定额记录，不可删除");
        }
        LambdaQueryWrapper<SysUser> userWrapper = new LambdaQueryWrapper<>();
        userWrapper.eq(SysUser::getPositionId, id)
                   .eq(SysUser::getDelFlag, "0");
        if (userDao.selectCount(userWrapper) > 0) {
            return R.fail(400, "该岗位下已有用户，不可删除");
        }
        positionDao.deleteById(id);
        return R.ok();
    }

    // ==================== 年份管理 ====================

    @GetMapping("/years")
    public R<List<SysCostYear>> listYears() {
        LambdaQueryWrapper<SysCostYear> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCostYear::getDelFlag, "0")
               .orderByDesc(SysCostYear::getStartDate);
        return R.ok(yearDao.selectList(wrapper));
    }

    @PostMapping("/years")
    public R<SysCostYear> createYear(@RequestBody SysCostYear year) {
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
        return R.ok(year);
    }

    @PutMapping("/years/{id}")
    public R<SysCostYear> updateYear(@PathVariable String id, @RequestBody SysCostYear year) {
        year.setId(id);
        yearDao.updateById(year);
        return R.ok(year);
    }

    @DeleteMapping("/years/{id}")
    public R<Void> deleteYear(@PathVariable String id) {
        SysCostYear year = yearDao.selectById(id);
        if (year == null || "1".equals(year.getDelFlag())) {
            return R.fail(404, "年份不存在");
        }
        LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
        quotaWrapper.eq(SysCostQuota::getYearId, id)
                    .eq(SysCostQuota::getDelFlag, "0");
        if (quotaDao.selectCount(quotaWrapper) > 0) {
            return R.fail(400, "该年份已有定额记录，不可删除");
        }
        yearDao.deleteById(id);
        return R.ok();
    }

    @GetMapping("/years/latest/default-start-date")
    public R<Map<String, String>> getDefaultStartDate() {
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
        return R.ok(result);
    }

    // ==================== 定额管理 ====================

    @GetMapping("/quotas")
    public R<List<Map<String, Object>>> listQuotas(@RequestParam String yearId) {
        // 获取所有岗位
        LambdaQueryWrapper<SysPosition> posWrapper = new LambdaQueryWrapper<>();
        posWrapper.eq(SysPosition::getDelFlag, "0")
                  .orderByAsc(SysPosition::getSortOrder);
        List<SysPosition> positions = positionDao.selectList(posWrapper);

        // 获取每个岗位在该年份的最新版本定额
        List<Map<String, Object>> result = new ArrayList<>();
        for (SysPosition pos : positions) {
            LambdaQueryWrapper<SysCostQuota> quotaWrapper = new LambdaQueryWrapper<>();
            quotaWrapper.eq(SysCostQuota::getPositionId, pos.getId())
                        .eq(SysCostQuota::getYearId, yearId)
                        .eq(SysCostQuota::getDelFlag, "0")
                        .orderByDesc(SysCostQuota::getVersionNo)
                        .last("LIMIT 1");
            SysCostQuota latest = quotaDao.selectOne(quotaWrapper);

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
        return R.ok(result);
    }

    @PostMapping("/quotas/adjust")
    public R<SysCostQuota> adjustQuota(@RequestBody Map<String, Object> body) {
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
            return R.fail(400, "该岗位在该日期已有生效定额");
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

        return R.ok(quota);
    }

    @GetMapping("/quotas/current-rate")
    public R<Map<String, Object>> getCurrentRate(@RequestParam String positionId) {
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
            return R.ok(result); // no matching year
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
            return R.ok(result); // no matching quota
        }

        SysPosition position = positionDao.selectById(positionId);
        BigDecimal costRate = matchedQuota.getDailyRate().divide(
                new BigDecimal("8"), 2, java.math.RoundingMode.HALF_UP);

        result.put("positionId", positionId);
        result.put("positionName", position != null ? position.getName() : null);
        result.put("yearId", matchedYear.getId());
        result.put("yearName", matchedYear.getName());
        result.put("dailyRate", matchedQuota.getDailyRate());
        result.put("costRate", costRate);
        result.put("effectiveDate", matchedQuota.getEffectiveDate());
        result.put("versionNo", matchedQuota.getVersionNo());
        return R.ok(result);
    }

    @GetMapping("/quotas/history")
    public R<List<Map<String, Object>>> getHistory(@RequestParam String positionId, @RequestParam String yearId) {
        LambdaQueryWrapper<SysCostQuota> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(SysCostQuota::getPositionId, positionId)
               .eq(SysCostQuota::getYearId, yearId)
               .eq(SysCostQuota::getDelFlag, "0")
               .orderByDesc(SysCostQuota::getVersionNo);
        List<SysCostQuota> quotas = quotaDao.selectList(wrapper);
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
            SysUser user = userDao.selectById(q.getCreateBy());
            map.put("createByName", user != null ? user.getRealName() : q.getCreateBy());
            result.add(map);
        }
        return R.ok(result);
    }

    @GetMapping("/quotas/compare")
    public R<Map<String, Object>> compareVersions(@RequestParam String quotaId1, @RequestParam String quotaId2) {
        SysCostQuota q1 = quotaDao.selectById(quotaId1);
        SysCostQuota q2 = quotaDao.selectById(quotaId2);
        Map<String, Object> result = new HashMap<>();
        result.put("version1", q1);
        result.put("version2", q2);
        result.put("rateDiff", q1.getDailyRate().subtract(q2.getDailyRate()));
        return R.ok(result);
    }

    private void copyLatestQuotas(String fromYearId, String toYearId) {
        List<SysPosition> positions = positionDao.selectList(
            new LambdaQueryWrapper<SysPosition>().eq(SysPosition::getDelFlag, "0"));
        for (SysPosition pos : positions) {
            LambdaQueryWrapper<SysCostQuota> wrapper = new LambdaQueryWrapper<>();
            wrapper.eq(SysCostQuota::getPositionId, pos.getId())
                   .eq(SysCostQuota::getYearId, fromYearId)
                   .eq(SysCostQuota::getDelFlag, "0")
                   .orderByDesc(SysCostQuota::getVersionNo)
                   .last("LIMIT 1");
            SysCostQuota latest = quotaDao.selectOne(wrapper);
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
}
