package com.wh.cost;

import com.wh.dao.pm.WhPmActualCostDao;
import com.wh.entity.pm.WhPmActualCost;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class DefaultCostEventPublisher implements CostEventPublisher {

    private final WhPmActualCostDao actualCostDao;

    public DefaultCostEventPublisher(WhPmActualCostDao actualCostDao) {
        this.actualCostDao = actualCostDao;
    }

    @Override
    public void publish(CostEvent event) {
        WhPmActualCost cost = new WhPmActualCost();
        cost.setProjectId(event.getProjectId());
        cost.setBudgetItemId(event.getBudgetItemId());
        cost.setCostDate(event.getCostDate() != null ? event.getCostDate().toString() : null);
        cost.setCostType(null); // Will be derived from budget item
        cost.setAmount(event.getAmount() != null ? event.getAmount().toPlainString() : "0");
        cost.setDescription(event.getSourceRef());
        cost.setSourceSystem(event.getSourceSystem() != null ? event.getSourceSystem().name().toLowerCase() : "manual");
        cost.setSourceRef(event.getSourceRef());
        cost.setSourceId(event.getSourceId());
        actualCostDao.insert(cost);
        log.info("Cost event published: projectId={}, budgetItemId={}, amount={}, source={}",
                event.getProjectId(), event.getBudgetItemId(), event.getAmount(), event.getSourceSystem());
    }
}
