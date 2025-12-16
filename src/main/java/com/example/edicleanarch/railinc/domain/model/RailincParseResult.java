package com.example.edicleanarch.railinc.domain.model;

import lombok.Getter;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Result of parsing a Railinc CLM file.
 *
 * Contains:
 * - Header fields (dynamic)
 * - Data records (dynamic, any fields from schema)
 * - Trailer fields (dynamic)
 * - Metadata (record count, parse timestamp)
 *
 * All fields are dynamic - no hardcoded structure.
 */
@Getter
public class RailincParseResult {

    private final Map<String, String> header;
    private final List<RailincRecord> records;
    private final Map<String, String> trailer;
    private final Map<String, Object> metadata;
    private final JsonNode sourceJson;

    private RailincParseResult(
            Map<String, String> header,
            List<RailincRecord> records,
            Map<String, String> trailer,
            Map<String, Object> metadata,
            JsonNode sourceJson) {
        this.header = header;
        this.records = records;
        this.trailer = trailer;
        this.metadata = metadata;
        this.sourceJson = sourceJson;
    }

    /**
     * Get header field value.
     */
    public String getHeaderField(String fieldName) {
        return header != null ? header.getOrDefault(fieldName, "") : "";
    }

    /**
     * Get trailer field value.
     */
    public String getTrailerField(String fieldName) {
        return trailer != null ? trailer.getOrDefault(fieldName, "") : "";
    }

    /**
     * Get record count from metadata.
     */
    public int getRecordCount() {
        Object count = metadata.get("recordCount");
        return count instanceof Integer ? (Integer) count : 0;
    }

    /**
     * Get trailer record count (from EOM line).
     */
    public int getTrailerRecordCount() {
        String countStr = getTrailerField("recordCount");
        try {
            return Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    /**
     * Validate record count matches trailer.
     */
    public boolean isRecordCountValid() {
        return getRecordCount() == getTrailerRecordCount();
    }

    /**
     * Create from JsonNode (output from FixedWidthToJsonConverter).
     */
    public static RailincParseResult fromJsonNode(JsonNode jsonNode) {
        // Parse header
        Map<String, String> header = parseSection(jsonNode.get("header"));

        // Parse records
        List<RailincRecord> records = new ArrayList<>();
        JsonNode recordsNode = jsonNode.get("records");
        if (recordsNode != null && recordsNode.isArray()) {
            for (JsonNode recordNode : recordsNode) records.add(RailincRecord.fromJsonNode(recordNode));
        }

        // Parse trailer
        Map<String, String> trailer = parseSection(jsonNode.get("trailer"));

        // Parse metadata
        Map<String, Object> metadata = new LinkedHashMap<>();
        JsonNode metaNode = jsonNode.get("_metadata");
        if (metaNode != null) {
            if (metaNode.has("recordCount")) {
                metadata.put("recordCount", metaNode.get("recordCount").asInt());
            }
            if (metaNode.has("parseTimestamp")) {
                metadata.put("parseTimestamp", metaNode.get("parseTimestamp").asText());
            }
        }

        return new RailincParseResult(header, records, trailer, metadata, jsonNode);
    }

    private static Map<String, String> parseSection(JsonNode node) {
        Map<String, String> fields = new LinkedHashMap<>();
        if (node != null && node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String value = entry.getValue().isNull() ? "" : entry.getValue().asText();
                fields.put(entry.getKey(), value);
            }
        }
        return fields;
    }
}
