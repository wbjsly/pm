package com.wh;

import com.wh.bo.system.WhPmDictBo;
import com.wh.common.ServiceException;
import com.wh.dao.system.WhDictItemDao;
import com.wh.dao.system.WhDictTypeDao;
import com.wh.entity.system.WhDictItem;
import com.wh.entity.system.WhDictType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("字典 BO 测试")
class WhPmDictBoTest {

    @Autowired
    private WhPmDictBo dictBo;

    @Autowired
    private WhDictTypeDao dictTypeDao;

    @Autowired
    private WhDictItemDao dictItemDao;

    // ══════════════════════════════════════════════════
    //  字典类型 CRUD
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("字典类型 CRUD")
    class DictTypeCrudTests {

        @Test
        @DisplayName("saveType - 创建字典类型")
        void testSaveType() {
            WhDictType type = new WhDictType();
            type.setTypeCode("TEST_TYPE");
            type.setTypeName("测试字典类型");
            type.setDescription("用于集成测试");
            type.setSortOrder(1);
            type.setStatus("0");

            WhDictType saved = dictBo.saveType(type);

            assertNotNull(saved.getId());
            assertEquals("TEST_TYPE", saved.getTypeCode());
        }

        @Test
        @DisplayName("getAllTypes - 返回所有类型")
        void testGetAllTypes() {
            // 初始可能有种子数据
            List<WhDictType> before = dictBo.getAllTypes();
            int initialCount = before.size();

            WhDictType type = new WhDictType();
            type.setTypeCode("TEST_TYPE_B");
            type.setTypeName("测试B");
            dictBo.saveType(type);

            List<WhDictType> after = dictBo.getAllTypes();
            assertEquals(initialCount + 1, after.size());
        }

        @Test
        @DisplayName("updateType - 更新字典类型")
        void testUpdateType() {
            WhDictType type = new WhDictType();
            type.setTypeCode("TYPE_UPD");
            type.setTypeName("原名");
            WhDictType saved = dictBo.saveType(type);

            saved.setTypeName("更新后名称");
            dictBo.updateType(saved);

            WhDictType updated = dictTypeDao.selectById(saved.getId());
            assertEquals("更新后名称", updated.getTypeName());
        }

        @Test
        @DisplayName("deleteType - 删除字典类型")
        void testDeleteType() {
            WhDictType type = new WhDictType();
            type.setTypeCode("TYPE_DEL");
            type.setTypeName("待删除");
            WhDictType saved = dictBo.saveType(type);

            // 删除后不应再出现在全量查询中
            List<WhDictType> before = dictBo.getAllTypes();
            dictBo.deleteType(saved.getId());
            List<WhDictType> after = dictBo.getAllTypes();

            assertEquals(before.size() - 1, after.size());
            assertTrue(after.stream().noneMatch(t -> saved.getId().equals(t.getId())));
        }
    }

    // ══════════════════════════════════════════════════
    //  字典条目 CRUD
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("字典条目 CRUD")
    class DictItemCrudTests {

        @Test
        @DisplayName("saveItem - 创建字典条目")
        void testSaveItem() {
            WhDictItem item = new WhDictItem();
            item.setTypeCode("TEST_TYPE");
            item.setItemCode("ITEM_001");
            item.setItemLabel("测试条目");
            item.setItemValue("1");
            item.setSortOrder(1);
            item.setStatus("0");

            WhDictItem saved = dictBo.saveItem(item);

            assertNotNull(saved.getId());
            assertEquals("ITEM_001", saved.getItemCode());
            assertEquals("测试条目", saved.getItemLabel());
        }

        @Test
        @DisplayName("updateItem - 更新字典条目")
        void testUpdateItem() {
            WhDictItem item = new WhDictItem();
            item.setTypeCode("TEST_TYPE");
            item.setItemCode("ITEM_UPD");
            item.setItemLabel("原名");
            WhDictItem saved = dictBo.saveItem(item);

            saved.setItemLabel("更新后名称");
            dictBo.updateItem(saved);

            WhDictItem updated = dictItemDao.selectById(saved.getId());
            assertEquals("更新后名称", updated.getItemLabel());
        }

        @Test
        @DisplayName("deleteItem - 删除字典条目")
        void testDeleteItem() {
            WhDictItem item = new WhDictItem();
            item.setTypeCode("TEST_TYPE");
            item.setItemCode("ITEM_DEL");
            item.setItemLabel("待删除");
            WhDictItem saved = dictBo.saveItem(item);

            // 删除后不应再被 getItemsByType 返回
            List<WhDictItem> before = dictBo.getItemsByType("TEST_TYPE");
            dictBo.deleteItem(saved.getId());
            List<WhDictItem> after = dictBo.getItemsByType("TEST_TYPE");

            assertEquals(before.size() - 1, after.size());
            assertTrue(after.stream().noneMatch(i -> saved.getId().equals(i.getId())));
        }
    }

    // ══════════════════════════════════════════════════
    //  字典查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("字典查询")
    class DictQueryTests {

        @Test
        @DisplayName("getAllDicts - 返回按 typeCode 分组的数据")
        void testGetAllDicts() {
            // 插入测试数据
            WhDictItem item1 = new WhDictItem();
            item1.setTypeCode("TYPE_A");
            item1.setItemCode("A_001");
            item1.setItemLabel("条目A1");
            dictBo.saveItem(item1);

            WhDictItem item2 = new WhDictItem();
            item2.setTypeCode("TYPE_A");
            item2.setItemCode("A_002");
            item2.setItemLabel("条目A2");
            dictBo.saveItem(item2);

            WhDictItem item3 = new WhDictItem();
            item3.setTypeCode("TYPE_B");
            item3.setItemCode("B_001");
            item3.setItemLabel("条目B1");
            dictBo.saveItem(item3);

            Map<String, List<WhDictItem>> allDicts = dictBo.getAllDicts();

            assertTrue(allDicts.containsKey("TYPE_A"));
            assertTrue(allDicts.containsKey("TYPE_B"));
            assertEquals(2, allDicts.get("TYPE_A").size());
            assertEquals(1, allDicts.get("TYPE_B").size());
        }

        @Test
        @DisplayName("getItemsByType - 按类型获取条目")
        void testGetItemsByType() {
            WhDictItem item = new WhDictItem();
            item.setTypeCode("TYPE_QUERY");
            item.setItemCode("Q_001");
            item.setItemLabel("查询测试");
            dictBo.saveItem(item);

            List<WhDictItem> items = dictBo.getItemsByType("TYPE_QUERY");
            assertEquals(1, items.size());
            assertEquals("查询测试", items.get(0).getItemLabel());
        }

        @Test
        @DisplayName("getItemsByType - 不存在的类型返回空")
        void testGetItemsByType_NotFound() {
            List<WhDictItem> items = dictBo.getItemsByType("NON_EXISTENT");
            assertTrue(items.isEmpty());
        }

        @Test
        @DisplayName("getLabel - 返回条目中文名")
        void testGetLabel() {
            WhDictItem item = new WhDictItem();
            item.setTypeCode("TYPE_LABEL");
            item.setItemCode("CODE_X");
            item.setItemLabel("中文名X");
            dictBo.saveItem(item);

            String label = dictBo.getLabel("TYPE_LABEL", "CODE_X");
            assertEquals("中文名X", label);
        }

        @Test
        @DisplayName("getLabel - 不存在的条目返回 itemCode 本身")
        void testGetLabel_NotFound() {
            String label = dictBo.getLabel("TYPE_LABEL", "NON_EXISTENT");
            assertEquals("NON_EXISTENT", label);
        }
    }
}
