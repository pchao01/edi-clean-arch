package com.example.edicleanarch.common.mapping;


import lombok.Data;

import java.util.*;

/**
 * Result of mapping transformation.
 */
@Data
public class MappingResult {
    private boolean success = true;
    private List<String> errors = new ArrayList<>();
    private Map<String, List<Map<String, Object>>> recordsByTable = new LinkedHashMap<>();

    public static MappingResult failed(List<String> errors) {
        MappingResult result = new MappingResult();
        result.setSuccess(false);
        result.setErrors(errors);
        return result;
    }

    public void addRecords(String tableName, List<Map<String, Object>> records) {
        recordsByTable.computeIfAbsent(tableName, k -> new ArrayList<>()).addAll(records);
    }

    public List<Map<String, Object>> getRecords(String tableName) {
        return recordsByTable.getOrDefault(tableName, Collections.emptyList());
    }

    public int getTotalRecords() {
        return recordsByTable.values().stream().mapToInt(List::size).sum();
    }
}
