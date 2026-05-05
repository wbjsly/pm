package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.ErpProductBo;
import com.wh.common.R;
import com.wh.entity.pm.ErpProduct;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/pm/products")
public class ErpProductController {

    private final ErpProductBo productBo;

    public ErpProductController(ErpProductBo productBo) {
        this.productBo = productBo;
    }

    @GetMapping
    public R<IPage<ErpProduct>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return R.ok(productBo.pageList(pageNum, pageSize, keyword));
    }

    @GetMapping("/all")
    public R<List<ErpProduct>> listAll() {
        return R.ok(productBo.listAll());
    }

    @GetMapping("/{id}")
    public R<ErpProduct> detail(@PathVariable String id) {
        return R.ok(productBo.getById(id));
    }

    @PostMapping
    public R<ErpProduct> create(@RequestBody ErpProduct product) {
        return R.ok(productBo.create(product));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody ErpProduct product) {
        productBo.update(id, product);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        productBo.delete(id);
        return R.ok();
    }
}
