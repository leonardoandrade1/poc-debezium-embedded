package com.leonardoandrade1.poc_debeizum_embedded.handler;

import java.math.BigDecimal;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonardoandrade1.poc_debeizum_embedded.model.Product;
import com.leonardoandrade1.poc_debeizum_embedded.repository.ProductRepository;

import io.debezium.engine.ChangeEvent;

@Component
public class CdcEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CdcEventHandler.class);

    private final ProductRepository productRepository;
    private final ObjectMapper objectMapper;

    public CdcEventHandler(ProductRepository productRepository) {
        this.productRepository = productRepository;
        this.objectMapper = new ObjectMapper();
    }

    @Transactional
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
            log.info("Processing CDC event: operation={}, key={}", operation, event.key());

            switch (operation) {
                case "c", "r", "u" -> handleUpsert(payload);
                case "d" -> handleDelete(payload);
                default -> log.warn("Unknown operation '{}', skipping event.", operation);
            }
        } catch (Exception e) {
            log.error("Error processing CDC event: {}", e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractPayload(Map<String, Object> eventData) {
        if (eventData.containsKey("payload")) {
            return (Map<String, Object>) eventData.get("payload");
        }
        // If the event itself is the payload (depending on Debezium serialization
        // config)
        if (eventData.containsKey("op")) {
            return eventData;
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private void handleUpsert(Map<String, Object> payload) {
        Map<String, Object> after = (Map<String, Object>) payload.get("after");
        if (after == null) {
            log.warn("No 'after' field in upsert event, skipping.");
            return;
        }

        Product product = Product.builder()
                .id(toLong(after.get("id")))
                .name((String) after.get("name"))
                .description((String) after.get("description"))
                .price(toBigDecimal(after.get("price")))
                .build();

        productRepository.save(product);
        log.info("Upserted product: id={}, name={}", product.getId(), product.getName());
    }

    @SuppressWarnings("unchecked")
    private void handleDelete(Map<String, Object> payload) {
        Map<String, Object> before = (Map<String, Object>) payload.get("before");
        if (before == null) {
            log.warn("No 'before' field in delete event, skipping.");
            return;
        }

        Long id = toLong(before.get("id"));
        productRepository.deleteById(id);
        log.info("Deleted product: id={}", id);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue());
        }
        return new BigDecimal(String.valueOf(value));
    }
}
