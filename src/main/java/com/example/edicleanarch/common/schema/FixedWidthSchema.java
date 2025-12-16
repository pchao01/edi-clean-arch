package com.example.edicleanarch.common.schema;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * Schema definition for fixed-width file parsing.
 */
@Data
public class FixedWidthSchema {
    private String name;
    private String version;
    private List<FieldDefinition> headerFields = new ArrayList<>();
    private List<FieldDefinition> dataFields = new ArrayList<>();
    private List<FieldDefinition> trailerFields = new ArrayList<>();

    // Custom setters to ensure mutable lists
    public void setHeaderFields(List<FieldDefinition> headerFields) {
        this.headerFields = headerFields != null ? new ArrayList<>(headerFields) : new ArrayList<>();
    }

    public void setDataFields(List<FieldDefinition> dataFields) {
        this.dataFields = dataFields != null ? new ArrayList<>(dataFields) : new ArrayList<>();
    }

    public void setTrailerFields(List<FieldDefinition> trailerFields) {
        this.trailerFields = trailerFields != null ? new ArrayList<>(trailerFields) : new ArrayList<>();
    }
}
