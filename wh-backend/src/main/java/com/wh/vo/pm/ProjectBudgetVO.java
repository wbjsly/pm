package com.wh.vo.pm;

import com.wh.entity.pm.WhPmBudget;
import lombok.Data;

import java.util.List;

@Data
public class ProjectBudgetVO {

    private String projectId;

    private String projectName;

    private String projectShortName;

    private String pmName;

    /** 项目状态（来自 Charter） */
    private String projectStatus;

    /** 是否有任何未删除的预算记录 */
    private boolean hasAnyBudget;

    /** 最新已审批预算的版本号 */
    private String latestApprovedVersion;

    /** 最新已审批预算的直接预算金额 */
    private String latestApprovedAmount;

    /** 最新已审批预算的项目总预算（直接预算+管理储备） */
    private String latestApprovedTotalBudget;

    /** 最新已审批预算的 ID（用于预实对比） */
    private String latestApprovedBudgetId;

    /** 项目实际成本 */
    private Double projectActualCost;

    /** 项目预算余额 */
    private Double projectBudgetRemaining;

    /** 项目预算投入比例 */
    private Double projectCostRatio;

    private List<WhPmBudget> budgets;
}
