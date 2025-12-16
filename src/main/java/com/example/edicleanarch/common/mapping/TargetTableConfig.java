package com.example.edicleanarch.common.mapping;


import lombok.Data;

import java.util.List;

/**
 * Target table configuration for mapping.
 */
@Data
public class TargetTableConfig {
    private String table;             // Target table name
    private String type;              // HEADER or DETAIL
    private String loopPath;          // For DETAIL: path to array (e.g., "N7", "records")
    private List<String> parentKeys;  // Keys inherited from parent
    private String condition;         // Optional condition for this target
    private String description;
    private List<FieldMapping> fields;
}
