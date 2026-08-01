package com.wh;

import com.wh.bo.pm.BudgetCreateRequest;
import com.wh.bo.pm.BudgetItemRequest;
import com.wh.bo.pm.WhPmBudgetBo;
import com.wh.bo.pm.WhPmBudgetItemBo;
import com.wh.dao.system.SysPositionDao;
import com.wh.entity.pm.WhPmBudget;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.system.SysPosition;
import com.wh.fixtures.TestFixtures;
import com.wh.vo.pm.BudgetItemVO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("预算科目 BO 测试")
class WhPmBudgetItemBoTest {

    @Autowired private WhPmBudgetBo budgetBo;
    @Autowired private WhPmBudgetItemBo budgetItemBo;
    @Autowired private TestFixtures fixtures;
    @Autowired private SysPositionDao positionDao;

    private String projectId;

    @BeforeEach
    void setUp() {
        WhPmCharter charter = fixtures.createTestProject("预算科目测试");
        projectId = charter.getId();
    }

    private BudgetItemRequest item(String category, String amount, String roleCode,
                                   String bomItem, String description) {
        BudgetItemRequest req = new BudgetItemRequest();
        req.setCategory(category);
        req.setAmount(amount);
        req.setLevel(1);
        req.setRoleCode(roleCode);
        req.setBomItem(bomItem);
        req.setDescription(description);
        return req;
    }

    private WhPmBudget createBudgetWithItems(List<BudgetItemRequest> items) {
        BudgetCreateRequest req = new BudgetCreateRequest();
        req.setProjectId(projectId);
        req.setItems(items);
        return budgetBo.create(req);
    }

    @Nested
    @DisplayName("创建科目")
    class CreateTests {

        @Test
        @DisplayName("创建各类别科目（LABOR/PROCUREMENT/OTHER 全分支）")
        void createAllCategories() {
            WhPmBudget budget = createBudgetWithItems(List.of(
                    item("LABOR", "1000", "DEV", null, null),
                    item("PROCUREMENT", "2000", null, "Server", null),
                    item("TRAVEL", "300", null, null, "差旅"),
                    item("BUSINESS", "400", null, null, "商务"),
                    item("ENTERTAINMENT", "500", null, null, "招待"),
                    item("ACTIVITY", "600", null, null, "活动"),
                    item("OTHER", "700", null, null, "其他")));

            assertEquals(7, budgetItemBo.buildFlatItemVOs(budget.getId()).size());
        }

        @Test
        @DisplayName("创建父子层级科目")
        void createHierarchy() {
            BudgetItemRequest parent = item("LABOR", "1000", "DEV", null, null);
            BudgetItemRequest child = item("LABOR", "600", "DEV", null, null);
            BudgetItemRequest grandChild = item("TRAVEL", "200", null, null, "子差旅");
            child.setChildren(List.of(grandChild));
            parent.setChildren(List.of(child));

            WhPmBudget budget = createBudgetWithItems(List.of(parent));

            List<BudgetItemVO> tree = budgetItemBo.buildTreeItemVOs(budget.getId());
            assertEquals(1, tree.size());
            assertEquals(1, tree.get(0).getChildren().size());
            assertEquals("TRAVEL", tree.get(0).getChildren().get(0).getChildren().get(0).getCategory());
        }

        @Test
        @DisplayName("LABOR 科目带岗位时解析岗位名称")
        void laborWithPosition_resolvesName() {
            SysPosition pos = new SysPosition();
            pos.setName("高级开发");
            pos.setIsDefault("0");
            pos.setDelFlag("0");
            pos.setVerNo(0);
            positionDao.insert(pos);

            BudgetItemRequest labor = item("LABOR", "1000", "DEV", null, null);
            labor.setPositionId(pos.getId());
            labor.setHours("100");
            labor.setCostRate("80");

            WhPmBudget budget = createBudgetWithItems(List.of(labor));
            List<BudgetItemVO> vos = budgetItemBo.buildFlatItemVOs(budget.getId());
            assertEquals("高级开发", vos.get(0).getPositionName());
            assertEquals("100", vos.get(0).getHours());
        }
    }

    @Nested
    @DisplayName("查询与删除")
    class QueryAndDeleteTests {

        @Test
        @DisplayName("buildTreeItemVOs 对无科目预算返回空树")
        void buildTree_emptyBudget() {
            WhPmBudget budget = createBudgetWithItems(List.of());
            assertTrue(budgetItemBo.buildTreeItemVOs(budget.getId()).isEmpty());
        }

        @Test
        @DisplayName("deleteAllItems 删除科目及明细")
        void deleteAllItems_removesItems() {
            WhPmBudget budget = createBudgetWithItems(List.of(
                    item("LABOR", "1000", "DEV", null, null),
                    item("PROCUREMENT", "2000", null, "Server", null),
                    item("TRAVEL", "300", null, null, "差旅")));

            budgetItemBo.deleteAllItems(budget.getId());

            assertTrue(budgetItemBo.buildFlatItemVOs(budget.getId()).isEmpty());
        }

        @Test
        @DisplayName("实际成本汇总填充到 VO")
        void actualAmountFilled() {
            // 无实际成本时 actualAmount 为 0
            WhPmBudget budget = createBudgetWithItems(List.of(item("LABOR", "1000", "DEV", null, null)));
            List<BudgetItemVO> vos = budgetItemBo.buildFlatItemVOs(budget.getId());
            assertEquals(0, vos.get(0).getActualAmount().doubleValue());
        }
    }

    @Nested
    @DisplayName("补充边界分支")
    class ExtraBranchTests {

        @Test
        @DisplayName("LABOR 无 roleCode 时不创建明细但科目创建成功")
        void laborWithoutRoleCode() {
            BudgetItemRequest labor = new BudgetItemRequest();
            labor.setCategory("LABOR");
            labor.setAmount("1000");
            labor.setLevel(1);
            // roleCode 为空
            WhPmBudget budget = createBudgetWithItems(List.of(labor));
            assertEquals(1, budgetItemBo.buildFlatItemVOs(budget.getId()).size());
        }

        @Test
        @DisplayName("PROCUREMENT 无 bomItem 时不创建明细")
        void procurementWithoutBomItem() {
            BudgetItemRequest proc = new BudgetItemRequest();
            proc.setCategory("PROCUREMENT");
            proc.setAmount("2000");
            proc.setLevel(1);
            WhPmBudget budget = createBudgetWithItems(List.of(proc));
            assertEquals(1, budgetItemBo.buildFlatItemVOs(budget.getId()).size());
        }

        @Test
        @DisplayName("未知类别仅创建科目不创建明细")
        void unknownCategory_onlyCreatesItem() {
            BudgetItemRequest unknown = new BudgetItemRequest();
            unknown.setCategory("UNKNOWN_CAT");
            unknown.setAmount("500");
            unknown.setLevel(1);
            WhPmBudget budget = createBudgetWithItems(List.of(unknown));
            assertEquals(1, budgetItemBo.buildFlatItemVOs(budget.getId()).size());
        }

        @Test
        @DisplayName("amount/level 为空时使用默认值")
        void nullAmountAndLevel_defaults() {
            BudgetItemRequest req = new BudgetItemRequest();
            req.setCategory("TRAVEL");
            // amount 与 level 均为空
            req.setDescription("默认值测试");
            WhPmBudget budget = createBudgetWithItems(List.of(req));
            List<BudgetItemVO> vos = budgetItemBo.buildFlatItemVOs(budget.getId());
            assertEquals(1, vos.size());
            assertEquals(1, vos.get(0).getLevel());
            assertEquals(0, vos.get(0).getBudgetAmount().doubleValue());
        }

        @Test
        @DisplayName("预算金额为 0 时比率也为 0")
        void zeroBudgetAmount_ratioZero() {
            BudgetItemRequest req = new BudgetItemRequest();
            req.setCategory("LABOR");
            req.setAmount("0");
            req.setLevel(1);
            WhPmBudget budget = createBudgetWithItems(List.of(req));
            List<BudgetItemVO> vos = budgetItemBo.buildFlatItemVOs(budget.getId());
            assertEquals(0, vos.get(0).getRatio().doubleValue());
        }
    }
}
