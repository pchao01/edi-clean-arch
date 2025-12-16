package com.example.edicleanarch.common.adapter.in.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.header.Header;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;

/**
 * Common Kafka Message Parser
 * Parses Kafka messages into content, partnerId, fileName.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class KafkaMessageParser {

    private final ObjectMapper objectMapper;

    /**
     * Parse a Kafka record into ParsedMessage.
     */
    public ParsedMessage parse(ConsumerRecord<String, String> record, String messageType) {
        String value = record.value();

        if (isJsonFormat(value)) {
            return parseJsonFormat(value, messageType);
        }

        return parseSimpleFormat(record, messageType);
    }

    private boolean isJsonFormat(String value) {
        return value != null && value.trim().startsWith("{");
    }

    private ParsedMessage parseJsonFormat(String value, String messageType) {
        try {
            JsonNode json = objectMapper.readTree(value);

            String content = getJsonField(json, "content");
            String partnerId = getJsonField(json, "partnerId");
            String fileName = getJsonField(json, "fileName");

            if (content == null || content.isEmpty()) {
                throw new IllegalArgumentException("Missing required field: content");
            }
            if (partnerId == null || partnerId.isEmpty()) {
                throw new IllegalArgumentException("Missing required field: partnerId");
            }
            if (fileName == null || fileName.isEmpty()) {
                fileName = generateFileName(partnerId, messageType);
            }

            return new ParsedMessage(content, partnerId, fileName);

        } catch (Exception e) {
            log.error("Error parsing JSON message: {}", e.getMessage());
            throw new IllegalArgumentException("Invalid JSON message format", e);
        }
    }

    private ParsedMessage parseSimpleFormat(ConsumerRecord<String, String> record, String messageType) {
        String content = record.value();
        String partnerId = record.key();
        String fileName = getHeader(record, "fileName");

        if (partnerId == null || partnerId.isEmpty()) {
            partnerId = getHeader(record, "partnerId");
        }

        if (content == null || content.isEmpty()) {
            throw new IllegalArgumentException("Message value (content) is empty");
        }
        if (partnerId == null || partnerId.isEmpty()) {
            partnerId = "UNKNOWN";
        }
        if (fileName == null || fileName.isEmpty()) {
            fileName = generateFileName(partnerId, messageType);
        }

        return new ParsedMessage(content, partnerId, fileName);
    }

    private String getJsonField(JsonNode json, String fieldName) {
        JsonNode node = json.get(fieldName);
        return node != null && !node.isNull() ? node.asText() : null;
    }

    private String getHeader(ConsumerRecord<String, String> record, String headerName) {
        Header header = record.headers().lastHeader(headerName);
        if (header != null && header.value() != null) {
            return new String(header.value(), StandardCharsets.UTF_8);
        }
        return null;
    }

    private String generateFileName(String partnerId, String messageType) {
        return String.format("%s_%s.%d.kafka",
                partnerId,
                messageType,
                System.currentTimeMillis());
    }

    /**
     * Parsed message data.
     */
    public record ParsedMessage(
            String content,
            String partnerId,
            String fileName
    ) {}
}
