package com.example.edicleanarch.railinc.domain.model;


import lombok.Getter;
import com.fasterxml.jackson.databind.JsonNode;


import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Dynamic Railinc record that holds all fields from schema.
 *
 * No hardcoded fields - all data stored in a Map.
 * New fields can be added to schema YAML without code changes.
 */
@Getter
public class RailincRecord {

    /**
     * Dynamic fields parsed from fixed-width data.
     * Key = field name from schema (e.g., "equipmentInitial", "mblNo")
     * Value = parsed value (trimmed string)
     */
    private final Map<String, String> fields;

    /**
     * Original JsonNode for advanced access.
     */
    private final JsonNode sourceNode;

    public RailincRecord(Map<String, String> fields, JsonNode sourceNode) {
        this.fields = new LinkedHashMap<>(fields);
        this.sourceNode = sourceNode;
    }

    /**
     * Get field value by name (from schema).
     */
    public String get(String fieldName) {
        return fields.getOrDefault(fieldName, "");
    }

    /**
     * Get field value with default.
     */
    public String get(String fieldName, String defaultValue) {
        String value = fields.get(fieldName);
        return (value != null && !value.isEmpty()) ? value : defaultValue;
    }

    /**
     * Check if field exists in the record.
     */
    public boolean hasField(String fieldName) {
        return fields.containsKey(fieldName);
    }

    /**
     * Check if field exists and has non-empty value.
     */
    public boolean hasValue(String fieldName) {
        String value = fields.get(fieldName);
        return value != null && !value.isEmpty();
    }

    /**
     * Get all field names.
     */
    public java.util.Set<String> fieldNames() {
        return fields.keySet();
    }

    /**
     * Create from JsonNode (parsed by FixedWidthToJsonConverter).
     */
    public static RailincRecord fromJsonNode(JsonNode node) {
        Map<String, String> fields = new LinkedHashMap<>();

        if (node != null && node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> it = node.fields();
            while (it.hasNext()) {
                Map.Entry<String, JsonNode> entry = it.next();
                String value = entry.getValue().isNull() ? "" : entry.getValue().asText();
                fields.put(entry.getKey(), value);
            }
        }

        return new RailincRecord(fields, node);
    }
}
