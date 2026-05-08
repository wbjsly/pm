package com.wh.controller.pm;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.DeliverableCreateRequest;
import com.wh.bo.pm.DeliverableSubmitRequest;
import com.wh.bo.pm.DeliverableUpdateRequest;
import com.wh.bo.pm.WhPmDeliverableBo;
import cn.hutool.json.JSONUtil;
import com.wh.common.R;
import com.wh.entity.pm.WhPmDeliverable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/pm/deliverables")
public class WhPmDeliverableController {

    private final WhPmDeliverableBo deliverableBo;

    public WhPmDeliverableController(WhPmDeliverableBo deliverableBo) {
        this.deliverableBo = deliverableBo;
    }

    @GetMapping
    public R<IPage<WhPmDeliverable>> list(
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String projectId,
            @RequestParam(required = false) String keyword) {
        return R.ok(deliverableBo.pageList(pageNum, pageSize, status, projectId, keyword));
    }

    @GetMapping("/{id}")
    public R<WhPmDeliverable> detail(@PathVariable String id) {
        WhPmDeliverable deliverable = deliverableBo.getById(id);
        List<Map<String, Object>> activeAttachments = deliverableBo.filterActiveAttachments(deliverable.getAttachments());
        deliverable.setAttachments(JSONUtil.toJsonStr(activeAttachments));
        return R.ok(deliverable);
    }

    @PostMapping
    public R<WhPmDeliverable> create(@RequestBody DeliverableCreateRequest req) {
        return R.ok(deliverableBo.create(req));
    }

    @PutMapping("/{id}")
    public R<Void> update(@PathVariable String id, @RequestBody DeliverableUpdateRequest req) {
        deliverableBo.update(id, req);
        return R.ok();
    }

    @DeleteMapping("/{id}")
    public R<Void> delete(@PathVariable String id) {
        deliverableBo.delete(id);
        return R.ok();
    }

    @PostMapping("/{id}/submit")
    public R<Void> submit(@PathVariable String id) {
        deliverableBo.submit(id);
        return R.ok();
    }

    @PostMapping("/{id}/approve")
    public R<Void> approve(@PathVariable String id, @RequestBody DeliverableSubmitRequest req) {
        deliverableBo.approve(id, req.getComment());
        return R.ok();
    }

    @PostMapping("/{id}/reject")
    public R<Void> reject(@PathVariable String id, @RequestBody DeliverableSubmitRequest req) {
        deliverableBo.reject(id, req.getRejectReason());
        return R.ok();
    }

    @PostMapping("/{id}/deliver")
    public R<Void> markDelivered(@PathVariable String id) {
        deliverableBo.markDelivered(id);
        return R.ok();
    }

    @PostMapping("/{id}/attachments")
    public R<List<Map<String, Object>>> uploadAttachment(@PathVariable String id,
                                                          @RequestParam("file") MultipartFile file) {
        return R.ok(deliverableBo.uploadAttachment(id, file));
    }

    @DeleteMapping("/{id}/attachments/{index}")
    public R<List<Map<String, Object>>> deleteAttachment(@PathVariable String id,
                                                          @PathVariable int index) {
        return R.ok(deliverableBo.deleteAttachment(id, index));
    }

    @GetMapping("/{id}/attachments/{index}")
    public void downloadAttachment(@PathVariable String id,
                                    @PathVariable int index,
                                    HttpServletResponse response) throws IOException {
        byte[] data = deliverableBo.getAttachmentBytes(id, index);
        String filename = deliverableBo.getAttachmentFilename(id, index);
        String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                .replaceAll("\\+", "%20");
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(data.length);
        response.getOutputStream().write(data);
    }

    @GetMapping("/{id}/attachments/zip")
    public void downloadAttachmentsZip(@PathVariable String id,
                                       HttpServletResponse response) throws IOException {
        byte[] zipBytes = deliverableBo.getAttachmentDownloadZip(id);
        String filename = deliverableBo.getZipDownloadFilename(id);
        String encodedFilename = java.net.URLEncoder.encode(filename, "UTF-8")
                .replaceAll("\\+", "%20");
        response.setContentType("application/zip");
        response.setHeader("Content-Disposition",
                "attachment; filename*=UTF-8''" + encodedFilename);
        response.setContentLength(zipBytes.length);
        response.getOutputStream().write(zipBytes);
    }
}
