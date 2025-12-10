package com.example.edicleanarch.common.schema;

import lombok.Data;

import java.util.List;

/**
 * Schema definition for fixed-width file parsing.
 */
@Data
public class FixedWidthSchema {
    private String name;
    private String version;
    private List<FieldDefinition> headerFields;
    private List<FieldDefinition> dataFields;
    private List<FieldDefinition> trailerFields;
}
