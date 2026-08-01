package com.wh.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("BusinessDataSourceConfig 测试")
class BusinessDataSourceConfigTest {

    @Test
    @DisplayName("创建 SQLite Druid 数据源成功")
    void createsSqliteDataSource() throws Exception {
        BusinessDataSourceConfig config = new BusinessDataSourceConfig();
        ReflectionTestUtils.setField(config, "url", "jdbc:sqlite::memory:");
        ReflectionTestUtils.setField(config, "driverClassName", "org.sqlite.JDBC");
        ReflectionTestUtils.setField(config, "initialSize", 1);
        ReflectionTestUtils.setField(config, "minIdle", 1);
        ReflectionTestUtils.setField(config, "maxActive", 2);
        ReflectionTestUtils.setField(config, "maxWait", 60000L);
        ReflectionTestUtils.setField(config, "validationQuery", "SELECT 1");

        DataSource ds = config.dataSource();
        assertNotNull(ds);
        // 验证可获取连接
        try (var conn = ds.getConnection()) {
            assertNotNull(conn);
        }
    }
}
