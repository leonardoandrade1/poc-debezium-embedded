package com.leonardoandrade1.poc_debeizum_embedded.engine;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.SmartLifecycle;
import org.springframework.stereotype.Component;

import com.leonardoandrade1.poc_debeizum_embedded.handler.CdcEventHandler;

import io.debezium.config.Configuration;

import io.debezium.engine.ChangeEvent;
import io.debezium.engine.DebeziumEngine;
import io.debezium.engine.format.Json;

@Component
public class DebeziumEngineLifecycle implements SmartLifecycle {

    private static final Logger log = LoggerFactory.getLogger(DebeziumEngineLifecycle.class);

    private final Configuration debeziumConfig;
    private final CdcEventHandler cdcEventHandler;

    private DebeziumEngine<ChangeEvent<String, String>> engine;
    private ExecutorService executor;
    private volatile boolean running = false;

    public DebeziumEngineLifecycle(Configuration debeziumConfig, CdcEventHandler cdcEventHandler) {
        this.debeziumConfig = debeziumConfig;
        this.cdcEventHandler = cdcEventHandler;
    }

    @Override
    public void start() {
        log.info("Starting Debezium Engine...");

        this.engine = DebeziumEngine.create(Json.class)
                .using(debeziumConfig.asProperties())
                .notifying(record -> cdcEventHandler.handleEvent(record))
                .using((success, message, error) -> {
                    if (!success) {
                        log.error("Debezium Engine completed with error: {}", message, error);
                    } else {
                        log.info("Debezium Engine completed successfully: {}", message);
                    }
                })
                .build();

        this.executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "debezium-engine");
            t.setDaemon(true);
            return t;
        });

        this.executor.execute(this.engine);
        this.running = true;

        log.info("Debezium Engine started successfully.");
    }

    @Override
    public void stop() {
        log.info("Stopping Debezium Engine...");

        if (this.engine != null) {
            try {
                this.engine.close();
            } catch (IOException e) {
                log.error("Error closing Debezium Engine: {}", e.getMessage(), e);
            }
        }

        if (this.executor != null) {
            this.executor.shutdown();
            try {
                if (!this.executor.awaitTermination(10, TimeUnit.SECONDS)) {
                    this.executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                this.executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }

        this.running = false;
        log.info("Debezium Engine stopped.");
    }

    @Override
    public boolean isRunning() {
        return this.running;
    }

    @Override
    public int getPhase() {
        // Start late, stop early — ensures datasources are ready before Debezium starts
        return Integer.MAX_VALUE;
    }

    @Override
    public boolean isAutoStartup() {
        return true;
    }
}
