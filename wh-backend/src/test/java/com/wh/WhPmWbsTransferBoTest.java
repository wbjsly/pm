package com.wh;

import com.wh.bo.pm.WhPmWbsElementBo;
import com.wh.bo.pm.WhPmWbsTransferBo;
import com.wh.bo.pm.WbsCreateRequest;
import com.wh.bo.pm.WbsImportResult;
import com.wh.entity.pm.WhPmCharter;
import com.wh.entity.pm.WhPmWbsElement;
import com.wh.fixtures.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles({"sqlite", "dev"})
@Transactional
@DisplayName("WBS 导入/导出 BO 测试")
class WhPmWbsTransferBoTest {

    @Autowired
    private WhPmWbsTransferBo transferBo;

    @Autowired
    private WhPmWbsElementBo wbsElementBo;

    @Autowired
    private TestFixtures fixtures;

    private String projectId;

    @BeforeEach
    void setUp() {
        WhPmCharter charter = fixtures.createTestProject("WBS导入导出测试");
        projectId = charter.getId();
    }

    // ═══════════════════════════════════════════════════════════
    //  导入 WBS - 正常场景
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("导入 - 正常场景")
    class ImportNormalTests {

        @Test
        @DisplayName("导入根节点 - 成功")
        void importRootNodes_Success() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,需求分析,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,需求分析阶段\n"
                    + "2,概要设计,,功能需求,PROD-PM,DES,HIGH,HIGH,李四,80,20000,概要设计阶段";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(2, result.getTotal());
            assertEquals(2, result.getSuccess());
            assertEquals(0, result.getFailed());
            assertEquals(0, result.getDegraded());

            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertEquals(2, tree.size());
        }

        @Test
        @DisplayName("导入根节点和子节点 - 成功")
        void importRootAndChildren_Success() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,需求分析,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,需求分析\n"
                    + "2,详细设计,需求分析,,PROD-PM,DES,HIGH,HIGH,李四,80,20000,详细设计\n";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(2, result.getTotal());
            assertEquals(2, result.getSuccess());
            assertEquals(0, result.getFailed());
            assertEquals(0, result.getDegraded());

            // Verify tree structure: 1 root + 1 child
            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertEquals(1, tree.size());
            assertEquals("需求分析", tree.get(0).getName());
            assertNotNull(tree.get(0).getChildren());
            assertEquals(1, tree.get(0).getChildren().size());
            assertEquals("详细设计", tree.get(0).getChildren().get(0).getName());
        }

        @Test
        @DisplayName("导入到已有节点 - 子节点关联已存在的父节点")
        void importWithExistingParent_Success() throws Exception {
            // Create existing node first
            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setName("现有节点");
            req.setElementType("TASK");
            wbsElementBo.create(req);

            // Import child referencing existing node
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,新子节点,现有节点,,PROD-PM,TASK,LOW,LOW,王五,20,5000,新子节点";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getTotal());
            assertEquals(1, result.getSuccess());
            assertEquals(0, result.getFailed());
        }

        @Test
        @DisplayName("导入仅含表头的CSV - 无数据")
        void importHeaderOnly_ReturnsEmptyResult() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(0, result.getTotal());
            assertEquals(0, result.getSuccess());
            assertEquals(0, result.getFailed());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  导入 WBS - 异常场景
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("导入 - 异常场景")
    class ImportErrorTests {

        @Test
        @DisplayName("导入名称为空的行 - 失败统计")
        void importEmptyName_Failed() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,\n";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getTotal());
            assertEquals(0, result.getSuccess());
            assertEquals(1, result.getFailed());
            assertEquals(0, result.getDegraded());
            assertNotNull(result.getDetails());
            assertEquals(1, result.getDetails().size());
            assertEquals("名称为空", result.getDetails().get(0).getReason());
        }

        @Test
        @DisplayName("导入父节点不存在的子节点 - 降级为根节点")
        void importNonExistentParent_Degraded() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,孤儿节点,不存在的父节点,,PROD-PM,TASK,LOW,LOW,王五,20,5000,父节点不存在\n";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getTotal());
            assertEquals(0, result.getSuccess());
            assertEquals(0, result.getFailed());
            assertEquals(1, result.getDegraded());

            // The degraded node should still be created as a root node
            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertFalse(tree.isEmpty());
            assertEquals("孤儿节点", tree.get(0).getName());
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  导出 WBS
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("导出 WBS")
    class ExportTests {

        @Test
        @DisplayName("导出有数据的WBS - 返回CSV内容")
        void exportWithData_Success() throws Exception {
            // Create a WBS node first
            WbsCreateRequest req = new WbsCreateRequest();
            req.setProjectId(projectId);
            req.setName("导出测试节点");
            req.setElementType("TASK");
            req.setEffortEstimate("40");
            req.setBudgetEstimate("5000");
            wbsElementBo.create(req);

            MockHttpServletResponse response = new MockHttpServletResponse();
            transferBo.exportWbs(projectId, response);

            assertNotNull(response.getContentAsString());
            assertTrue(response.getContentAsString().contains("导出测试节点"));
            assertNotNull(response.getHeader("Content-Disposition"),
                    "应包含 Content-Disposition header");
        }

        @Test
        @DisplayName("导出空项目WBS - 返回空CSV")
        void exportEmptyProject_Success() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            transferBo.exportWbs(projectId, response);

            String content = response.getContentAsString();
            assertNotNull(content);
            // Should have header but no data rows (except header)
            String[] lines = content.split("\n");
            assertEquals(1, lines.length, "空项目导出应只有表头行");
        }
    }

    // ═══════════════════════════════════════════════════════════
    //  下载模板
    // ═══════════════════════════════════════════════════════════

    @Nested
    @DisplayName("下载模板")
    class DownloadTemplateTests {

        @Test
        @DisplayName("下载模板 - 返回CSV模板内容")
        void downloadTemplate_Success() throws Exception {
            MockHttpServletResponse response = new MockHttpServletResponse();
            transferBo.downloadTemplate(response);

            String content = response.getContentAsString();
            assertNotNull(content);

            // Should include header and sample data
            assertTrue(content.contains("名称"), "模板应包含表头");
            assertTrue(content.contains("需求分析"), "模板应包含示例数据");
            assertTrue(content.contains("1,需求分析,"), "模板应包含示例数据行");
            assertNotNull(response.getHeader("Content-Disposition"),
                    "Content-Disposition 应包含文件名");
        }
    }

    // ═══════════════════════════════════════════════════════
    //  补充：产品/模块 placeholder、责任人解析、递归导出
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("导入 - 引用解析")
    class ReferenceResolutionTests {

        @Test
        @DisplayName("产品编码不存在时创建 placeholder 产品与模块")
        void unknownProduct_createsPlaceholder() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,新需求,,功能需求,BRAND-NEW-PROD,NEW-MOD,HIGH,MEDIUM,张三,40,10000,描述";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getTotal());
            assertEquals(1, result.getSuccess());

            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertEquals(1, tree.size());
            assertEquals("BRAND-NEW-PROD", tree.get(0).getProductName());
        }

        @Test
        @DisplayName("计划责任人匹配真实用户时设置 plannedOwnerId")
        void matchingOwner_setsPlannedOwnerId() throws Exception {
            // seed 用户 user00000000000000000000000000002 真实姓名 张伟
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,需求分析,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张伟,40,10000,需求分析";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getSuccess());
            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertEquals("user00000000000000000000000000002", tree.get(0).getPlannedOwnerId());
        }
    }

    @Nested
    @DisplayName("导出 - 递归结构")
    class ExportRecursiveTests {

        @Test
        @DisplayName("导出含子节点的 WBS 时递归输出")
        void exportWithChildren_recurses() throws Exception {
            // 创建根节点与子节点
            WbsCreateRequest rootReq = new WbsCreateRequest();
            rootReq.setProjectId(projectId);
            rootReq.setName("根任务");
            rootReq.setElementType("TASK");
            WhPmWbsElement root = wbsElementBo.create(rootReq);

            WbsCreateRequest childReq = new WbsCreateRequest();
            childReq.setProjectId(projectId);
            childReq.setParentId(root.getId());
            childReq.setName("子任务");
            childReq.setElementType("TASK");
            wbsElementBo.create(childReq);

            org.springframework.mock.web.MockHttpServletResponse response = new org.springframework.mock.web.MockHttpServletResponse();
            transferBo.exportWbs(projectId, response);

            String content = response.getContentAsString();
            assertTrue(content.contains("根任务"));
            assertTrue(content.contains("子任务"));
            assertTrue(content.contains("父节点名称") || content.contains("序号"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  补充：CSV 列数不足 / 子节点空名 / 带产品字段导出
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("导入 - 边界场景")
    class EdgeCaseTests {

        @Test
        @DisplayName("CSV 列数不足时按空值处理")
        void shortRow_parsesDefaults() throws Exception {
            // 仅 2 列：序号,名称 —— 其余字段走默认空值分支
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,短行节点";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getTotal());
            assertEquals(1, result.getSuccess());
            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertEquals("短行节点", tree.get(0).getName());
            assertNull(tree.get(0).getProductId());
        }

        @Test
        @DisplayName("子节点名称为空 - 失败统计")
        void childEmptyName_failed() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,根节点,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,根\n"
                    + "2,,根节点,,PROD-PM,TASK,LOW,LOW,王五,20,5000,子节点空名";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(2, result.getTotal());
            assertEquals(1, result.getSuccess());
            assertEquals(1, result.getFailed());
        }
    }

    @Nested
    @DisplayName("导出 - 带产品模块字段")
    class ExportWithFieldsTests {

        @Test
        @DisplayName("导出时填充产品/模块/责任人字段")
        void exportWithProductFields() throws Exception {
            // 导入创建 placeholder 产品，导出时 productName 非空
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,需求分析,,功能需求,PROD-XYZ,MOD-1,HIGH,MEDIUM,张伟,40,10000,描述";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            transferBo.importWbs(file, projectId);

            org.springframework.mock.web.MockHttpServletResponse response =
                    new org.springframework.mock.web.MockHttpServletResponse();
            transferBo.exportWbs(projectId, response);

            String content = response.getContentAsString();
            assertTrue(content.contains("需求分析"));
            assertTrue(content.contains("PROD-XYZ"));
        }
    }

    // ═══════════════════════════════════════════════════════
    //  补充：空行/单列行/空编码/已有模块
    // ═══════════════════════════════════════════════════════

    @Nested
    @DisplayName("补充 - CSV 边界与引用解析")
    class ExtraCsvEdgeTests {

        @Test
        @DisplayName("CSV 含空行与单列行时容错")
        void blankAndSingleColumnRows_tolerated() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,正常节点,,功能需求,PROD-PM,REQ,HIGH,MEDIUM,张三,40,10000,正常\n"
                    + "\n"
                    + "仅单列";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            // 空行被跳过，单列行被当作名称为空 → 失败，正常节点成功
            assertEquals(2, result.getTotal());
            assertEquals(1, result.getSuccess());
        }

        @Test
        @DisplayName("产品编码/模块编码/责任人为空时不解析")
        void emptyReferences_skipResolution() throws Exception {
            String csv = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,无引用节点,,功能需求,,,,,,,40,10000,描述";

            MockMultipartFile file = new MockMultipartFile("file", "test.csv",
                    "text/csv", csv.getBytes(java.nio.charset.StandardCharsets.UTF_8));

            WbsImportResult result = transferBo.importWbs(file, projectId);

            assertEquals(1, result.getTotal());
            assertEquals(1, result.getSuccess());
            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertNull(tree.get(0).getProductId());
            assertNull(tree.get(0).getModuleId());
            assertNull(tree.get(0).getPlannedOwnerId());
        }

        @Test
        @DisplayName("模块已存在时直接关联而非创建 placeholder")
        void existingModule_associated() throws Exception {
            // 先通过一次导入创建 placeholder 产品与模块
            String csv1 = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,首次导入,,功能需求,EXIST-PROD,EXIST-MOD,HIGH,MEDIUM,,40,10000,首次";
            transferBo.importWbs(new MockMultipartFile("file", "a.csv",
                    "text/csv", csv1.getBytes(java.nio.charset.StandardCharsets.UTF_8)), projectId);

            // 第二次导入相同模块编码，应关联已有模块
            String csv2 = "序号,名称,父节点名称,产品类型,产品编码,模块编码,优先级,技术难度,计划责任人,估算工时(小时),估算成本(元),描述\n"
                    + "1,第二次导入,,功能需求,EXIST-PROD,EXIST-MOD,HIGH,MEDIUM,,40,10000,第二次";
            WbsImportResult result = transferBo.importWbs(new MockMultipartFile("file", "b.csv",
                    "text/csv", csv2.getBytes(java.nio.charset.StandardCharsets.UTF_8)), projectId);

            assertEquals(1, result.getSuccess());
            List<WhPmWbsElement> tree = wbsElementBo.getTreeByProjectId(projectId, null, null);
            assertEquals(2, tree.size());
        }
    }
}
