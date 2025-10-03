package com.francelabs.datafari.config;

import com.francelabs.datafari.service.db.SqlService;
import com.francelabs.datafari.service.db.SqlServiceBridge;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

/**
 * Bridge configuration that publishes the Spring-managed SqlService
 * into the static SqlServiceBridge once the Spring context is ready.
 */
@Component
public class SqlServiceBridgeConfig {

    private final SqlService sqlService;

    // Spring injects the singleton SqlService here
    public SqlServiceBridgeConfig(SqlService sqlService) {
        this.sqlService = sqlService;
    }

    /**
     * When the Spring context is fully initialized, 
     * we expose the SqlService into the static bridge.
     */
    @EventListener(ContextRefreshedEvent.class)
    public void onContextRefreshed() {
        SqlServiceBridge.set(sqlService);
    }
}