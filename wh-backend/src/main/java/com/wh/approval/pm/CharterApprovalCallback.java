package com.wh.approval.pm;

import com.wh.approval.ApprovalCompletedCallback;
import com.wh.dao.pm.WhPmCharterDao;
import com.wh.entity.pm.WhPmCharter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Map;

@Slf4j
@Component
public class CharterApprovalCallback implements ApprovalCompletedCallback {

    private final WhPmCharterDao charterDao;

    public CharterApprovalCallback(WhPmCharterDao charterDao) {
        this.charterDao = charterDao;
    }

    @Override
    public String getFlowCode() {
        return "PM_CHARTER_APPROVAL";
    }

    @Override
    public void onApproved(String bizId, Map<String, Object> params) {
        WhPmCharter charter = charterDao.selectById(bizId);
        if (charter != null) {
            charter.setStatus("APPROVED");
            charter.setApprovalComment((String) params.get("comment"));
            charterDao.updateById(charter);
            log.info("Charter {} approved", bizId);
        }
    }

    @Override
    public void onRejected(String bizId, String rejectReason) {
        WhPmCharter charter = charterDao.selectById(bizId);
        if (charter != null) {
            charter.setStatus("REJECTED");
            charter.setApprovalComment(rejectReason);
            charter.setProcessInstanceId(null);
            charterDao.updateById(charter);
            log.info("Charter {} rejected: {}", bizId, rejectReason);
        }
    }
}
