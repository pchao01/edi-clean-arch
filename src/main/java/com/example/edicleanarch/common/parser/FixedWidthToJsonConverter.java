package com.example.edicleanarch.common.parser;

import com.example.edicleanarch.common.schema.FieldDefinition;
import com.example.edicleanarch.common.schema.FixedWidthSchema;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class FixedWidthToJsonConverter {
    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert fixed-width content to JsonNode using provided schema.
     */
    public JsonNode convert(String content, FixedWidthSchema schema) {
        ObjectNode root = mapper.createObjectNode();
        String[] lines = content.split("\\r?\\n");

        ObjectNode header = null;
        ArrayNode records = mapper.createArrayNode();
        ObjectNode trailer = null;

        for (String line : lines) {
            if (line.isBlank()) continue;

            if (line.startsWith("CLM")) {
                header = parseFixedWidthLine(line, schema.getHeaderFields());
            } else if (line.startsWith("EOM")) {
                trailer = parseFixedWidthLine(line, schema.getTrailerFields());
            } else {
                ObjectNode record = parseFixedWidthLine(line, schema.getDataFields());
                records.add(record);
            }
        }

        root.set("header", header);
        root.set("records", records);
        root.set("trailer", trailer);

        // Add metadata
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("recordCount", records.size());
        metadata.put("parseTimestamp", LocalDateTime.now().toString());
        root.set("_metadata", metadata);

        return root;
    }
    /**
     * Parse a single fixed-width line into JsonNode.
     */
    private ObjectNode parseFixedWidthLine(String line, List<FieldDefinition> fields) {
        ObjectNode node = mapper.createObjectNode();

        if (fields == null) return node;

        for (FieldDefinition field : fields) {
            String value = extractField(line, field.getStart(), field.getEnd());

            if (field.isTrim()) {
                value = value.trim();
            }

            node.put(field.getName(), value);
        }

        return node;
    }
    /**
     * Extract field value from line by position.
     */
    private String extractField(String line, int start, int end) {
        if (start >= line.length()) return "";
        int actualEnd = Math.min(end, line.length());
        return line.substring(start, actualEnd);
    }
}
