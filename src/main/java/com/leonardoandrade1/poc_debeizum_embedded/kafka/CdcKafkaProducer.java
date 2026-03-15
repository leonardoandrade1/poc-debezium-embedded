package com.leonardoandrade1.poc_debeizum_embedded.kafka;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;

@Service
public class CdcKafkaProducer {

    private static final Logger log = LoggerFactory.getLogger(CdcKafkaProducer.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Map<String, String> tableToTopicMap;

    public CdcKafkaProducer(
            KafkaTemplate<String, String> kafkaTemplate,
            @Value("${kafka.topic.product}") String productTopic,
            @Value("${kafka.topic.orders}") String ordersTopic) {
        this.kafkaTemplate = kafkaTemplate;
        this.tableToTopicMap = Map.of(
                "product", productTopic,
                "orders", ordersTopic);
    }

    /**
     * Sends a CDC event to the appropriate Kafka topic based on the source table.
     *
     * @param table     source table name (e.g. "product", "orders")
     * @param key       record key (typically the row ID)
     * @param eventJson full CDC event JSON payload
     */
    public void send(String table, String key, String eventJson) {
        String topic = tableToTopicMap.get(table);
        if (topic == null) {
            log.warn("No Kafka topic mapped for table '{}', skipping event with key={}", table, key);
            return;
        }

        log.info("Producing CDC event: topic={}, key={}", topic, key);

        CompletableFuture<SendResult<String, String>> future = kafkaTemplate.send(topic, key, eventJson);

        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to produce CDC event: topic={}, key={}, error={}",
                        topic, key, ex.getMessage(), ex);
            } else {
                log.info("CDC event produced successfully: topic={}, key={}, partition={}, offset={}",
                        topic, key,
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        });
    }
}
