package com.wh;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.ErpModuleBo;
import com.wh.common.ServiceException;
import com.wh.dao.pm.ErpModuleDao;
import com.wh.dao.pm.ErpProductDao;
import com.wh.entity.pm.ErpModule;
import com.wh.entity.pm.ErpProduct;
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
@DisplayName("ERP 模块 BO 测试")
class ErpModuleBoTest {

    @Autowired
    private ErpModuleBo moduleBo;

    @Autowired
    private ErpModuleDao moduleDao;

    @Autowired
    private ErpProductDao productDao;

    private String productId;

    @BeforeEach
    void setUp() {
        // 创建测试产品
        ErpProduct product = new ErpProduct();
        product.setProductCode("PROD-TEST");
        product.setProductName("测试产品");
        product.setStatus("ACTIVE");
        productDao.insert(product);
        productId = product.getId();
    }

    private ErpModule createModule(String name, String code) {
        ErpModule module = new ErpModule();
        module.setProductId(productId);
        module.setModuleName(name);
        module.setModuleCode(code);
        module.setDescription(name + "描述");
        module.setStatus("ACTIVE");
        return moduleBo.create(module);
    }

    // ══════════════════════════════════════════════════
    //  分页查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("分页查询")
    class PageListTests {

        @Test
        @DisplayName("pageList - 无条件分页（含种子数据）")
        void testPageList_NoFilter() {
            long initialCount = moduleBo.pageList(1, 1, null, null).getTotal();
            createModule("模块A", "MOD-A");
            createModule("模块B", "MOD-B");

            IPage<ErpModule> page = moduleBo.pageList(1, 10, null, null);
            assertEquals(initialCount + 2, page.getTotal());
            assertTrue(page.getRecords().size() >= 2);
        }

        @Test
        @DisplayName("pageList - 按产品过滤")
        void testPageList_FilterByProduct() {
            createModule("模块A", "MOD-A");

            // 不同产品下的模块
            ErpProduct otherProduct = new ErpProduct();
            otherProduct.setProductCode("PROD-OTHER");
            otherProduct.setProductName("其他产品");
            productDao.insert(otherProduct);

            ErpModule otherModule = new ErpModule();
            otherModule.setProductId(otherProduct.getId());
            otherModule.setModuleName("其他模块");
            otherModule.setModuleCode("MOD-OTHER");
            moduleBo.create(otherModule);

            IPage<ErpModule> page = moduleBo.pageList(1, 10, productId, null);
            assertEquals(1, page.getTotal());
            assertTrue(page.getRecords().stream().allMatch(m -> productId.equals(m.getProductId())));
        }

        @Test
        @DisplayName("pageList - 按关键词搜索")
        void testPageList_FilterByKeyword() {
            createModule("用户管理", "UM");
            createModule("订单管理", "OM");

            IPage<ErpModule> page = moduleBo.pageList(1, 10, null, "用户");
            assertEquals(1, page.getTotal());
            assertEquals("用户管理", page.getRecords().get(0).getModuleName());
        }

        @Test
        @DisplayName("pageList - 翻页")
        void testPageList_Pagination() {
            long initialCount = moduleBo.pageList(1, 1, null, null).getTotal();
            int newModules = 5;
            for (int i = 0; i < newModules; i++) {
                createModule("模块" + i, "MOD-" + i);
            }

            IPage<ErpModule> page1 = moduleBo.pageList(1, 2, null, null);
            assertEquals(initialCount + newModules, page1.getTotal());
            assertEquals(2, page1.getRecords().size());

            IPage<ErpModule> page3 = moduleBo.pageList(3, 2, null, null);
            assertEquals(initialCount + newModules, page3.getTotal());
            assertFalse(page3.getRecords().isEmpty());
        }

        @Test
        @DisplayName("pageList - 关键词无匹配返回空")
        void testPageList_NoMatch() {
            IPage<ErpModule> page = moduleBo.pageList(1, 10, null, "___NON_EXISTENT_KEYWORD___");
            assertEquals(0, page.getTotal());
            assertTrue(page.getRecords().isEmpty());
        }
    }

    // ══════════════════════════════════════════════════
    //  按产品查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("按产品查询")
    class ListByProductTests {

        @Test
        @DisplayName("listByProductId - 返回产品下的所有模块")
        void testListByProductId() {
            createModule("模块A", "MOD-A");
            createModule("模块B", "MOD-B");

            List<ErpModule> list = moduleBo.listByProductId(productId);
            assertEquals(2, list.size());
        }

        @Test
        @DisplayName("listByProductId - 不包含其他产品的模块")
        void testListByProductId_ExcludesOtherProducts() {
            createModule("本产品的模块", "MOD-THIS");

            ErpProduct otherProduct = new ErpProduct();
            otherProduct.setProductCode("PROD-OTHER");
            otherProduct.setProductName("其他产品");
            productDao.insert(otherProduct);

            ErpModule otherModule = new ErpModule();
            otherModule.setProductId(otherProduct.getId());
            otherModule.setModuleName("其他产品模块");
            otherModule.setModuleCode("MOD-OTHER");
            moduleBo.create(otherModule);

            List<ErpModule> list = moduleBo.listByProductId(productId);
            assertEquals(1, list.size());
            assertEquals("本产品的模块", list.get(0).getModuleName());
        }
    }

    // ══════════════════════════════════════════════════
    //  单条查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("单条查询")
    class GetByIdTests {

        @Test
        @DisplayName("getById - 存在返回")
        void testGetById_Success() {
            ErpModule module = createModule("查询测试", "QUERY");

            ErpModule found = moduleBo.getById(module.getId());
            assertNotNull(found);
            assertEquals("查询测试", found.getModuleName());
        }

        @Test
        @DisplayName("getById - 不存在抛出 404")
        void testGetById_NotFound() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> moduleBo.getById("non-existent-id"));
            assertEquals(404, ex.getCode());
        }
    }

    // ══════════════════════════════════════════════════
    //  CRUD
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("模块 CRUD")
    class CrudTests {

        @Test
        @DisplayName("create - 创建模块")
        void testCreate() {
            ErpModule module = new ErpModule();
            module.setProductId(productId);
            module.setModuleName("新模块");
            module.setModuleCode("NEW-MOD");
            module.setDescription("新模块描述");
            module.setStatus("ACTIVE");

            ErpModule saved = moduleBo.create(module);

            assertNotNull(saved.getId());
            assertEquals("0", saved.getDelFlag());
            assertEquals("新模块", saved.getModuleName());
        }

        @Test
        @DisplayName("create - 默认状态为 ACTIVE")
        void testCreate_DefaultStatus() {
            ErpModule module = new ErpModule();
            module.setProductId(productId);
            module.setModuleName("默认状态");
            module.setModuleCode("DEF-STATUS");

            ErpModule saved = moduleBo.create(module);
            assertEquals("ACTIVE", saved.getStatus());
        }

        @Test
        @DisplayName("update - 更新模块")
        void testUpdate() {
            ErpModule module = createModule("原名", "ORIG");

            ErpModule req = new ErpModule();
            req.setModuleName("新名称");
            req.setModuleCode("NEW-CODE");
            req.setDescription("新描述");

            moduleBo.update(module.getId(), req);

            ErpModule updated = moduleBo.getById(module.getId());
            assertEquals("新名称", updated.getModuleName());
            assertEquals("NEW-CODE", updated.getModuleCode());
        }

        @Test
        @DisplayName("delete - 逻辑删除模块")
        void testDelete() {
            ErpModule module = createModule("待删除", "DEL");

            moduleBo.delete(module.getId());

            // 删除后 getById 应抛出 404
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> moduleBo.getById(module.getId()));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("delete - 不存在的模块抛出 404")
        void testDelete_NotFound() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> moduleBo.delete("non-existent-id"));
            assertEquals(404, ex.getCode());
        }
    }

    @Nested
    @DisplayName("补充 - 创建分支")
    class ExtraCreateTests {

        @Test
        @DisplayName("create - 状态为空时默认为 ACTIVE")
        void createWithoutStatus_defaultsToActive() {
            ErpModule module = new ErpModule();
            module.setProductId("product-extra");
            module.setModuleName("默认状态模块");
            module.setModuleCode("DEF-" + System.nanoTime());
            // 不设置 status

            ErpModule saved = moduleBo.create(module);

            assertEquals("ACTIVE", saved.getStatus());
            assertEquals("0", saved.getDelFlag());
        }
    }

    @Nested
    @DisplayName("补充 - 空字符串与默认状态")
    class ExtraBranchTests {

        @Test
        @DisplayName("pageList - 空字符串过滤参数视为无条件")
        void pageList_emptyStrings() {
            createModule("模块A", "MOD-A");
            IPage<com.wh.entity.pm.ErpModule> page = moduleBo.pageList(1, 10, "", "");
            assertNotNull(page);
            assertTrue(page.getTotal() >= 1);
        }

        @Test
        @DisplayName("create - 状态为空字符串时默认为 ACTIVE")
        void createEmptyStatus_defaultsToActive() {
            ErpModule module = new ErpModule();
            module.setProductId("product-empty-status");
            module.setModuleName("空状态模块");
            module.setModuleCode("EMP-" + System.nanoTime());
            module.setStatus("");

            ErpModule saved = moduleBo.create(module);

            assertEquals("ACTIVE", saved.getStatus());
        }
    }
}
