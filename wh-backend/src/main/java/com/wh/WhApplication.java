package com.wh;

import org.flowable.spring.boot.ProcessEngineAutoConfiguration;
import org.flowable.spring.boot.ProcessEngineServicesAutoConfiguration;
import org.flowable.spring.boot.FlowableSecurityAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineAutoConfiguration;
import org.flowable.spring.boot.app.AppEngineServicesAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineAutoConfiguration;
import org.flowable.spring.boot.cmmn.CmmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.content.ContentEngineAutoConfiguration;
import org.flowable.spring.boot.content.ContentEngineServicesAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineAutoConfiguration;
import org.flowable.spring.boot.dmn.DmnEngineServicesAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryAutoConfiguration;
import org.flowable.spring.boot.eventregistry.EventRegistryServicesAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineAutoConfiguration;
import org.flowable.spring.boot.form.FormEngineServicesAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineAutoConfiguration;
import org.flowable.spring.boot.idm.IdmEngineServicesAutoConfiguration;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@SpringBootApplication(exclude = {
        FlowableSecurityAutoConfiguration.class
})
@EnableAutoConfiguration(exclude = {
        ProcessEngineAutoConfiguration.class,
        ProcessEngineServicesAutoConfiguration.class,
        AppEngineAutoConfiguration.class,
        AppEngineServicesAutoConfiguration.class,
        CmmnEngineAutoConfiguration.class,
        CmmnEngineServicesAutoConfiguration.class,
        DmnEngineAutoConfiguration.class,
        DmnEngineServicesAutoConfiguration.class,
        FormEngineAutoConfiguration.class,
        FormEngineServicesAutoConfiguration.class,
        EventRegistryAutoConfiguration.class,
        EventRegistryServicesAutoConfiguration.class,
        IdmEngineAutoConfiguration.class,
        IdmEngineServicesAutoConfiguration.class,
        ContentEngineAutoConfiguration.class,
        ContentEngineServicesAutoConfiguration.class
})
@EnableTransactionManagement
@EnableScheduling
@EnableAsync
@MapperScan("com.wh.dao")
public class WhApplication {

    public static void main(String[] args) {
        SpringApplication.run(WhApplication.class, args);
    }
}
