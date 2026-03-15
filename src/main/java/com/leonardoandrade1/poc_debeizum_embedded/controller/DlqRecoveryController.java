package com.leonardoandrade1.poc_debeizum_embedded.controller;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.common.TopicPartition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dlq")
public class DlqRecoveryController {

    private static final Logger log = LoggerFactory.getLogger(DlqRecoveryController.class);

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ConsumerFactory<String, String> consumerFactory;

    public DlqRecoveryController(KafkaTemplate<String, String> kafkaTemplate,
            ConsumerFactory<String, String> consumerFactory) {
        this.kafkaTemplate = kafkaTemplate;
        this.consumerFactory = consumerFactory;
    }

    /**
     * Retries all messages from a DLT topic by republishing them to the original
     * topic.
     * Example: POST /api/dlq/retry/cdc.product
     * This will read all messages from cdc.product.DLT and send them back to
     * cdc.product.
     */
    @PostMapping("/retry/{topic}")
    public Map<String, Object> retryDlqMessages(@PathVariable String topic) {
        String dlqTopic = topic + ".DLT";
        log.info("DLQ recovery started: reading from {} to republish to {}", dlqTopic, topic);

        List<ConsumerRecord<String, String>> records = readAllFromTopic(dlqTopic);
        int count = 0;

        for (ConsumerRecord<String, String> record : records) {
            kafkaTemplate.send(topic, record.key(), record.value());
            count++;
            log.info("DLQ recovery: republished event key={} from {} to {}", record.key(), dlqTopic, topic);
        }

        log.info("DLQ recovery completed: republished {} events from {} to {}", count, dlqTopic, topic);

        return Map.of(
                "source", dlqTopic,
                "destination", topic,
                "republishedCount", count);
    }

    /**
     * Returns the count of pending DLQ messages for a given topic.
     * Example: GET /api/dlq/count/cdc.product
     */
    @GetMapping("/count/{topic}")
    public Map<String, Object> getDlqCount(@PathVariable String topic) {
        String dlqTopic = topic + ".DLT";
        List<ConsumerRecord<String, String>> records = readAllFromTopic(dlqTopic);

        return Map.of(
                "topic", dlqTopic,
                "pendingCount", records.size());
    }

    private List<ConsumerRecord<String, String>> readAllFromTopic(String topic) {
        List<ConsumerRecord<String, String>> allRecords = new ArrayList<>();

        String groupSuffix = "dlq-recovery-" + System.currentTimeMillis();

        try (Consumer<String, String> consumer = consumerFactory.createConsumer(groupSuffix, null)) {
            List<TopicPartition> partitions = consumer.partitionsFor(topic).stream()
                    .map(pi -> new TopicPartition(pi.topic(), pi.partition()))
                    .toList();

            if (partitions.isEmpty()) {
                return Collections.emptyList();
            }

            consumer.assign(partitions);
            consumer.seekToBeginning(partitions);

            ConsumerRecords<String, String> records = consumer.poll(Duration.ofSeconds(5));
            records.forEach(allRecords::add);
        } catch (Exception e) {
            log.error("Failed to read from DLQ topic {}: {}", topic, e.getMessage(), e);
        }

        return allRecords;
    }
}
