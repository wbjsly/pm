package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.*;
import com.wh.common.R;
import com.wh.entity.pm.WhPmBudget;
import com.wh.vo.pm.BudgetComparisonVO;
import com.wh.vo.pm.BudgetDetailVO;
import com.wh.vo.pm.BudgetVersionVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pm/budgets")
public class WhPmBudgetController {

    private final WhPmBudgetBo budgetBo;

    public WhPmBudgetController(WhPmBudgetBo budgetBo) {
        this.budgetBo = budgetBo;
    }

    @GetMapping
    public R<IPage<WhPmBudget>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String pmId,
            @RequestParam(required = false) String status) {
        return R.ok(budgetBo.pageList(pageNum, pageSize, projectId, pmId, status));
    }

    @GetMapping("/{id}")
    public R<WhPmBudget> detail(@PathVariable String id) {
        return R.ok(budgetBo.getById(id));
    }

    @GetMapping("/{id}/detail")
    public R<BudgetDetailVO> detailWithItems(@PathVariable String id) {
        return R.ok(budgetBo.getDetailWithItems(id));
    }

    @PostMapping
    public R<WhPmBudget> create(@RequestBody BudgetCreateRequest req) {
        return R.ok(budgetBo.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody BudgetUpdateRequest req) {
        budgetBo.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        budgetBo.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable String id) {
        budgetBo.submit(id);
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable String id, @RequestBody BudgetApprovalRequest req) {
        budgetBo.approve(id, req.getComment());
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable String id, @RequestBody BudgetApprovalRequest req) {
        budgetBo.reject(id, req.getComment());
        return R.ok();
    }

    @GetMapping("/{id}/comparison")
    public R<BudgetComparisonVO> comparison(
            @PathVariable String id,
            @RequestParam(required = false) String version) {
        return R.ok(budgetBo.getComparison(id, version));
    }

    @GetMapping("/versions/{projectId}")
    public R<List<BudgetVersionVO>> versions(@PathVariable String projectId) {
        return R.ok(budgetBo.getVersionHistory(projectId));
    }
}
