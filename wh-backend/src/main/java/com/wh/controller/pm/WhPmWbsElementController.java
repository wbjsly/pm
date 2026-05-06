package com.wh.controller.pm;

import com.wh.bo.pm.*;
import com.wh.common.R;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.entity.pm.WhPmWbsVersion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/pm/wbs")
public class WhPmWbsElementController {

    private final WhPmWbsElementBo wbsBo;

    public WhPmWbsElementController(WhPmWbsElementBo wbsBo) {
        this.wbsBo = wbsBo;
    }

    @GetMapping
    public R<List<WhPmWbsElement>> list(
            @RequestParam String projectId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String keyword) {
        return R.ok(wbsBo.getTreeByProjectId(projectId, status, keyword));
    }

    @GetMapping("/{id}")
    public R<WhPmWbsElement> detail(@PathVariable String id) {
        return R.ok(wbsBo.getDetail(id));
    }

    @PostMapping
    public R<WhPmWbsElement> create(@RequestBody WbsCreateRequest req) {
        return R.ok(wbsBo.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody WbsUpdateRequest req) {
        wbsBo.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        wbsBo.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/suspend")
    public R<Void> suspend(@PathVariable String id) {
        wbsBo.suspend(id);
        return R.ok();
    }

    @PostMapping("/{id}/resume")
    public R<Void> resume(@PathVariable String id) {
        wbsBo.resume(id);
        return R.ok();
    }

    @PostMapping("/{id}/reopen")
    public R<Void> reopen(@PathVariable String id) {
        wbsBo.reopen(id);
        return R.ok();
    }

    @PostMapping("/{id}/start")
    public R<Void> start(@PathVariable String id) {
        wbsBo.start(id);
        return R.ok();
    }

    @PostMapping("/{id}/test")
    public R<Void> test(@PathVariable String id) {
        wbsBo.test(id);
        return R.ok();
    }

    @PostMapping("/{id}/complete")
    public R<Void> complete(@PathVariable String id) {
        wbsBo.complete(id);
        return R.ok();
    }

    @PostMapping("/{id}/cancel")
    public R<Void> cancel(@PathVariable String id) {
        wbsBo.cancel(id);
        return R.ok();
    }

    @GetMapping("/{id}/versions")
    public R<List<WhPmWbsVersion>> versions(@PathVariable String id) {
        return R.ok(wbsBo.getVersionHistory(id));
    }

    // Import/Export endpoints
    @PostMapping("/import")
    public R<WbsImportResult> importWbs(@RequestParam("file") MultipartFile file,
                                        @RequestParam String projectId) throws IOException {
        return R.ok(wbsBo.importWbs(file, projectId));
    }

    @GetMapping("/export")
    public void exportWbs(@RequestParam String projectId,
                          javax.servlet.http.HttpServletResponse response) throws IOException {
        wbsBo.exportWbs(projectId, response);
    }

    @GetMapping("/template")
    public void downloadTemplate(javax.servlet.http.HttpServletResponse response) throws IOException {
        wbsBo.downloadTemplate(response);
    }
}
