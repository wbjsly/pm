package com.wh.task;

import com.wh.bo.pm.WhPmCostWarningBo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CostWarningScheduledTask {

    private final WhPmCostWarningBo costWarningBo;

    public CostWarningScheduledTask(WhPmCostWarningBo costWarningBo) {
        this.costWarningBo = costWarningBo;
    }

    @Scheduled(cron = "${cost.warning.cron:0 0 6 * * ?}")
    public void executeCostWarningCalculation() {
        log.info("Scheduled cost warning calculation triggered");
        try {
            costWarningBo.triggerCalculation();
            log.info("Scheduled cost warning calculation completed");
        } catch (Exception e) {
            log.error("Scheduled cost warning calculation failed", e);
        }
    }
}
