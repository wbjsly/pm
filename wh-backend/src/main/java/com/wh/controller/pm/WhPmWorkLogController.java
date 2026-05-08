package com.wh.controller.pm;

import com.wh.bo.pm.*;
import com.wh.common.R;
import com.wh.entity.pm.WhPmWorkLog;
import com.wh.util.SecurityUtils;
import com.wh.vo.pm.WorkHoursStatsVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pm/work-hours")
public class WhPmWorkLogController {

    private final WhPmWorkLogBo workLogBo;

    public WhPmWorkLogController(WhPmWorkLogBo workLogBo) {
        this.workLogBo = workLogBo;
    }

    @GetMapping
    public R<List<WhPmWorkLog>> list(@RequestParam String year, @RequestParam int month) {
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.getByUserAndMonth(userId, year, month));
    }

    @GetMapping("/pending")
    public R<List<WhPmWorkLog>> pending(@RequestParam String year, @RequestParam int month) {
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.getPendingByPm(userId, year, month));
    }

    @GetMapping("/{id}")
    public R<WhPmWorkLog> get(@PathVariable String id) {
        return R.ok(workLogBo.getById(id));
    }

    @PostMapping
    public R<WhPmWorkLog> create(@RequestBody WorkLogCreateRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.create(userId, req));
    }

    @PutMapping("/{id}")
    public R<WhPmWorkLog> update(@PathVariable String id, @RequestBody WorkLogUpdateRequest req) {
        req.setId(id);
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.update(userId, req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        workLogBo.delete(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/resubmit")
    public R<Void> resubmit(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        workLogBo.resubmit(userId, id);
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable String id, @RequestBody(required = false) WorkLogApprovalRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        String comment = req != null ? req.getReason() : null;
        workLogBo.approve(userId, id, comment);
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable String id, @RequestBody WorkLogApprovalRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        workLogBo.reject(userId, id, req.getReason());
        return R.ok();
    }

    @PostMapping("/batch-approve")
    public R<Map<String, Object>> batchApprove(@RequestBody WorkLogApprovalRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.batchApprove(userId, req.getIds()));
    }

    @PostMapping("/batch-reject")
    public R<Map<String, Object>> batchReject(@RequestBody WorkLogApprovalRequest req) {
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.batchReject(userId, req.getIds(), req.getReason()));
    }

    @GetMapping("/by-project")
    public R<List<WhPmWorkLog>> byProject(@RequestParam String projectId,
                                           @RequestParam String year,
                                           @RequestParam int month) {
        return R.ok(workLogBo.getByProjectAndMonth(projectId, year, month));
    }

    @GetMapping("/stats")
    public R<WorkHoursStatsVO> stats(@RequestParam String year, @RequestParam int month) {
        String userId = SecurityUtils.getCurrentUserId();
        return R.ok(workLogBo.getStats(userId, year, month));
    }
}
