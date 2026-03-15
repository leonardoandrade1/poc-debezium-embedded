package com.leonardoandrade1.poc_debeizum_embedded.handler;

import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonardoandrade1.poc_debeizum_embedded.kafka.CdcKafkaProducer;

import io.debezium.engine.ChangeEvent;

@Component
public class CdcEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CdcEventHandler.class);

    private final CdcKafkaProducer kafkaProducer;
    private final ObjectMapper objectMapper;

    public CdcEventHandler(CdcKafkaProducer kafkaProducer) {
        this.kafkaProducer = kafkaProducer;
        this.objectMapper = new ObjectMapper();
    }

    public void handleEvent(ChangeEvent<String, String> event) {
        if (event.value() == null) {
            log.debug("Received tombstone event (null value), skipping.");
            return;
        }

        try {
            Map<String, Object> eventData = objectMapper.readValue(
                    event.value(), new TypeReference<Map<String, Object>>() {
                    });

            Map<String, Object> payload = extractPayload(eventData);
            if (payload == null) {
                log.warn("No payload found in event: {}", event.key());
                return;
            }

            String operation = (String) payload.get("op");
            if (operation == null) {
                log.warn("No operation found in event: {}", event.key());
                return;
            }

            @SuppressWarnings("unchecked")
            Map<String, Object> source = (Map<String, Object>) payload.get("source");
            String table = (source != null) ? (String) source.get("table") : "unknown";

            // Extract the record key for Kafka partitioning
            String key = extractRecordKey(payload, operation);

            log.info("Dispatching CDC event to Kafka: table={}, operation={}, key={}", table, operation, key);

            kafkaProducer.send(table, key, event.value());

        } catch (Exception e) {
            log.error("CRITICAL: Error processing CDC event. Stopping engine to prevent data loss. Error: {}",
                    e.getMessage(), e);
            throw new RuntimeException("CDC processing failed, stopping engine.", e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Map<String, Object> eventData) {
        if (eventData.containsKey("payload")) {
            return (Map<String, Object>) eventData.get("payload");
        }
        if (eventData.containsKey("op")) {
            return eventData;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private String extractRecordKey(Map<String, Object> payload, String operation) {
        Map<String, Object> data;
        if ("d".equals(operation)) {
            data = (Map<String, Object>) payload.get("before");
        } else {
            data = (Map<String, Object>) payload.get("after");
        }

        if (data != null && data.get("id") != null) {
            return String.valueOf(data.get("id"));
        }

        return "unknown";
    }
}
