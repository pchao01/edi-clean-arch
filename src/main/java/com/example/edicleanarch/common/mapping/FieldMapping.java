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
    private String transform;         // DIRECT, CONCAT, LOOKUP, BUILD_DATETIME, CONSTANT, COALESCE, etc.
    private String value;             // For CONSTANT transform
    private String concatWith;        // For simple CONCAT
    private List<String> concatFields; // For multi-field CONCAT
    private Map<String, String> sourceFields; // For BUILD_DATETIME
    private List<SourceConfig> sources; // For COALESCE - list of sources to try

    /**
     * Source configuration for COALESCE transform.
     */
    @Data
    public static class SourceConfig {
        private String source;           // Source path
        private String transform;        // Transform to apply (e.g., QUALIFIED_SEGMENT)
        private List<String> concatFields; // For concat-based source
    }

    // Lookup configuration
    private String lookupTable;
    private String lookupKeyColumn;   // Column to match the key against (e.g., "CODE", "SCAC_CD")
    private String lookupKeyExpr;     // Expression like "RRDC_${eventTypeCode}_A"
    private String lookupColumn;
    private String lookupCondition;   // Multi-column WHERE condition like "SCAC_CD = '${scac}' AND PRTNR_EVENT_CD = '${B4.03}'"
    private String lookupFallbackCondition; // Fallback WHERE condition if primary lookup returns null

    // Conditional
    private String condition;
    private String trueValue;
    private String falseValue;
}
