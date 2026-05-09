package com.wh.config;

import org.flowable.common.engine.impl.cfg.IdGenerator;

import java.util.UUID;

/**
 * UUID-based ID generator for Flowable, avoiding SQL sequence dependencies (required for SQLite).
 */
public class UuidIdGenerator implements IdGenerator {

    @Override
    public String getNextId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
