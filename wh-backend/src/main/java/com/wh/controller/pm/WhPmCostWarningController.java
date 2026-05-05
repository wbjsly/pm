package com.wh.controller.pm;

import com.wh.bo.pm.WhPmCostWarningBo;
import com.wh.common.R;
import com.wh.entity.pm.WhPmCostWarning;
import com.wh.util.SecurityUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pm/cost-warnings")
public class WhPmCostWarningController {

    private final WhPmCostWarningBo costWarningBo;

    public WhPmCostWarningController(WhPmCostWarningBo costWarningBo) {
        this.costWarningBo = costWarningBo;
    }

    @GetMapping
    public R<List<WhPmCostWarning>> list(
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String status) {
        return R.ok(costWarningBo.listByProjectId(projectId, status));
    }

    @PostMapping("/{id}/close")
    public R<Void> close(@PathVariable String id) {
        String userId = SecurityUtils.getCurrentUserId();
        costWarningBo.closeWarning(id, userId);
        return R.ok();
    }

    @PostMapping("/trigger")
    public R<Void> trigger() {
        costWarningBo.triggerCalculation();
        return R.ok();
    }

    @GetMapping("/active")
    public R<List<WhPmCostWarning>> listActive(
            @RequestParam(defaultValue = "5") int limit) {
        return R.ok(costWarningBo.listActiveAll(limit));
    }
}
