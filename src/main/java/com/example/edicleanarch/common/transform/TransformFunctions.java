package com.example.edicleanarch.common.transform;


import com.example.edicleanarch.common.mapping.FieldMapping;
import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Registry of transform functions for config-driven mapping.
 */
@Component
public class TransformFunctions {

    private final Map<String, TransformFunction> functions = new HashMap<>();

    public TransformFunctions() {
        initFunctions();
    }

    public TransformFunction get(String name) {
        return functions.get(name);
    }

    private void initFunctions() {
        // DIRECT - Direct value extraction (default)
        functions.put("DIRECT", ctx -> ctx.getStringValue(ctx.getField().getSource()));

        // CONSTANT - Return constant value
        functions.put("CONSTANT", ctx -> ctx.getField().getValue());

        // CURRENT_TIMESTAMP - Current timestamp
        functions.put("CURRENT_TIMESTAMP", ctx -> LocalDateTime.now());

        // CONCAT - Concatenate fields
        functions.put("CONCAT", ctx -> {
            StringBuilder sb = new StringBuilder();
            String baseValue = ctx.getStringValue(ctx.getField().getSource());
            sb.append(baseValue != null ? baseValue : "");

            // Simple concat with concatWith
            if (ctx.getField().getConcatWith() != null) {
                String val2 = ctx.getStringValue(ctx.getField().getConcatWith());
                sb.append(val2 != null ? val2 : "");
            }

            // Multi-field concat
            List<String> concatFields = ctx.getField().getConcatFields();
            if (concatFields != null) {
                for (String field : concatFields) {
                    String val = ctx.getStringValue(field);
                    sb.append(val != null ? val : "");
                }
            }

            return sb.toString().trim();
        });

        // BUILD_DATETIME - Build datetime from component fields
        functions.put("BUILD_DATETIME", ctx -> {
            Map<String, String> sourceFields = ctx.getField().getSourceFields();
            if (sourceFields == null) return null;

            StringBuilder dateStr = new StringBuilder();

            appendIfPresent(dateStr, ctx, sourceFields.get("century"));
            appendIfPresent(dateStr, ctx, sourceFields.get("year"));
            appendIfPresent(dateStr, ctx, sourceFields.get("month"));
            appendIfPresent(dateStr, ctx, sourceFields.get("day"));
            appendIfPresent(dateStr, ctx, sourceFields.get("hour"));

            String minute = sourceFields.get("minute");
            if (minute != null) {
                String minValue = ctx.getStringValue(minute);
                dateStr.append(minValue != null ? minValue : "00");
            }

            String format = ctx.getField().getFormat();
            if (format == null) format = "yyyyMMddHHmm";

            try {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format);
                return LocalDateTime.parse(dateStr.toString(), formatter);
            } catch (Exception e) {
                return null;
            }
        });

        // DIVIDE_100 - Divide by 100 for implicit decimal
        functions.put("DIVIDE_100", ctx -> {
            String value = ctx.getStringValue(ctx.getField().getSource());
            if (value == null || value.isEmpty()) return null;
            try {
                return new BigDecimal(value).divide(BigDecimal.valueOf(100));
            } catch (Exception e) {
                return null;
            }
        });

        // TRIM_OR_NULL - Trim and return null if empty
        functions.put("TRIM_OR_NULL", ctx -> {
            String value = ctx.getStringValue(ctx.getField().getSource());
            if (value == null) return null;
            value = value.trim();
            return value.isEmpty() ? null : value;
        });

        // UPPERCASE - Convert to uppercase
        functions.put("UPPERCASE", ctx -> {
            String value = ctx.getStringValue(ctx.getField().getSource());
            return value != null ? value.toUpperCase() : null;
        });

        // LOOKUP - Database lookup (supports single-column or multi-column condition with fallback)
        functions.put("LOOKUP", ctx -> {
            if (ctx.getLookupService() == null) return null;

            String lookupCondition = ctx.getField().getLookupCondition();
            Object result = null;

            if (lookupCondition != null && !lookupCondition.isEmpty()) {
                // Multi-column lookup using WHERE condition
                String resolvedCondition = resolveExpression(lookupCondition, ctx);
                result = ctx.getLookupService().lookupWithCondition(
                        ctx.getField().getLookupTable(),
                        resolvedCondition,
                        ctx.getField().getLookupColumn()
                );

                // Try fallback condition if primary lookup returns null
                if (result == null && ctx.getField().getLookupFallbackCondition() != null) {
                    String fallbackCondition = resolveExpression(ctx.getField().getLookupFallbackCondition(), ctx);
                    result = ctx.getLookupService().lookupWithCondition(
                            ctx.getField().getLookupTable(),
                            fallbackCondition,
                            ctx.getField().getLookupColumn()
                    );
                }
            } else {
                // Single-column lookup using key expression
                String keyExpr = ctx.getField().getLookupKeyExpr();
                String resolvedKey = resolveExpression(keyExpr, ctx);
                result = ctx.getLookupService().lookup(
                        ctx.getField().getLookupTable(),
                        ctx.getField().getLookupKeyColumn(),
                        resolvedKey,
                        ctx.getField().getLookupColumn()
                );
            }
            return result;
        });

        // QUALIFIED_SEGMENT - Extract from qualified segment like N9[01=BM].02
        // Uses transaction (not loop record) because N9, B4 etc. are at transaction level
        functions.put("QUALIFIED_SEGMENT", ctx -> {
            String source = ctx.getField().getSource();
            // Use transaction for X12 segments like N9, B4; fall back to record if transaction is null
            JsonNode searchNode = ctx.getTransaction() != null ? ctx.getTransaction() : ctx.getRecord();
            return extractQualifiedValue(source, searchNode, ctx.getLoopIndex());
        });

        // COALESCE - Return first non-empty value from multiple sources
        functions.put("COALESCE", ctx -> {
            List<FieldMapping.SourceConfig> sources = ctx.getField().getSources();
            if (sources == null || sources.isEmpty()) return null;

            JsonNode searchNode = ctx.getTransaction() != null ? ctx.getTransaction() : ctx.getRecord();

            for (FieldMapping.SourceConfig sourceConfig : sources) {
                Object value = null;

                if (sourceConfig.getConcatFields() != null && !sourceConfig.getConcatFields().isEmpty()) {
                    // Concat multiple fields
                    StringBuilder sb = new StringBuilder();
                    for (String field : sourceConfig.getConcatFields()) {
                        String val = ctx.getStringValue(field);
                        if (val != null) sb.append(val);
                    }
                    value = sb.length() > 0 ? sb.toString() : null;
                } else if (sourceConfig.getSource() != null) {
                    // Apply transform if specified
                    if ("QUALIFIED_SEGMENT".equals(sourceConfig.getTransform())) {
                        value = extractQualifiedValue(sourceConfig.getSource(), searchNode, ctx.getLoopIndex());
                    } else {
                        value = ctx.getStringValue(sourceConfig.getSource());
                    }
                }

                if (value != null && !value.toString().isEmpty()) {
                    return value;
                }
            }
            return null;
        });

        // BOOKNO_FLAG - "" if N9_BM exists, "X" if only N9_BN exists
        functions.put("BOOKNO_FLAG", ctx -> {
            JsonNode searchNode = ctx.getTransaction() != null ? ctx.getTransaction() : ctx.getRecord();
            Object bmValue = extractQualifiedValue("N9[01=BM].02", searchNode, ctx.getLoopIndex());
            if (bmValue != null && !bmValue.toString().isEmpty()) {
                return "";  // N9_BM exists
            }
            Object bnValue = extractQualifiedValue("N9[01=BN].02", searchNode, ctx.getLoopIndex());
            if (bnValue != null && !bnValue.toString().isEmpty()) {
                return "X";  // Only N9_BN exists
            }
            return null;  // Neither exists
        });

        // ID_MAP_FLAG - From R4.01: "D" if "1", "T" if "Y" or null, else R4.01
        functions.put("ID_MAP_FLAG", ctx -> {
            String r401 = ctx.getStringValue(ctx.getField().getSource());
            if (r401 == null || r401.isEmpty()) {
                return "T";
            }
            if ("1".equals(r401)) {
                return "D";
            }
            if ("Y".equals(r401)) {
                return "T";
            }
            return r401;
        });
    }

    private void appendIfPresent(StringBuilder sb, TransformContext ctx, String field) {
        if (field != null) {
            String val = ctx.getStringValue(field);
            sb.append(val != null ? val : "");
        }
    }

    /**
     * Resolve ${path} expressions in string.
     * Supports dot-notation paths like ${envelope.ISA.06}, ${B4.03}, ${context.fileName}
     */
    private String resolveExpression(String expr, TransformContext ctx) {
        if (expr == null) return null;

        // Match ${...} including dot-notation paths
        Pattern pattern = Pattern.compile("\\$\\{([^}]+)\\}");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fieldPath = matcher.group(1);
            String value = ctx.getStringValue(fieldPath);
            matcher.appendReplacement(result, Matcher.quoteReplacement(value != null ? value : ""));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Extract value from qualified segment path like N1[01=SH].02
     */
    private Object extractQualifiedValue(String sourcePath, JsonNode transaction, int loopIndex) {
        Pattern pattern = Pattern.compile("(\\w+)\\[([^\\]]+)\\]\\.(\\d+)");
        Matcher matcher = pattern.matcher(sourcePath);

        if (!matcher.matches()) return null;

        String segmentId = matcher.group(1);
        String qualifier = matcher.group(2);
        String position = matcher.group(3);

        JsonNode segments = transaction.get(segmentId);
        if (segments == null) return null;

        // Handle _index for loop correlation
        if (qualifier.equals("_index")) {
            if (segments.isArray() && loopIndex < segments.size()) {
                JsonNode posNode = segments.get(loopIndex).get(position);
                return posNode != null ? posNode.asText() : null;
            }
            return null;
        }

        // Handle qualifier match: 01=SH
        String[] parts = qualifier.split("=");
        if (parts.length != 2) return null;

        String qualPos = parts[0];
        String qualValue = parts[1];

        if (segments.isArray()) {
            for (JsonNode segment : segments) {
                JsonNode qualNode = segment.get(qualPos);
                if (qualNode != null && qualValue.equals(qualNode.asText())) {
                    JsonNode posNode = segment.get(position);
                    return posNode != null ? posNode.asText() : null;
                }
            }
        } else {
            JsonNode qualNode = segments.get(qualPos);
            if (qualNode != null && qualValue.equals(qualNode.asText())) {
                JsonNode posNode = segments.get(position);
                return posNode != null ? posNode.asText() : null;
            }
        }

        return null;
    }
}
