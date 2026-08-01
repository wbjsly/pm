package com.wh;

import com.baomidou.mybatisplus.core.metadata.IPage;
import com.wh.bo.pm.ErpProductBo;
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
@DisplayName("ERP 产品 BO 测试")
class ErpProductBoTest {

    @Autowired private ErpProductBo productBo;
    @Autowired private ErpProductDao productDao;
    @Autowired private ErpModuleDao moduleDao;

    private String productId;

    @BeforeEach
    void setUp() {
        ErpProduct product = new ErpProduct();
        product.setProductCode("PROD-BO-TEST");
        product.setProductName("BO测试产品");
        product.setStatus("ACTIVE");
        productDao.insert(product);
        productId = product.getId();
    }

    // ══════════════════════════════════════════════════
    //  分页查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("分页查询")
    class PageList {

        @Test
        @DisplayName("无条件分页")
        void noFilter() {
            IPage<ErpProduct> page = productBo.pageList(1, 10, null);
            assertTrue(page.getTotal() >= 1);
        }

        @Test
        @DisplayName("按关键词搜索")
        void filterByKeyword() {
            IPage<ErpProduct> page = productBo.pageList(1, 10, "BO测试");
            assertTrue(page.getTotal() >= 1);
        }

        @Test
        @DisplayName("关键词无匹配返回空")
        void noMatch() {
            IPage<ErpProduct> page = productBo.pageList(1, 10, "___NONEXISTENT___");
            assertEquals(0, page.getTotal());
        }
    }

    // ══════════════════════════════════════════════════
    //  列表查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("列表查询")
    class ListAll {

        @Test
        @DisplayName("listAll - 返回所有产品列表")
        void returnsAll() {
            List<ErpProduct> list = productBo.listAll();
            assertTrue(list.size() >= 1);
        }

        @Test
        @DisplayName("listAll - 包含模块数量")
        void includesModuleCount() {
            ErpModule module = new ErpModule();
            module.setProductId(productId);
            module.setModuleName("测试模块");
            module.setModuleCode("MOD-COUNT");
            module.setStatus("ACTIVE");
            moduleDao.insert(module);

            List<ErpProduct> list = productBo.listAll();
            ErpProduct found = list.stream().filter(p -> productId.equals(p.getId())).findFirst().orElse(null);
            assertNotNull(found);
            assertEquals(Integer.valueOf(1), found.getModuleCount());
        }
    }

    // ══════════════════════════════════════════════════
    //  单条查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("单条查询")
    class GetById {

        @Test
        @DisplayName("存在返回详情")
        void existingId_returnsDetail() {
            ErpProduct product = productBo.getById(productId);
            assertNotNull(product);
            assertEquals("BO测试产品", product.getProductName());
        }

        @Test
        @DisplayName("不存在抛出404")
        void nonexistentId_throws404() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productBo.getById("nonexistent-id"));
            assertEquals(404, ex.getCode());
        }
    }

    // ══════════════════════════════════════════════════
    //  按编码查询
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("按编码查询")
    class GetByProductCode {

        @Test
        @DisplayName("存在返回产品")
        void existingCode_returnsProduct() {
            ErpProduct product = productBo.getByProductCode("PROD-BO-TEST");
            assertNotNull(product);
        }

        @Test
        @DisplayName("不存在返回null")
        void nonexistentCode_returnsNull() {
            ErpProduct product = productBo.getByProductCode("NONEXISTENT");
            assertNull(product);
        }
    }

    // ══════════════════════════════════════════════════
    //  创建
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("创建")
    class Create {

        @Test
        @DisplayName("正常创建")
        void validRequest_creates() {
            ErpProduct product = new ErpProduct();
            product.setProductCode("NEW-PROD-" + System.currentTimeMillis());
            product.setProductName("新产品");
            product.setStatus("ACTIVE");

            ErpProduct saved = productBo.create(product);
            assertNotNull(saved.getId());
            assertEquals("新产品", saved.getProductName());
        }

        @Test
        @DisplayName("状态为空时默认为 ACTIVE")
        void nullStatus_defaultsToActive() {
            ErpProduct product = new ErpProduct();
            product.setProductCode("DEF-STATUS-" + System.currentTimeMillis());
            product.setProductName("默认状态");

            ErpProduct saved = productBo.create(product);
            assertEquals("ACTIVE", saved.getStatus());
        }

        @Test
        @DisplayName("重复编码返回409")
        void duplicateCode_throws409() {
            ErpProduct product = new ErpProduct();
            product.setProductCode("PROD-BO-TEST"); // Same as setUp
            product.setProductName("重复编码");

            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productBo.create(product));
            assertEquals(409, ex.getCode());
        }
    }

    // ══════════════════════════════════════════════════
    //  更新
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("更新")
    class Update {

        @Test
        @DisplayName("更新成功")
        void success() {
            ErpProduct req = new ErpProduct();
            req.setProductName("更新后的产品名");
            req.setStatus("INACTIVE");
            req.setDescription("更新描述");

            productBo.update(productId, req);

            ErpProduct updated = productBo.getById(productId);
            assertEquals("更新后的产品名", updated.getProductName());
            assertEquals("INACTIVE", updated.getStatus());
            assertEquals("更新描述", updated.getDescription());
        }

        @Test
        @DisplayName("不存在的ID抛出404")
        void nonexistentId_throws404() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productBo.update("nonexistent-id", new ErpProduct()));
            assertEquals(404, ex.getCode());
        }
    }

    // ══════════════════════════════════════════════════
    //  删除
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("删除")
    class Delete {

        @Test
        @DisplayName("删除成功")
        void success() {
            productBo.delete(productId);
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productBo.getById(productId));
            assertEquals(404, ex.getCode());
        }

        @Test
        @DisplayName("不存在的ID抛出404")
        void nonexistentId_throws404() {
            ServiceException ex = assertThrows(ServiceException.class,
                    () -> productBo.delete("nonexistent-id"));
            assertEquals(404, ex.getCode());
        }
    }

    // ══════════════════════════════════════════════════
    //  占位创建
    // ══════════════════════════════════════════════════

    @Nested
    @DisplayName("占位创建")
    class CreatePlaceholder {

        @Test
        @DisplayName("createPlaceholder - 创建占位产品")
        void createsPlaceholder() {
            ErpProduct product = productBo.createPlaceholder("PLACEHOLDER-001");
            assertNotNull(product.getId());
            assertEquals("PLACEHOLDER", product.getStatus());
            assertEquals("PLACEHOLDER-001", product.getProductCode());
        }
    }

    @Nested
    @DisplayName("补充 - 空字符串边界")
    class ExtraBranchTests {

        @Test
        @DisplayName("pageList - 空字符串关键词视为无条件")
        void pageList_emptyKeyword() {
            var page = productBo.pageList(1, 10, "");
            assertNotNull(page);
        }

        @Test
        @DisplayName("create - 状态为空字符串时默认为 ACTIVE")
        void createEmptyStatus_defaultsToActive() {
            ErpProduct product = new ErpProduct();
            product.setProductCode("PROD-EMPTY-" + System.nanoTime());
            product.setProductName("空状态产品");
            product.setStatus("");

            ErpProduct saved = productBo.create(product);

            assertEquals("ACTIVE", saved.getStatus());
        }
    }
}
