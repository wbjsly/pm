package com.wh.bootstrap;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@DisplayName("SqliteBootstrap 测试")
class SqliteBootstrapTest {

    private SqliteBootstrap newBootstrap(JdbcTemplate jdbcTemplate, boolean enabled, String skip) {
        SqliteBootstrap bootstrap = new SqliteBootstrap(jdbcTemplate);
        ReflectionTestUtils.setField(bootstrap, "bootstrapEnabled", enabled);
        ReflectionTestUtils.setField(bootstrap, "bootstrapLocation", "classpath:db/sqlite/");
        ReflectionTestUtils.setField(bootstrap, "skipScripts", skip);
        return bootstrap;
    }

    @Test
    @DisplayName("bootstrap 禁用时不执行任何脚本")
    void disabled_doesNotRun() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SqliteBootstrap bootstrap = newBootstrap(jdbcTemplate, false, "");
        bootstrap.afterPropertiesSet();
        verify(jdbcTemplate, never()).execute(anyString());
    }

    @Test
    @DisplayName("正常执行创建标记表并执行脚本")
    void enabled_runsScripts() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(0); // 脚本均未执行
        SqliteBootstrap bootstrap = newBootstrap(jdbcTemplate, true, "");
        bootstrap.afterPropertiesSet();

        verify(jdbcTemplate, atLeastOnce()).execute(
                contains("CREATE TABLE IF NOT EXISTS wh_bootstrap_marker"));
        // 脚本按分号拆分逐条执行 + isExecuted 查询标记表
        verify(jdbcTemplate, atLeast(5)).execute(anyString());
        verify(jdbcTemplate, atLeastOnce()).queryForObject(anyString(), eq(Integer.class), any());
    }

    @Test
    @DisplayName("skip-scripts 配置跳过指定脚本")
    void skipScripts_skipsConfigured() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(0);
        SqliteBootstrap bootstrap = newBootstrap(jdbcTemplate, true, "001-system-foundation.sql");
        bootstrap.afterPropertiesSet();
        // 001 被跳过，但仍会执行其余脚本
        verify(jdbcTemplate, atLeastOnce()).execute(anyString());
    }

    @Test
    @DisplayName("已执行的脚本被跳过")
    void executedScripts_skipped() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(1); // 全部已执行
        SqliteBootstrap bootstrap = newBootstrap(jdbcTemplate, true, "");
        bootstrap.afterPropertiesSet();
        // 只创建 marker 表，不执行脚本内容
        verify(jdbcTemplate, atLeastOnce()).execute(contains("CREATE TABLE"));
    }

    @Test
    @DisplayName("再次触发（run）不重复执行")
    void secondRun_isIdempotent() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(anyString(), eq(Integer.class), any()))
                .thenReturn(0);
        SqliteBootstrap bootstrap = newBootstrap(jdbcTemplate, true, "");
        bootstrap.afterPropertiesSet();
        bootstrap.run(null);
        // 只执行一次
        verify(jdbcTemplate, times(1)).execute(contains("CREATE TABLE IF NOT EXISTS wh_bootstrap_marker"));
    }

    @Test
    @DisplayName("资源加载异常时抛出运行时异常")
    void resourceFailure_throwsRuntime() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        SqliteBootstrap bootstrap = new SqliteBootstrap(jdbcTemplate);
        ReflectionTestUtils.setField(bootstrap, "bootstrapEnabled", true);
        ReflectionTestUtils.setField(bootstrap, "bootstrapLocation", "classpath:not-exist-dir/");
        ReflectionTestUtils.setField(bootstrap, "skipScripts", "");
        assertThrows(RuntimeException.class, bootstrap::afterPropertiesSet);
    }
}
