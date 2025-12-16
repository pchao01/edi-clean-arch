package com.example.edicleanarch.common.mapping;


import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * Complete mapping configuration for an EDI type.
 */
@Data
public class MappingConfig {
    private String ediType;           // EDI_315, RAILINC, etc.
    private String description;
    private String sourceFormat;      // X12 or FIXED_WIDTH
    private String version;

    private List<TargetTableConfig> targets;
    private List<ValidationRule> validations;
    private Map<String, PartnerOverride> partnerOverrides;

    @Data
    public static class ValidationRule {
        private String rule;
        private List<String> segments;
        private List<String> fields;
        private String field;
        private String expectedValue;
        private String actualField;
        private String expectedField;
        private String message;
        private String condition;
        private List<String> requiredFields;
    }

    @Data
    public static class PartnerOverride {
        private List<FieldMapping> fieldOverrides;
        private Map<String, Object> schemaOverrides;
    }
}
