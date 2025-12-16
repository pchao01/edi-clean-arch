package com.example.edicleanarch.x12.edi315.adapter.in.outbound;

import com.example.edicleanarch.common.annotation.KafkaAdapter;
import com.example.edicleanarch.x12.edi315.domain.service.outbound.GenerateEdi315Command;
import com.example.edicleanarch.x12.edi315.port.in.GenerateEdi315UseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * Inbound Adapter: Kafka trigger for EDI 315 outbound generation.
 * Listens for send requests and triggers generation.
 */
@Slf4j
@KafkaAdapter
@RequiredArgsConstructor
class GenerateEdi315KafkaConsumer {

    private final GenerateEdi315UseCase generateEdi315UseCase;
    private final ObjectMapper objectMapper;

    @KafkaListener(
            topics = "${edi.x12.edi315.outbound.kafka.trigger-topic:edi315-send-trigger}",
            groupId = "${edi.x12.edi315.outbound.kafka.group-id:edi315-outbound-group}",
            containerFactory = "ediKafkaListenerContainerFactory"
    )
    void handleSendRequest(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        log.debug("Received EDI 315 send trigger: {}", record.key());

        try {
            GenerateEdi315Command command = parseCommand(record.value());
            generateEdi315UseCase.generate(command);
            acknowledgment.acknowledge();
            log.info("EDI 315 outbound processed for SCAC: {}", command.scac());

        } catch (Exception e) {
            log.error("Failed to process EDI 315 send trigger", e);
            // Don't acknowledge - will be retried
        }
    }

    private GenerateEdi315Command parseCommand(String json) throws Exception {
        SendTriggerMessage message = objectMapper.readValue(json, SendTriggerMessage.class);

        if (message.eventIds != null && !message.eventIds.isEmpty()) {
            return GenerateEdi315Command.forEvents(message.scac, message.partnerId, message.eventIds);
        } else {
            return GenerateEdi315Command.forPending(message.scac, message.partnerId);
        }
    }

    /**
     * Kafka message format for send trigger.
     */
    record SendTriggerMessage(String scac, String partnerId, List<Long> eventIds) {}
}
