package com.wh.config;

import org.apache.ibatis.session.Configuration;
import org.apache.ibatis.type.JdbcType;
import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;
import java.util.Date;

/**
 * Flowable shares the same SQLite database as business data.
 * Uses H2 dialect (closest to SQLite) with strong UUIDs to avoid sequence dependencies.
 * Custom initMybatisTypeHandlers registers SQLite-compatible TypeHandlers before XML parsing.
 */
@org.springframework.context.annotation.Configuration
public class FlowableConfig {

    /**
     * Custom ProcessEngineConfiguration that registers SQLite-compatible MyBatis TypeHandlers
     * during initMybatisTypeHandlers, which is called BEFORE XML mapper parsing.
     */
    static class SqliteProcessEngineConfiguration extends StandaloneProcessEngineConfiguration {
        @Override
        public void initMybatisTypeHandlers(Configuration configuration) {
            super.initMybatisTypeHandlers(configuration);
            configuration.getTypeHandlerRegistry()
                    .register(Date.class, new SqliteDateTypeHandler());
            configuration.getTypeHandlerRegistry()
                    .register(byte[].class, JdbcType.BLOB, new SqliteBlobTypeHandler());
        }
    }

    @Bean
    @DependsOn({"dataSource", "sqliteBootstrap"})
    public ProcessEngineConfiguration processEngineConfiguration(
            @org.springframework.beans.factory.annotation.Qualifier("dataSource") DataSource dataSource) {
        SqliteProcessEngineConfiguration config = new SqliteProcessEngineConfiguration();
        config.setDataSource(dataSource);
        config.setDatabaseType("h2");
        config.setIdGenerator(new UuidIdGenerator());
        config.setDatabaseSchemaUpdate("false");
        config.setAsyncExecutorActivate(false);
        config.setDisableIdmEngine(true);
        config.setDisableEventRegistry(true);
        return config;
    }

    @Bean
    public ProcessEngine processEngine(ProcessEngineConfiguration processEngineConfiguration) {
        ProcessEngine engine = processEngineConfiguration.buildProcessEngine();

        engine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("bpmn/pm-charter-approval.bpmn20.xml")
                .name("pm-charter-approval")
                .deploy();
        engine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("bpmn/pm-budget-approval.bpmn20.xml")
                .name("pm-budget-approval")
                .deploy();
        engine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("bpmn/pm-wbs-modify-approval.bpmn20.xml")
                .name("pm-wbs-modify-approval")
                .deploy();
        engine.getRepositoryService()
                .createDeployment()
                .addClasspathResource("bpmn/pm-deliverable-approval.bpmn20.xml")
                .name("pm-deliverable-approval")
                .deploy();
        return engine;
    }

    @Bean
    public RuntimeService runtimeService(ProcessEngine processEngine) {
        return processEngine.getRuntimeService();
    }

    @Bean
    public TaskService taskService(ProcessEngine processEngine) {
        return processEngine.getTaskService();
    }

    @Bean
    public RepositoryService repositoryService(ProcessEngine processEngine) {
        return processEngine.getRepositoryService();
    }

    @Bean
    public HistoryService historyService(ProcessEngine processEngine) {
        return processEngine.getHistoryService();
    }

    @Bean
    public ManagementService managementService(ProcessEngine processEngine) {
        return processEngine.getManagementService();
    }
}
