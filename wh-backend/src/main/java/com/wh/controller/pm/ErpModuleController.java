package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.ErpModuleBo;
import com.wh.common.R;
import com.wh.entity.pm.ErpModule;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pm/modules")
public class ErpModuleController {

    private final ErpModuleBo moduleBo;

    public ErpModuleController(ErpModuleBo moduleBo) {
        this.moduleBo = moduleBo;
    }

    @GetMapping
    public R<IPage<ErpModule>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String productId,
            @RequestParam(required = false) String keyword) {
        return R.ok(moduleBo.pageList(pageNum, pageSize, productId, keyword));
    }

    @GetMapping("/by-product/{productId}")
    public R<List<ErpModule>> listByProduct(@PathVariable String productId) {
        return R.ok(moduleBo.listByProductId(productId));
    }

    @GetMapping("/{id}")
    public R<ErpModule> detail(@PathVariable String id) {
        return R.ok(moduleBo.getById(id));
    }

    @PostMapping
    public R<ErpModule> create(@RequestBody ErpModule module) {
        return R.ok(moduleBo.create(module));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody ErpModule module) {
        moduleBo.update(id, module);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        moduleBo.delete(id);
        return R.ok();
    }
}
