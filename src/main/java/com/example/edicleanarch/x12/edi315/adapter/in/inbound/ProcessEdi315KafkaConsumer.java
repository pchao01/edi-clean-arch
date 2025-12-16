package com.example.edicleanarch.x12.edi315.adapter.in.inbound;

import com.example.edicleanarch.common.adapter.in.kafka.AbstractKafkaConsumerAdapter;
import com.example.edicleanarch.common.adapter.in.kafka.KafkaMessageParser;
import com.example.edicleanarch.common.annotation.KafkaAdapter;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.x12.edi315.domain.service.inbound.ProcessEdi315Command;
import com.example.edicleanarch.x12.edi315.domain.service.inbound.ProcessEdi315Service;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

/**
 * Inbound Adapter: Kafka Consumer for EDI 315 files
 *
 * Receives EDI 315 messages from Kafka topic and delegates to ProcessEdi315Service.
 *
 * Flow:
 * Kafka Topic -> This Adapter -> ProcessEdi315Service -> SaveEdi315EventsPort -> Database
 */
@KafkaAdapter
@RequiredArgsConstructor
class ProcessEdi315KafkaConsumer extends AbstractKafkaConsumerAdapter<ProcessEdi315Command> {

    private static final String MESSAGE_TYPE = "EDI_315";

    private final ProcessEdi315Service processEdi315Service;
    private final KafkaMessageParser messageParser;

    @KafkaListener(
            topics = "${edi.edi315.kafka.topic:edi315-inbound}",
            groupId = "${edi.edi315.kafka.group-id:edi315-processor-group}",
            concurrency = "${edi.edi315.kafka.concurrency:1}"
    )
    @Override
    protected void handleMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        super.handleMessage(record, acknowledgment);
    }

    @Override
    protected ProcessEdiFileUseCase<ProcessEdi315Command> getUseCase() {
        return processEdi315Service;
    }

    @Override
    protected ProcessEdi315Command parseMessage(ConsumerRecord<String, String> record) {
        var parsed = messageParser.parse(record, MESSAGE_TYPE);
        return new ProcessEdi315Command(parsed.content(), parsed.partnerId(), parsed.fileName());
    }

    @Override
    protected String getMessageType() {
        return MESSAGE_TYPE;
    }
}
