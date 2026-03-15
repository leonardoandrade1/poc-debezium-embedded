package com.leonardoandrade1.poc_debeizum_embedded.kafka;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.leonardoandrade1.poc_debeizum_embedded.model.Order;
import com.leonardoandrade1.poc_debeizum_embedded.model.Product;
import com.leonardoandrade1.poc_debeizum_embedded.repository.OrderRepository;
import com.leonardoandrade1.poc_debeizum_embedded.repository.ProductRepository;

@Component
public class CdcKafkaConsumer {

    private static final Logger log = LoggerFactory.getLogger(CdcKafkaConsumer.class);

    private final ProductRepository productRepository;
    private final OrderRepository orderRepository;
    private final ObjectMapper objectMapper;

    public CdcKafkaConsumer(ProductRepository productRepository, OrderRepository orderRepository) {
        this.productRepository = productRepository;
        this.orderRepository = orderRepository;
        this.objectMapper = new ObjectMapper();
    }

    @KafkaListener(topics = "${kafka.topic.product}", groupId = "${kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consumeProductEvent(ConsumerRecord<String, String> record) {
        log.info("Processing CDC event: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        Map<String, Object> payload = parsePayload(record.value());
        if (payload == null)
            return;

        String operation = (String) payload.get("op");
        if (operation == null) {
            log.warn("No operation found in Kafka message: topic={}, offset={}", record.topic(), record.offset());
            return;
        }

        log.info("Processing product event: op={}, key={}", operation, record.key());

        switch (operation) {
            case "c", "r", "u" -> handleProductUpsert(payload);
            case "d" -> handleProductDelete(payload);
            default -> log.warn("Unknown operation '{}' for product topic, skipping.", operation);
        }
    }

    @KafkaListener(topics = "${kafka.topic.orders}", groupId = "${kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    @Transactional
    public void consumeOrderEvent(ConsumerRecord<String, String> record) {
        log.info("Processing CDC event: topic={}, partition={}, offset={}, key={}",
                record.topic(), record.partition(), record.offset(), record.key());

        Map<String, Object> payload = parsePayload(record.value());
        if (payload == null)
            return;

        String operation = (String) payload.get("op");
        if (operation == null) {
            log.warn("No operation found in Kafka message: topic={}, offset={}", record.topic(), record.offset());
            return;
        }

        log.info("Processing order event: op={}, key={}", operation, record.key());

        switch (operation) {
            case "c", "r", "u" -> handleOrderUpsert(payload);
            case "d" -> handleOrderDelete(payload);
            default -> log.warn("Unknown operation '{}' for orders topic, skipping.", operation);
        }
    }

    // ── Payload Parsing ──────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private Map<String, Object> parsePayload(String json) {
        try {
            Map<String, Object> eventData = objectMapper.readValue(
                    json, new TypeReference<Map<String, Object>>() {
                    });

            if (eventData.containsKey("payload")) {
                return (Map<String, Object>) eventData.get("payload");
            }
            if (eventData.containsKey("op")) {
                return eventData;
            }
            log.warn("No payload found in Kafka message");
            return null;
        } catch (Exception e) {
            log.error("Failed to parse CDC event JSON: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to parse CDC event", e);
        }
    }

    // ── Product Handlers ─────────────────────────────────────────────────────

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
        log.info("Product UPSERT completed: id={}", product.getId());
    }

    @SuppressWarnings("unchecked")
    private void handleProductDelete(Map<String, Object> payload) {
        Map<String, Object> before = (Map<String, Object>) payload.get("before");
        if (before == null)
            return;

        Long id = toLong(before.get("id"));
        productRepository.deleteById(id);
        log.info("Product DELETE completed: id={}", id);
    }

    // ── Order Handlers ───────────────────────────────────────────────────────

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
        log.info("Order UPSERT completed: id={}", order.getId());
    }

    @SuppressWarnings("unchecked")
    private void handleOrderDelete(Map<String, Object> payload) {
        Map<String, Object> before = (Map<String, Object>) payload.get("before");
        if (before == null)
            return;

        Long id = toLong(before.get("id"));
        orderRepository.deleteById(id);
        log.info("Order DELETE completed: id={}", id);
    }

    // ── Type Conversion Utilities ────────────────────────────────────────────

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
        if (value instanceof Long l) {
            return LocalDateTime.ofInstant(Instant.ofEpochMilli(l), ZoneId.systemDefault());
        }
        if (value instanceof String s) {
            if (s.endsWith("Z")) {
                return Instant.parse(s).atZone(ZoneId.systemDefault()).toLocalDateTime();
            }
            return LocalDateTime.parse(s);
        }
        return null;
    }
}
