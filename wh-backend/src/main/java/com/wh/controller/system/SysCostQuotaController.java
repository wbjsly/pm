package com.wh.controller.system;

import com.wh.bo.system.SysCostQuotaBo;
import com.wh.common.R;
import com.wh.entity.system.SysCostQuota;
import com.wh.entity.system.SysCostYear;
import com.wh.entity.system.SysPosition;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/cost-quota")
public class SysCostQuotaController {

    private final SysCostQuotaBo costQuotaBo;

    public SysCostQuotaController(SysCostQuotaBo costQuotaBo) {
        this.costQuotaBo = costQuotaBo;
    }

    // ==================== 岗位管理 ====================

    @GetMapping("/positions")
    public R<List<SysPosition>> listPositions() {
        return R.ok(costQuotaBo.listPositions());
    }

    @PostMapping("/positions")
    public R<SysPosition> createPosition(@RequestBody SysPosition position) {
        return R.ok(costQuotaBo.createPosition(position));
    }

    @PutMapping("/positions/{id}")
    public R<SysPosition> updatePosition(@PathVariable String id, @RequestBody SysPosition position) {
        return R.ok(costQuotaBo.updatePosition(id, position));
    }

    @DeleteMapping("/positions/{id}")
    public R<Void> deletePosition(@PathVariable String id) {
        costQuotaBo.deletePosition(id);
        return R.ok();
    }

    // ==================== 年份管理 ====================

    @GetMapping("/years")
    public R<List<SysCostYear>> listYears() {
        return R.ok(costQuotaBo.listYears());
    }

    @PostMapping("/years")
    public R<SysCostYear> createYear(@RequestBody SysCostYear year) {
        return R.ok(costQuotaBo.createYear(year));
    }

    @PutMapping("/years/{id}")
    public R<SysCostYear> updateYear(@PathVariable String id, @RequestBody SysCostYear year) {
        return R.ok(costQuotaBo.updateYear(id, year));
    }

    @DeleteMapping("/years/{id}")
    public R<Void> deleteYear(@PathVariable String id) {
        costQuotaBo.deleteYear(id);
        return R.ok();
    }

    @GetMapping("/years/latest/default-start-date")
    public R<Map<String, String>> getDefaultStartDate() {
        return R.ok(costQuotaBo.getDefaultStartDate());
    }

    // ==================== 定额管理 ====================

    @GetMapping("/quotas")
    public R<List<Map<String, Object>>> listQuotas(@RequestParam String yearId) {
        return R.ok(costQuotaBo.listQuotas(yearId));
    }

    @PostMapping("/quotas/adjust")
    public R<SysCostQuota> adjustQuota(@RequestBody Map<String, Object> body) {
        return R.ok(costQuotaBo.adjustQuota(body));
    }

    @GetMapping("/quotas/current-rate")
    public R<Map<String, Object>> getCurrentRate(@RequestParam String positionId) {
        return R.ok(costQuotaBo.getCurrentRate(positionId));
    }

    @GetMapping("/quotas/history")
    public R<List<Map<String, Object>>> getHistory(@RequestParam String positionId, @RequestParam String yearId) {
        return R.ok(costQuotaBo.getHistory(positionId, yearId));
    }

    @GetMapping("/quotas/compare")
    public R<Map<String, Object>> compareVersions(@RequestParam String quotaId1, @RequestParam String quotaId2) {
        return R.ok(costQuotaBo.compareVersions(quotaId1, quotaId2));
    }
}
