package com.wh.bo.pm;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wh.common.ServiceException;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.dao.pm.WhPmDeliverableDao;
import com.wh.dao.system.SysUserDao;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmDeliverable;
import com.wh.entity.system.SysUser;
import com.wh.service.SequenceService;
import com.wh.util.SecurityUtils;
import io.minio.GetPresignedObjectUrlArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import io.minio.http.Method;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.TaskService;
import org.flowable.task.api.Task;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class WhPmDeliverableBo {

    private static final int MAX_ATTACHMENTS = 10;

    private final WhPmDeliverableDao deliverableDao;
    private final WhPmCharterDao charterDao;
    private final SysUserDao sysUserDao;
    private final SequenceService sequenceService;
    private final RuntimeService runtimeService;
    private final TaskService taskService;
    private final MinioClient minioClient;
    private final String bucket;

    public WhPmDeliverableBo(WhPmDeliverableDao deliverableDao, WhPmCharterDao charterDao,
                             SysUserDao sysUserDao, SequenceService sequenceService,
                             RuntimeService runtimeService, TaskService taskService,
                             MinioClient minioClient,
                             @Value("${app.minio.bucket}") String bucket) {
        this.deliverableDao = deliverableDao;
        this.charterDao = charterDao;
        this.sysUserDao = sysUserDao;
        this.sequenceService = sequenceService;
        this.runtimeService = runtimeService;
        this.taskService = taskService;
        this.minioClient = minioClient;
        this.bucket = bucket;
    }

    // ─── QUERY METHODS ───

    public IPage<WhPmDeliverable> pageList(int pageNum, int pageSize, String status,
                                            String projectId, String keyword) {
        Page<WhPmDeliverable> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<WhPmDeliverable> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(WhPmDeliverable::getDelFlag, "0");
        if (status != null && !status.isEmpty()) {
            wrapper.eq(WhPmDeliverable::getStatus, status);
        }
        if (projectId != null && !projectId.isEmpty()) {
            wrapper.eq(WhPmDeliverable::getProjectId, projectId);
        }
        if (keyword != null && !keyword.isEmpty()) {
            wrapper.and(w -> w.like(WhPmDeliverable::getName, keyword)
                    .or()
                    .like(WhPmDeliverable::getDeliverableCode, keyword));
        }
        wrapper.orderByDesc(WhPmDeliverable::getCreateDate);
        IPage<WhPmDeliverable> result = deliverableDao.selectPage(page, wrapper);
        fillCreateByName(result.getRecords());
        return result;
    }

    public WhPmDeliverable getById(String id) {
        WhPmDeliverable deliverable = deliverableDao.selectById(id);
        if (deliverable == null || "1".equals(deliverable.getDelFlag())) {
            throw new ServiceException(404, "成果物不存在");
        }
        fillCreateByName(List.of(deliverable));
        fillSponsorName(deliverable);
        return deliverable;
    }

    private void fillCreateByName(List<WhPmDeliverable> deliverables) {
        Map<String, String> userNameMap = new HashMap<>();
        for (WhPmDeliverable d : deliverables) {
            if (d.getCreateBy() != null && !d.getCreateBy().isEmpty()
                    && !userNameMap.containsKey(d.getCreateBy())) {
                SysUser user = sysUserDao.selectById(d.getCreateBy());
                if (user != null) {
                    userNameMap.put(d.getCreateBy(), user.getRealName());
                }
            }
        }
        for (WhPmDeliverable d : deliverables) {
            d.setCreateByName(userNameMap.get(d.getCreateBy()));
        }
    }

    private void fillSponsorName(WhPmDeliverable deliverable) {
        if (deliverable.getProjectId() == null) return;
        WhPmCharter charter = charterDao.selectById(deliverable.getProjectId());
        if (charter != null && charter.getSponsorId() != null) {
            SysUser sponsor = sysUserDao.selectById(charter.getSponsorId());
            if (sponsor != null) {
                deliverable.setSponsorName(sponsor.getRealName());
            }
        }
    }

    // ─── CRUD METHODS ───

    @Transactional
    public WhPmDeliverable create(DeliverableCreateRequest req) {
        WhPmCharter charter = charterDao.selectById(req.getProjectId());
        if (charter == null || "1".equals(charter.getDelFlag())) {
            throw new ServiceException("项目不存在");
        }
        if (!"APPROVED".equals(charter.getStatus())) {
            throw new ServiceException("只有已审批通过的项目可以创建成果物");
        }
        if (!getCurrentUserIdOrThrow().equals(charter.getPmId())) {
            throw new ServiceException("只有项目经理可以创建成果物");
        }

        WhPmDeliverable deliverable = new WhPmDeliverable();
        deliverable.setDeliverableCode(sequenceService.generateCode("PM_DELIVERABLE", "DELIVERABLE-"));
        deliverable.setName(req.getName());
        deliverable.setDescription(req.getDescription());
        deliverable.setPlannedDeliveryDate(req.getPlannedDeliveryDate());
        deliverable.setProjectId(req.getProjectId());
        deliverable.setRemarks(req.getRemarks());
        deliverable.setStatus("DRAFT");
        deliverable.setDelFlag("0");
        deliverableDao.insert(deliverable);
        return deliverable;
    }

    @Transactional
    public void update(String id, DeliverableUpdateRequest req) {
        WhPmDeliverable deliverable = getById(id);
        if (!"DRAFT".equals(deliverable.getStatus()) && !"REJECTED".equals(deliverable.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的成果物可以修改");
        }
        checkIsProjectPm(deliverable);
        deliverable.setName(req.getName());
        deliverable.setDescription(req.getDescription());
        deliverable.setPlannedDeliveryDate(req.getPlannedDeliveryDate());
        deliverable.setRemarks(req.getRemarks());
        int rows = deliverableDao.updateById(deliverable);
        if (rows == 0) {
            throw new ServiceException("数据已被他人修改，请刷新后重试");
        }
    }

    @Transactional
    public void delete(String id) {
        WhPmDeliverable deliverable = getById(id);
        if (!"DRAFT".equals(deliverable.getStatus())) {
            throw new ServiceException("只有草稿状态的成果物可以删除");
        }
        checkIsProjectPm(deliverable);
        deliverableDao.physicalDeleteById(id);
    }

    // ─── WORKFLOW METHODS ───

    @Transactional
    public void submit(String id) {
        WhPmDeliverable deliverable = getById(id);
        if (!"DRAFT".equals(deliverable.getStatus()) && !"REJECTED".equals(deliverable.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的成果物可以提交审批");
        }
        checkIsProjectPm(deliverable);

        WhPmCharter charter = charterDao.selectById(deliverable.getProjectId());

        Map<String, Object> variables = new HashMap<>();
        variables.put("flowCode", "PM_DELIVERABLE_APPROVAL");
        variables.put("bizId", id);
        variables.put("assignee", charter.getSponsorId());

        org.flowable.engine.runtime.ProcessInstance instance =
                runtimeService.startProcessInstanceByKey("PM_DELIVERABLE_APPROVAL", variables);

        deliverable.setStatus("PENDING_APPROVAL");
        deliverable.setProcessInstanceId(instance.getId());
        int rows = deliverableDao.updateById(deliverable);
        if (rows == 0) {
            throw new ServiceException("数据已被他人修改，请刷新后重试");
        }

        Task task = taskService.createTaskQuery().processInstanceId(instance.getId()).singleResult();
        if (task != null) {
            log.info("Deliverable {} submitted for approval, task: {}", id, task.getId());
        }
    }

    @Transactional
    public void approve(String id, String comment) {
        WhPmDeliverable deliverable = getById(id);
        if (!"PENDING_APPROVAL".equals(deliverable.getStatus())) {
            throw new ServiceException("成果物不在审批中");
        }
        checkIsProjectSponsor(deliverable);

        Task task = taskService.createTaskQuery()
                .processInstanceId(deliverable.getProcessInstanceId())
                .singleResult();
        if (task == null) {
            throw new ServiceException("未找到审批任务");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalResult", "APPROVED");
        variables.put("comment", comment);
        taskService.complete(task.getId(), variables);
    }

    @Transactional
    public void reject(String id, String comment) {
        WhPmDeliverable deliverable = getById(id);
        if (!"PENDING_APPROVAL".equals(deliverable.getStatus())) {
            throw new ServiceException("成果物不在审批中");
        }
        checkIsProjectSponsor(deliverable);

        Task task = taskService.createTaskQuery()
                .processInstanceId(deliverable.getProcessInstanceId())
                .singleResult();
        if (task == null) {
            throw new ServiceException("未找到审批任务");
        }

        Map<String, Object> variables = new HashMap<>();
        variables.put("approvalResult", "REJECTED");
        variables.put("rejectReason", comment);
        taskService.complete(task.getId(), variables);
    }

    @Transactional
    public void markDelivered(String id) {
        WhPmDeliverable deliverable = getById(id);
        if (!"APPROVED".equals(deliverable.getStatus())) {
            throw new ServiceException("只有已审批通过的成果物可以标记已交付");
        }
        checkIsProjectPm(deliverable);
        deliverable.setStatus("DELIVERED");
        deliverable.setActualDeliveryDate(java.time.LocalDate.now().toString());
        int rows = deliverableDao.updateById(deliverable);
        if (rows == 0) {
            throw new ServiceException("数据已被他人修改，请刷新后重试");
        }
    }

    // ─── ATTACHMENT METHODS ───

    @Transactional
    public List<Map<String, Object>> uploadAttachment(String id, MultipartFile file) {
        WhPmDeliverable deliverable = getById(id);
        if (!"DRAFT".equals(deliverable.getStatus()) && !"REJECTED".equals(deliverable.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的成果物可以上传附件");
        }
        checkIsProjectPm(deliverable);

        List<Map<String, Object>> attachments = parseAttachments(deliverable.getAttachments());
        if (attachments.size() >= MAX_ATTACHMENTS) {
            throw new ServiceException("附件数量已达上限（" + MAX_ATTACHMENTS + "个）");
        }

        String objectKey = "deliverable/" + id + "/" + UUID.randomUUID() + "_" + file.getOriginalFilename();
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .stream(file.getInputStream(), file.getSize(), -1)
                            .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to upload file to MinIO", e);
            throw new ServiceException("文件上传失败: " + e.getMessage());
        }

        Map<String, Object> entry = new HashMap<>();
        entry.put("fileName", file.getOriginalFilename());
        entry.put("fileSize", file.getSize());
        entry.put("minioKey", objectKey);
        entry.put("uploadTime", java.time.LocalDateTime.now().toString());
        attachments.add(entry);

        deliverable.setAttachments(toJson(attachments));
        int rows = deliverableDao.updateById(deliverable);
        if (rows == 0) {
            throw new ServiceException("数据已被他人修改，请刷新后重试");
        }
        return attachments;
    }

    @Transactional
    public List<Map<String, Object>> deleteAttachment(String id, int index) {
        WhPmDeliverable deliverable = getById(id);
        if (!"DRAFT".equals(deliverable.getStatus()) && !"REJECTED".equals(deliverable.getStatus())) {
            throw new ServiceException("只有草稿或被驳回的成果物可以删除附件");
        }
        checkIsProjectPm(deliverable);

        List<Map<String, Object>> attachments = parseAttachments(deliverable.getAttachments());
        if (index < 0 || index >= attachments.size()) {
            throw new ServiceException("附件索引无效");
        }

        Map<String, Object> removed = attachments.remove(index);
        String objectKey = (String) removed.get("minioKey");
        if (objectKey != null) {
            try {
                minioClient.removeObject(
                        RemoveObjectArgs.builder().bucket(bucket).object(objectKey).build()
                );
            } catch (Exception e) {
                log.warn("Failed to delete file from MinIO: {}", objectKey, e);
            }
        }

        deliverable.setAttachments(toJson(attachments));
        int rows = deliverableDao.updateById(deliverable);
        if (rows == 0) {
            throw new ServiceException("数据已被他人修改，请刷新后重试");
        }
        return attachments;
    }

    public String getAttachmentDownloadUrl(String id, int index) {
        WhPmDeliverable deliverable = getById(id);
        List<Map<String, Object>> attachments = parseAttachments(deliverable.getAttachments());
        if (index < 0 || index >= attachments.size()) {
            throw new ServiceException("附件索引无效");
        }
        String objectKey = (String) attachments.get(index).get("minioKey");
        try {
            return minioClient.getPresignedObjectUrl(
                    GetPresignedObjectUrlArgs.builder()
                            .bucket(bucket)
                            .object(objectKey)
                            .method(Method.GET)
                            .expiry(1, TimeUnit.HOURS)
                            .build()
            );
        } catch (Exception e) {
            log.error("Failed to generate presigned URL for: {}", objectKey, e);
            throw new ServiceException("生成下载链接失败");
        }
    }

    // ─── PERMISSION HELPERS ───

    private String getCurrentUserIdOrThrow() {
        String userId = SecurityUtils.getCurrentUserId();
        if (userId == null) {
            throw new ServiceException(401, "未登录或登录已过期");
        }
        return userId;
    }

    private void checkIsProjectPm(WhPmDeliverable deliverable) {
        WhPmCharter charter = charterDao.selectById(deliverable.getProjectId());
        if (charter == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (!getCurrentUserIdOrThrow().equals(charter.getPmId())) {
            throw new ServiceException("只有项目经理可以执行此操作");
        }
    }

    private void checkIsProjectSponsor(WhPmDeliverable deliverable) {
        WhPmCharter charter = charterDao.selectById(deliverable.getProjectId());
        if (charter == null) {
            throw new ServiceException("关联项目不存在");
        }
        if (!getCurrentUserIdOrThrow().equals(charter.getSponsorId())) {
            throw new ServiceException("只有项目发起人可以执行审批操作");
        }
    }

    // ─── JSON HELPERS ───

    @SuppressWarnings({"unchecked", "rawtypes"})
    private List<Map<String, Object>> parseAttachments(String attachmentsJson) {
        if (attachmentsJson == null || attachmentsJson.isEmpty()) {
            return new ArrayList<>();
        }
        try {
            return (List) cn.hutool.json.JSONUtil.toList(attachmentsJson, Map.class);
        } catch (Exception e) {
            log.warn("Failed to parse attachments JSON, returning empty list", e);
            return new ArrayList<>();
        }
    }

    private String toJson(List<Map<String, Object>> attachments) {
        return cn.hutool.json.JSONUtil.toJsonStr(attachments);
    }
}
