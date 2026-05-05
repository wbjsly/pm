package com.wh.config;

import org.flowable.engine.*;
import org.flowable.engine.impl.cfg.StandaloneProcessEngineConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import javax.sql.DataSource;

/**
 * Flowable uses H2 in-memory database since it doesn't support SQLite natively.
 * Business data stays in SQLite; Flowable tables live in H2 (in-memory, zero persistence).
 */
@Configuration
public class FlowableConfig {

    @Bean("flowableDataSource")
    public DataSource flowableDataSource() {
        org.h2.jdbcx.JdbcDataSource ds = new org.h2.jdbcx.JdbcDataSource();
        ds.setURL("jdbc:h2:mem:flowable;DB_CLOSE_DELAY=-1;MODE=Regular");
        ds.setUser("sa");
        ds.setPassword("");
        return ds;
    }

    @Bean
    @DependsOn("flowableDataSource")
    public ProcessEngineConfiguration processEngineConfiguration(
            @org.springframework.beans.factory.annotation.Qualifier("flowableDataSource") DataSource flowableDS) {
        StandaloneProcessEngineConfiguration config = new StandaloneProcessEngineConfiguration();
        config.setDataSource(flowableDS);
        config.setDatabaseSchemaUpdate("true");
        config.setAsyncExecutorActivate(false);
        config.setDisableIdmEngine(true);
        return config;
    }

    @Bean
    public ProcessEngine processEngine(ProcessEngineConfiguration processEngineConfiguration) {
        ProcessEngine engine = processEngineConfiguration.buildProcessEngine();
        // Deploy BPMN files
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
