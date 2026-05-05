package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.ActualCostCreateRequest;
import com.wh.bo.pm.WhPmActualCostBo;
import com.wh.common.R;
import com.wh.entity.pm.WhPmActualCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/actual-costs")
public class WhPmActualCostController {

    private final WhPmActualCostBo actualCostBo;

    public WhPmActualCostController(WhPmActualCostBo actualCostBo) {
        this.actualCostBo = actualCostBo;
    }

    @GetMapping
    public R<IPage<WhPmActualCost>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String budgetItemId,
            @RequestParam(required = false) String sourceSystem) {
        return R.ok(actualCostBo.pageList(pageNum, pageSize, projectId, budgetItemId, sourceSystem));
    }

    @GetMapping("/{id}")
    public R<WhPmActualCost> detail(@PathVariable String id) {
        return R.ok(actualCostBo.getById(id));
    }

    @PostMapping
    public R<WhPmActualCost> create(@RequestBody ActualCostCreateRequest req) {
        return R.ok(actualCostBo.create(req));
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        actualCostBo.delete(id);
        return R.ok();
    }
}
