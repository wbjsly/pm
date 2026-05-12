package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.*;
import com.wh.common.R;
import com.wh.entity.pm.WhPmCharter;
import com.wh.vo.pm.CharterStatsVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/pm/charters")
public class WhPmCharterController {

    private final com.wh.bo.pm.WhPmCharterBo charterBo;

    public WhPmCharterController(com.wh.bo.pm.WhPmCharterBo charterBo) {
        this.charterBo = charterBo;
    }

    @GetMapping
    public R<IPage<WhPmCharter>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String pmId,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String progress) {
        return R.ok(charterBo.pageList(pageNum, pageSize, status, pmId, keyword, progress));
    }

    @GetMapping("/{id}")
    public R<WhPmCharter> detail(@PathVariable String id) {
        return R.ok(charterBo.getById(id));
    }

    @PostMapping
    public R<WhPmCharter> create(@RequestBody CharterCreateRequest req) {
        return R.ok(charterBo.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody CharterUpdateRequest req) {
        charterBo.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        charterBo.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable String id) {
        charterBo.submit(id);
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable String id, @RequestBody CharterSubmitRequest req) {
        charterBo.approve(id, req.getRejectReason());
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable String id, @RequestBody CharterSubmitRequest req) {
        charterBo.reject(id, req.getRejectReason());
        return R.ok();
    }

    @GetMapping("/stats")
    public R<CharterStatsVO> stats(@RequestParam String pmId) {
        return R.ok(charterBo.getStatsByPmId(pmId));
    }
}
