package com.wh.bootstrap;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SqliteBootstrap implements InitializingBean, ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;
    private final AtomicBoolean initialized = new AtomicBoolean(false);

    @Value("${app.sqlite.bootstrap.enabled:true}")
    private boolean bootstrapEnabled;

    @Value("${app.sqlite.bootstrap.location:classpath:db/sqlite/}")
    private String bootstrapLocation;

    @Value("${app.sqlite.bootstrap.skip-scripts:}")
    private String skipScripts;

    public SqliteBootstrap(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void afterPropertiesSet() {
        runMigrations();
    }

    @Override
    public void run(ApplicationArguments args) {
        runMigrations();
    }

    private void runMigrations() {
        if (!initialized.compareAndSet(false, true)) {
            return;
        }

        if (!bootstrapEnabled) {
            log.info("SQLite Bootstrap is disabled.");
            return;
        }

        log.info("=== Starting SQLite Bootstrap ===");

        // 1. Create marker table
        createMarkerTable();

        // 2. Scan and execute SQL scripts
        try {
            PathMatchingResourcePatternResolver resolver = new PathMatchingResourcePatternResolver();
            Resource[] resources = resolver.getResources(bootstrapLocation + "*.sql");

            List<String> skipList = Arrays.stream(skipScripts.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .collect(Collectors.toList());

            // Sort by filename
            List<Resource> sortedResources = Arrays.stream(resources)
                    .sorted((a, b) -> {
                        try {
                            return a.getFilename().compareTo(b.getFilename());
                        } catch (Exception e) {
                            return 0;
                        }
                    })
                    .collect(Collectors.toList());

            for (Resource resource : sortedResources) {
                String filename = resource.getFilename();
                if (skipList.contains(filename)) {
                    log.info("Skipping script (configured): {}", filename);
                    continue;
                }

                if (isExecuted(filename)) {
                    log.info("Skipping script (already executed): {}", filename);
                    continue;
                }

                log.info("Executing SQL script: {}", filename);
                String sql = readResource(resource);
                executeSqlScript(sql);
                markExecuted(filename);
                log.info("SQL script executed successfully: {}", filename);
            }

        } catch (Exception e) {
            log.error("Failed to execute bootstrap scripts", e);
            throw new RuntimeException("SQLite Bootstrap failed", e);
        }

        log.info("=== SQLite Bootstrap Complete ===");
    }

    private void createMarkerTable() {
        jdbcTemplate.execute(
                "CREATE TABLE IF NOT EXISTS wh_bootstrap_marker (" +
                "  SCRIPT_NAME TEXT NOT NULL PRIMARY KEY, " +
                "  EXECUTED_AT TEXT NOT NULL DEFAULT (datetime('now', 'localtime'))" +
                ")"
        );
    }

    private boolean isExecuted(String scriptName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(1) FROM wh_bootstrap_marker WHERE SCRIPT_NAME = ?",
                Integer.class, scriptName
        );
        return count != null && count > 0;
    }

    private void markExecuted(String scriptName) {
        jdbcTemplate.update(
                "INSERT INTO wh_bootstrap_marker (SCRIPT_NAME, EXECUTED_AT) VALUES (?, datetime('now', 'localtime'))",
                scriptName
        );
    }

    private String readResource(Resource resource) throws Exception {
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
            return reader.lines().collect(Collectors.joining("\n"));
        }
    }

    private void executeSqlScript(String sql) {
        // Split by semicolons and execute each statement individually
        String[] statements = sql.split(";", -1);
        for (String stmt : statements) {
            String trimmed = stmt.trim();
            if (trimmed.isEmpty()) {
                continue;
            }
            // Remove SQL comment lines
            String cleaned = trimmed.replaceAll("(?m)^--.*$", "").trim();
            if (!cleaned.isEmpty()) {
                try {
                    jdbcTemplate.execute(cleaned);
                } catch (Exception e) {
                    log.warn("SQL execution warning: {}", e.getMessage());
                }
            }
        }
    }
}
