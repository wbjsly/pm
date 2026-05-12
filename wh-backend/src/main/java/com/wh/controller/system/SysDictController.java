package com.wh.controller.system;

import com.wh.bo.system.WhPmDictBo;
import com.wh.common.R;
import com.wh.entity.system.WhDictItem;
import com.wh.entity.system.WhDictType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/system/dict")
public class SysDictController {

    private final WhPmDictBo dictBo;

    public SysDictController(WhPmDictBo dictBo) {
        this.dictBo = dictBo;
    }

    /** 全量字典（所有登录用户均可访问） */
    @GetMapping("/all")
    public R<Map<String, List<WhDictItem>>> getAll() {
        return R.ok(dictBo.getAllDicts());
    }

    /** 按类型获取条目列表 */
    @GetMapping("/items/{typeCode}")
    public R<List<WhDictItem>> getItems(@PathVariable String typeCode) {
        return R.ok(dictBo.getItemsByType(typeCode));
    }

    /** 字典类型列表 */
    @GetMapping("/types")
    public R<List<WhDictType>> getTypes() {
        return R.ok(dictBo.getAllTypes());
    }

    /** 新增字典类型 */
    @PostMapping("/type")
    public R<WhDictType> createType(@RequestBody WhDictType type) {
        return R.ok(dictBo.saveType(type));
    }

    /** 更新字典类型 */
    @PutMapping("/type")
    public R<WhDictType> updateType(@RequestBody WhDictType type) {
        return R.ok(dictBo.updateType(type));
    }

    /** 删除字典类型 */
    @DeleteMapping("/type/{id}")
    public R<Void> deleteType(@PathVariable String id) {
        dictBo.deleteType(id);
        return R.ok();
    }

    /** 新增字典条目 */
    @PostMapping("/item")
    public R<WhDictItem> createItem(@RequestBody WhDictItem item) {
        return R.ok(dictBo.saveItem(item));
    }

    /** 更新字典条目 */
    @PutMapping("/item")
    public R<WhDictItem> updateItem(@RequestBody WhDictItem item) {
        return R.ok(dictBo.updateItem(item));
    }

    /** 删除字典条目 */
    @DeleteMapping("/item/{id}")
    public R<Void> deleteItem(@PathVariable String id) {
        dictBo.deleteItem(id);
        return R.ok();
    }
}
