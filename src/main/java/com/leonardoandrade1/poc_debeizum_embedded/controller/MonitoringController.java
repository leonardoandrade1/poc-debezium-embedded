package com.leonardoandrade1.poc_debeizum_embedded.controller;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.leonardoandrade1.poc_debeizum_embedded.engine.DebeziumEngineLifecycle;

@RestController
@RequestMapping("/api/debezium")
public class MonitoringController {

    private final DebeziumEngineLifecycle debeziumEngineLifecycle;

    public MonitoringController(DebeziumEngineLifecycle debeziumEngineLifecycle) {
        this.debeziumEngineLifecycle = debeziumEngineLifecycle;
    }

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        return Map.of(
                "engine", "mysql-connector",
                "running", debeziumEngineLifecycle.isRunning(),
                "timestamp", System.currentTimeMillis());
    }
}
