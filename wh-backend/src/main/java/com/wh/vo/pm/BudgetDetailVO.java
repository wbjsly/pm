package com.wh.vo.pm;

import com.wh.entity.pm.WhPmBudget;
import lombok.Data;

import java.util.List;

@Data
public class BudgetDetailVO {
    private WhPmBudget budget;
    private List<BudgetItemVO> items;
}
