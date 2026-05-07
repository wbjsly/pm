package com.wh.approval.pm;

import com.wh.approval.ApprovalCompletedCallback;
import com.wh.dao.pm.WhPmDeliverableDao;
import com.wh.entity.pm.WhPmDeliverable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class DeliverableApprovalCallback implements ApprovalCompletedCallback {

    private final WhPmDeliverableDao deliverableDao;

    public DeliverableApprovalCallback(WhPmDeliverableDao deliverableDao) {
        this.deliverableDao = deliverableDao;
    }

    @Override
    public String getFlowCode() {
        return "PM_DELIVERABLE_APPROVAL";
    }

    @Override
    public void onApproved(String bizId, Map<String, Object> params) {
        WhPmDeliverable deliverable = deliverableDao.selectById(bizId);
        if (deliverable != null) {
            deliverable.setStatus("APPROVED");
            deliverable.setApprovalComment((String) params.get("comment"));
            deliverableDao.updateById(deliverable);
            log.info("Deliverable {} approved", bizId);
        }
    }

    @Override
    public void onRejected(String bizId, String rejectReason) {
        WhPmDeliverable deliverable = deliverableDao.selectById(bizId);
        if (deliverable != null) {
            deliverable.setStatus("REJECTED");
            deliverable.setApprovalComment(rejectReason);
            deliverable.setProcessInstanceId(null);
            deliverableDao.updateById(deliverable);
            log.info("Deliverable {} rejected: {}", bizId, rejectReason);
        }
    }
}
