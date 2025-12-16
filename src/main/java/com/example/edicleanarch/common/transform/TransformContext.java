package com.example.edicleanarch.common.transform;


import com.example.edicleanarch.common.mapping.FieldMapping;
import com.example.edicleanarch.common.mapping.ProcessingContext;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.Data;

/**
 * Context for transform functions.
 */
@Data
public class TransformContext {
    private final JsonNode record;           // Current loop element (e.g., R4 element)
    private final JsonNode transaction;      // Current transaction (contains B4, N9, Q2, R4, etc.)
    private final FieldMapping field;
    private final JsonNode fullJson;
    private final ProcessingContext processingContext;
    private final LookupService lookupService;
    private final int loopIndex;

    public TransformContext(JsonNode record, JsonNode transaction, FieldMapping field, JsonNode fullJson,
                            ProcessingContext processingContext, LookupService lookupService, int loopIndex) {
        this.record = record;
        this.transaction = transaction;
        this.field = field;
        this.fullJson = fullJson;
        this.processingContext = processingContext;
        this.lookupService = lookupService;
        this.loopIndex = loopIndex;
    }

    /**
     * Get string value from record by field name.
     *
     * Supports:
     * - 'literal' - literal string value
     * - context.fileName - processing context value
     * - envelope.ISA.06 - envelope segment value
     * - header.field - header value (fixed-width)
     * - B4.07 - transaction segment value (X12)
     * - 03 - current loop element value (R4 element)
     */
    public String getStringValue(String fieldName) {
        if (fieldName == null) return null;

        // Literal value
        if (fieldName.startsWith("'") && fieldName.endsWith("'")) {
            return fieldName.substring(1, fieldName.length() - 1);
        }

        // Context value
        if (fieldName.startsWith("context.")) {
            Object val = processingContext.getValue(fieldName.substring(8));
            return val != null ? val.toString() : null;
        }

        // Header value (for fixed-width)
        if (fieldName.startsWith("header.") && fullJson.has("header")) {
            String headerField = fieldName.substring(7);
            return getJsonText(fullJson.get("header"), headerField);
        }

        // Envelope value (for X12)
        if (fieldName.startsWith("envelope.") && fullJson.has("envelope")) {
            String[] parts = fieldName.substring(9).split("\\.");
            JsonNode node = fullJson.get("envelope");
            for (String part : parts) {
                if (node == null) return null;
                node = node.get(part);
            }
            return node != null && !node.isNull() ? node.asText().trim() : null;
        }

        // Transaction segment value (e.g., B4.07, N9.01, Q2.13)
        // For X12, fields like "B4.07" should look in the transaction, not the loop element
        if (fieldName.contains(".") && transaction != null) {
            String[] parts = fieldName.split("\\.", 2);
            String segmentId = parts[0];
            String elementId = parts[1];

            JsonNode segment = transaction.get(segmentId);
            if (segment != null) {
                return getJsonText(segment, elementId);
            }
        }

        // Record value (loop element field, e.g., "03" for R4.03)
        return getJsonText(record, fieldName);
    }

    /**
     * Get text from JsonNode safely.
     */
    private String getJsonText(JsonNode node, String field) {
        if (node == null || !node.has(field)) return null;
        JsonNode value = node.get(field);
        if (value.isNull()) return null;
        String text = value.asText();
        return text.isEmpty() ? null : text.trim();
    }
}
