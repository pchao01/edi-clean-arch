package com.example.edicleanarch.railinc.adapter.in;

import com.example.edicleanarch.common.adapter.in.kafka.AbstractKafkaConsumerAdapter;
import com.example.edicleanarch.common.adapter.in.kafka.KafkaMessageParser;
import com.example.edicleanarch.common.annotation.KafkaAdapter;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.railinc.domain.service.ProcessRailincFileCommand;
import com.example.edicleanarch.railinc.domain.service.ProcessRailincFileService;
import lombok.RequiredArgsConstructor;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;

@KafkaAdapter
@RequiredArgsConstructor
class RailincKafkaConsumerAdapter extends AbstractKafkaConsumerAdapter<ProcessRailincFileCommand> {

    private static final String MESSAGE_TYPE = "RAILINC_CLM";

    private final ProcessRailincFileService processRailincFileService;
    private final KafkaMessageParser messageParser;

    @KafkaListener(
            topics = "${edi.railinc.kafka.topic:railinc-inbound}",
            groupId = "${edi.railinc.kafka.group-id:railinc-processor-group}",
            concurrency = "${edi.railinc.kafka.concurrency:1}"
    )
    @Override
    protected void handleMessage(ConsumerRecord<String, String> record, Acknowledgment acknowledgment) {
        super.handleMessage(record, acknowledgment);
    }

    @Override
    protected ProcessEdiFileUseCase<ProcessRailincFileCommand> getUseCase() {
        return processRailincFileService;
    }

    @Override
    protected ProcessRailincFileCommand parseMessage(ConsumerRecord<String, String> record) {
        var parsed = messageParser.parse(record, MESSAGE_TYPE);
        return new ProcessRailincFileCommand(parsed.content(), parsed.partnerId(), parsed.fileName());
    }

    @Override
    protected String getMessageType() {
        return MESSAGE_TYPE;
    }
}
