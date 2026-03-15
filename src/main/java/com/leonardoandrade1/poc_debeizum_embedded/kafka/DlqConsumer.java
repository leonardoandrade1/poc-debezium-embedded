package com.leonardoandrade1.poc_debeizum_embedded.kafka;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class DlqConsumer {

    private static final Logger log = LoggerFactory.getLogger(DlqConsumer.class);

    @KafkaListener(topics = { "${kafka.topic.product}.DLT",
            "${kafka.topic.orders}.DLT" }, groupId = "${kafka.consumer.group-id}-dlq", containerFactory = "dlqKafkaListenerContainerFactory")
    public void consumeDlqEvent(ConsumerRecord<String, String> record) {
        log.error("DLQ event received: topic={}, partition={}, offset={}, key={}, value={}",
                record.topic(), record.partition(), record.offset(), record.key(), record.value());

        // Extract original exception info from headers if available
        record.headers().forEach(header -> {
            if (header.key().startsWith("kafka_dlt-exception")) {
                log.error("DLQ header: {}={}", header.key(), new String(header.value()));
            }
        });
    }
}
