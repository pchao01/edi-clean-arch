package com.example.edicleanarch.x12.edi315.adapter.out.send;

import com.example.edicleanarch.common.annotation.PersistenceAdapter;
import com.example.edicleanarch.x12.edi315.port.out.SendEdiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.kafka.core.KafkaTemplate;

/**
 * Outbound Adapter: Send EDI 315 via Kafka.
 * Activated when edi.x12.edi315.outbound.send.mode=kafka
 */
@Slf4j
@PersistenceAdapter
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edi.x12.edi315.outbound.send.mode", havingValue = "kafka")
class Edi315KafkaProducerAdapter implements SendEdiPort {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${edi.x12.edi315.outbound.kafka.output-topic:edi315-outbound}")
    private String outputTopic;

    @Override
    public void send(String ediContent, String partnerId, String fileName) {
        log.info("Sending EDI 315 via Kafka: {} to topic: {}", fileName, outputTopic);

        kafkaTemplate.send(outputTopic, partnerId, ediContent)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send EDI 315 to Kafka: {}", fileName, ex);
                    } else {
                        log.info("Successfully sent EDI 315 to Kafka: {} offset: {}",
                                fileName, result.getRecordMetadata().offset());
                    }
                });
    }
}
