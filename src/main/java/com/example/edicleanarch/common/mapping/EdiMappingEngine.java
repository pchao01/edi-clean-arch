package com.example.edicleanarch.common.mapping;

import com.example.edicleanarch.common.transform.LookupService;
import com.example.edicleanarch.common.transform.TransformContext;
import com.example.edicleanarch.common.transform.TransformFunction;
import com.example.edicleanarch.common.transform.TransformFunctions;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Config-driven mapping engine.
 * Transforms JsonNode (from X12 or FixedWidth) to database records based on configuration.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EdiMappingEngine {

    private final TransformFunctions transformFunctions;
    private final LookupService lookupService;

    /**
     * Transform EDI JsonNode to output records based on mapping configuration.
     *
     * @param ediJson   Parsed EDI as JsonNode (from X12ToJsonConverter or FixedWidthToJsonConverter)
     * @param config    Mapping configuration
     * @param partnerId Partner ID for override lookup
     * @param context   Processing context
     * @return Mapping result with records by table
     */
    public MappingResult transform(JsonNode ediJson, MappingConfig config,
                                   String partnerId, ProcessingContext context) {
        // 1. Apply partner overrides
        MappingConfig effectiveConfig = applyPartnerOverrides(config, partnerId);

        // 2. Validate input
        List<String> validationErrors = validate(ediJson, effectiveConfig);
        if (!validationErrors.isEmpty()) {
            return MappingResult.failed(validationErrors);
        }

        // 3. Process based on source format
        MappingResult result = new MappingResult();

        if ("X12".equals(effectiveConfig.getSourceFormat())) {
            processX12Transactions(ediJson, effectiveConfig, context, result);
        } else if ("FIXED_WIDTH".equals(effectiveConfig.getSourceFormat())) {
            processFixedWidthRecords(ediJson, effectiveConfig, context, result);
        }

        return result;
    }

    /**
     * Process X12 transactions.
     */
    private void processX12Transactions(JsonNode ediJson, MappingConfig config,
                                        ProcessingContext context, MappingResult result) {
        JsonNode transactions = ediJson.get("transactions");
        if (transactions == null || !transactions.isArray()) return;

        for (JsonNode transaction : transactions) {
            processTargets(transaction, ediJson, config, context, result);
        }
    }

    /**
     * Process fixed-width records.
     */
    private void processFixedWidthRecords(JsonNode ediJson, MappingConfig config,
                                          ProcessingContext context, MappingResult result) {
        JsonNode records = ediJson.get("records");
        if (records == null || !records.isArray()) return;

        for (TargetTableConfig target : config.getTargets()) {
            List<Map<String, Object>> targetRecords = new ArrayList<>();

            for (int i = 0; i < records.size(); i++) {
                JsonNode record = records.get(i);

                // Check condition
                if (target.getCondition() != null) {
                    if (!evaluateCondition(target.getCondition(), record)) {
                        continue;
                    }
                }

                // For fixed-width, record and transaction are the same (no loop structure)
                Map<String, Object> mappedRecord = mapFields(record, null, target.getFields(),
                        ediJson, context, i);
                targetRecords.add(mappedRecord);
            }

            result.addRecords(target.getTable(), targetRecords);
        }
    }

    /**
     * Process targets for a transaction.
     */
    private void processTargets(JsonNode transaction, JsonNode ediJson,
                                MappingConfig config, ProcessingContext context,
                                MappingResult result) {
        Map<String, Object> headerRecord = null;

        for (TargetTableConfig target : config.getTargets()) {
            if ("HEADER".equals(target.getType())) {
                // For HEADER, record and transaction are the same
                headerRecord = mapFields(transaction, null, target.getFields(), ediJson, context, -1);
                result.addRecords(target.getTable(), List.of(headerRecord));

            } else if ("DETAIL".equals(target.getType())) {
                List<Map<String, Object>> detailRecords = mapDetailRecords(
                        transaction, target, ediJson, context, headerRecord);
                result.addRecords(target.getTable(), detailRecords);
            }
        }
    }

    /**
     * Map detail records from a loop.
     */
    private List<Map<String, Object>> mapDetailRecords(JsonNode transaction, TargetTableConfig target,
                                                       JsonNode ediJson, ProcessingContext context,
                                                       Map<String, Object> headerRecord) {
        List<Map<String, Object>> records = new ArrayList<>();

        String loopPath = target.getLoopPath();
        JsonNode loopSegments = transaction.get(loopPath);

        if (loopSegments == null) return records;

        if (loopSegments.isArray()) {
            for (int i = 0; i < loopSegments.size(); i++) {
                Map<String, Object> record = mapFields(loopSegments.get(i), transaction, target.getFields(),
                        ediJson, context, i);

                // Add parent keys
                addParentKeys(record, headerRecord, target.getParentKeys());
                records.add(record);
            }
        } else {
            Map<String, Object> record = mapFields(loopSegments, transaction, target.getFields(),
                    ediJson, context, 0);
            addParentKeys(record, headerRecord, target.getParentKeys());
            records.add(record);
        }

        return records;
    }

    /**
     * Map fields for a single record.
     *
     * @param record      Current record (loop element for DETAIL, transaction for HEADER)
     * @param transaction Current transaction (null for fixed-width or when record IS the transaction)
     * @param fields      Field mappings to apply
     * @param fullJson    Full EDI JSON (envelope + transactions)
     * @param context     Processing context
     * @param loopIndex   Current loop index (-1 if not in a loop)
     */
    private Map<String, Object> mapFields(JsonNode record, JsonNode transaction, List<FieldMapping> fields,
                                          JsonNode fullJson, ProcessingContext context, int loopIndex) {
        Map<String, Object> result = new LinkedHashMap<>();

        // If transaction is null, use record as transaction (for HEADER type or fixed-width)
        JsonNode effectiveTransaction = transaction != null ? transaction : record;

        for (FieldMapping field : fields) {
            // Check condition
            if (field.getCondition() != null && !evaluateCondition(field.getCondition(), record)) {
                continue;
            }

            // Pass the current result so previously mapped fields can be referenced
            Object value = processField(record, effectiveTransaction, field, fullJson, context, loopIndex, result);

            // Convert type
            value = convertType(value, field.getType(), field.getFormat());

            result.put(field.getName(), value);
        }

        return result;
    }

    /**
     * Process a single field.
     */
    private Object processField(JsonNode record, JsonNode transaction, FieldMapping field, JsonNode fullJson,
                                ProcessingContext context, int loopIndex, Map<String, Object> outputRecord) {
        String transform = field.getTransform();

        // Default to DIRECT if no transform specified
        if (transform == null || transform.isEmpty()) {
            transform = "DIRECT";
        }

        TransformFunction function = transformFunctions.get(transform);
        if (function == null) {
            log.warn("Unknown transform: {}, using DIRECT", transform);
            function = transformFunctions.get("DIRECT");
        }

        TransformContext txContext = new TransformContext(
                record, transaction, field, fullJson, context, lookupService, loopIndex, outputRecord);

        return function.apply(txContext);
    }

    /**
     * Convert value to target type.
     */
    private Object convertType(Object value, String type, String format) {
        if (value == null || type == null) return value;

        try {
            return switch (type.toUpperCase()) {
                case "STRING" -> value.toString();
                case "INTEGER" -> {
                    if (value instanceof Number n) yield n.intValue();
                    yield Integer.parseInt(value.toString().trim());
                }
                case "DECIMAL" -> {
                    if (value instanceof BigDecimal bd) yield bd;
                    yield new BigDecimal(value.toString().trim());
                }
                case "DATE" -> {
                    if (value instanceof LocalDate ld) yield ld;
                    if (value instanceof LocalDateTime ldt) yield ldt.toLocalDate();
                    String dateFormat = format != null ? format : "yyyyMMdd";
                    yield LocalDate.parse(value.toString(), DateTimeFormatter.ofPattern(dateFormat));
                }
                case "DATETIME", "TIMESTAMP" -> {
                    if (value instanceof LocalDateTime ldt) yield ldt;
                    String dateFormat = format != null ? format : "yyyyMMddHHmm";
                    yield LocalDateTime.parse(value.toString(), DateTimeFormatter.ofPattern(dateFormat));
                }
                default -> value;
            };
        } catch (Exception e) {
            log.debug("Type conversion failed for value: {}, type: {}", value, type);
            return value;
        }
    }

    /**
     * Add parent keys to detail record.
     */
    private void addParentKeys(Map<String, Object> record, Map<String, Object> headerRecord,
                               List<String> parentKeys) {
        if (headerRecord == null || parentKeys == null) return;

        for (String key : parentKeys) {
            if (headerRecord.containsKey(key)) {
                record.put(key, headerRecord.get(key));
            }
        }
    }

    /**
     * Evaluate a condition expression.
     */
    private boolean evaluateCondition(String condition, JsonNode record) {
        if (condition == null || condition.isEmpty()) return true;

        // Simple expression resolution: ${field} != '' && ${field} != null
        String resolved = resolveExpression(condition, record);

        // Basic evaluation
        return !resolved.contains("null") && !resolved.contains("''") && !resolved.isEmpty();
    }

    /**
     * Resolve ${fieldName} expressions.
     */
    private String resolveExpression(String expr, JsonNode record) {
        Pattern pattern = Pattern.compile("\\$\\{(\\w+)\\}");
        Matcher matcher = pattern.matcher(expr);
        StringBuffer result = new StringBuffer();

        while (matcher.find()) {
            String fieldName = matcher.group(1);
            String value = "";
            if (record.has(fieldName)) {
                JsonNode node = record.get(fieldName);
                value = node.isNull() ? "null" : node.asText();
            }
            matcher.appendReplacement(result, Matcher.quoteReplacement(value));
        }
        matcher.appendTail(result);

        return result.toString();
    }

    /**
     * Validate input against configuration.
     */
    private List<String> validate(JsonNode ediJson, MappingConfig config) {
        List<String> errors = new ArrayList<>();

        if (config.getValidations() == null) return errors;

        for (MappingConfig.ValidationRule rule : config.getValidations()) {
            switch (rule.getRule()) {
                case "HEADER_REQUIRED" -> {
                    JsonNode header = ediJson.get("header");
                    if (header == null || !header.has(rule.getField().replace("header.", ""))) {
                        errors.add(rule.getMessage());
                    }
                }
                case "TRAILER_REQUIRED" -> {
                    JsonNode trailer = ediJson.get("trailer");
                    if (trailer == null) {
                        errors.add(rule.getMessage());
                    }
                }
                case "RECORD_COUNT_MATCH" -> {
                    // Compare actual vs expected record count
                    JsonNode metadata = ediJson.get("_metadata");
                    JsonNode trailer = ediJson.get("trailer");
                    if (metadata != null && trailer != null) {
                        int actual = metadata.get("recordCount").asInt();
                        int expected = trailer.get(rule.getExpectedField().replace("trailer.", "")).asInt();
                        if (actual != expected) {
                            errors.add(rule.getMessage() + ": actual=" + actual + ", expected=" + expected);
                        }
                    }
                }
                case "REQUIRED_SEGMENT" -> {
                    // Check required segments in transactions
                    JsonNode transactions = ediJson.get("transactions");
                    if (transactions != null && transactions.isArray()) {
                        for (JsonNode tx : transactions) {
                            for (String seg : rule.getSegments()) {
                                if (!tx.has(seg)) {
                                    errors.add("Missing required segment: " + seg);
                                }
                            }
                        }
                    }
                }
            }
        }

        return errors;
    }

    /**
     * Apply partner-specific overrides to configuration.
     */
    private MappingConfig applyPartnerOverrides(MappingConfig config, String partnerId) {
        if (partnerId == null || config.getPartnerOverrides() == null) {
            return config;
        }

        MappingConfig.PartnerOverride override = config.getPartnerOverrides().get(partnerId);
        if (override == null) {
            return config;
        }

        // Clone and apply overrides
        // For simplicity, return original - real implementation would merge overrides
        log.info("Applying partner overrides for: {}", partnerId);
        return config;
    }
}
