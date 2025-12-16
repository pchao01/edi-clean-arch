package com.example.edicleanarch.common.mapping;


import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Field mapping configuration for transforming JsonNode to target record.
 */
@Data
public class FieldMapping {
    private String name;              // Target column name
    private String source;            // Source path (e.g., "B3.02", "context.partnerId")
    private String type;              // STRING, INTEGER, DECIMAL, DATE, DATETIME, TIMESTAMP
    private String format;            // Date format pattern
    private boolean required;
    private Integer maxLength;

    // Transform configuration
    private String transform;         // DIRECT, CONCAT, LOOKUP, BUILD_DATETIME, CONSTANT, etc.
    private String value;             // For CONSTANT transform
    private String concatWith;        // For simple CONCAT
    private List<String> concatFields; // For multi-field CONCAT
    private Map<String, String> sourceFields; // For BUILD_DATETIME

    // Lookup configuration
    private String lookupTable;
    private String lookupKeyExpr;     // Expression like "RRDC_${eventTypeCode}_A"
    private String lookupColumn;

    // Conditional
    private String condition;
    private String trueValue;
    private String falseValue;
}
