package com.leonardoandrade1.poc_debeizum_embedded.handler;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonardoandrade1.poc_debeizum_embedded.model.Order;
import com.leonardoandrade1.poc_debeizum_embedded.model.Product;
import com.leonardoandrade1.poc_debeizum_embedded.repository.OrderRepository;
import com.leonardoandrade1.poc_debeizum_embedded.repository.ProductRepository;

import io.debezium.engine.ChangeEvent;

@Component
public class CdcEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CdcEventHandler.class);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public CdcEventHandler(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
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

            Map<String, Object> source = (Map<String, Object>) payload.get("source");
            String table = (source != null) ? (String) source.get("table") : "unknown";

            log.info("Processing CDC event: table={}, operation={}, key={}", table, operation, event.key());

            switch (table) {
                case "product" -> handleProductEvent(operation, payload);
                case "orders" -> handleOrderEvent(operation, payload);
                default -> log.warn("Event from unknown table '{}', skipping.", table);
            }
        } catch (Exception e) {
            // Ensure at least once delivery:
            // This ensure that the offset will not be updated, because we're stoping engine
            // immediately after an error, so the event will try to process again when
            // the engine is restarted based on the last committed offset.
            log.error("CRITICAL: Error processing CDC event. Stopping engine to prevent data loss. Error: {}",
                    e.getMessage(), e);
            throw new RuntimeException("CDC processing failed, stopping engine.", e);
        }
    }

    private void handleProductEvent(String operation, Map<String, Object> payload) {
        switch (operation) {
            case "c", "r", "u" -> handleProductUpsert(payload);
            case "d" -> handleProductDelete(payload);
            default -> log.warn("Unknown operation '{}' for product table, skipping.", operation);
        }
    }

    private void handleOrderEvent(String operation, Map<String, Object> payload) {
        switch (operation) {
            case "c", "r", "u" -> handleOrderUpsert(payload);
            case "d" -> handleOrderDelete(payload);
            default -> log.warn("Unknown operation '{}' for orders table, skipping.", operation);
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
    private void handleProductUpsert(Map<String, Object> payload) {
        Map<String, Object> after = (Map<String, Object>) payload.get("after");
        if (after == null)
            return;

        Product product = Product.builder()
                .id(toLong(after.get("id")))
                .name((String) after.get("name"))
                .description((String) after.get("description"))
                .price(toBigDecimal(after.get("price")))
                .build();

        productRepository.save(product);
        log.info("Product UPSERT: id={}", product.getId());
    }

    @SuppressWarnings("unchecked")
    private void handleProductDelete(Map<String, Object> payload) {
        Map<String, Object> before = (Map<String, Object>) payload.get("before");
        if (before == null)
            return;

        Long id = toLong(before.get("id"));
        productRepository.deleteById(id);
        log.info("Product DELETE: id={}", id);
    }

    @SuppressWarnings("unchecked")
    private void handleOrderUpsert(Map<String, Object> payload) {
        Map<String, Object> after = (Map<String, Object>) payload.get("after");
        if (after == null)
            return;

        Order order = Order.builder()
                .id(toLong(after.get("id")))
                .productId(toLong(after.get("product_id")))
                .quantity((Integer) after.get("quantity"))
                .customerEmail((String) after.get("customer_email"))
                .orderDate(toLocalDateTime(after.get("order_date")))
                .build();

        orderRepository.save(order);
        log.info("Order UPSERT: id={}", order.getId());
    }

    @SuppressWarnings("unchecked")
    private void handleOrderDelete(Map<String, Object> payload) {
        Map<String, Object> before = (Map<String, Object>) payload.get("before");
        if (before == null)
            return;

        Long id = toLong(before.get("id"));
        orderRepository.deleteById(id);
        log.info("Order DELETE: id={}", id);
    }

    private Long toLong(Object value) {
        if (value == null)
            return null;
        if (value instanceof Integer i)
            return i.longValue();
        if (value instanceof Long l)
            return l;
        if (value instanceof String s)
            return Long.parseLong(s);
        return null;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value == null)
            return null;
        if (value instanceof Integer i)
            return new BigDecimal(i);
        if (value instanceof Double d)
            return new BigDecimal(d);
        if (value instanceof String s)
            return new BigDecimal(s);
        return null;
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null)
            return null;
        // Debezium often returns timestamps as long (epoch millis or micro)
        if (value instanceof Long l) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
        }
        if (value instanceof String s) {
            return LocalDateTime.parse(s);
        }
        return null;
    }
}
