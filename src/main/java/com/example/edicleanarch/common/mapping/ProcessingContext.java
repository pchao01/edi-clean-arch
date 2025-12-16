package com.example.edicleanarch.common.mapping;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
/**
 * Processing context passed through the mapping engine.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessingContext {
    private String partnerId;
    private String fileName;
    private String ediType;
    private LocalDateTime timestamp;
    private Map<String, Object> additionalContext;

    public Object getValue(String key) {
        return switch (key) {
            case "partnerId" -> partnerId;
            case "fileName" -> fileName;
            case "ediType" -> ediType;
            case "timestamp" -> timestamp;
            default -> additionalContext != null ? additionalContext.get(key) : null;
        };
    }

    public void setValue(String key, Object value) {
        if (additionalContext == null) {
            additionalContext = new HashMap<>();
        }
        additionalContext.put(key, value);
    }
}
