package com.example.edicleanarch.railinc.domain.service;


import com.example.edicleanarch.common.mapping.EdiMappingEngine;
import com.example.edicleanarch.common.mapping.MappingConfig;
import com.example.edicleanarch.common.mapping.MappingResult;
import com.example.edicleanarch.common.mapping.ProcessingContext;
import com.example.edicleanarch.railinc.domain.model.RailincParseResult;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * Railinc Mapping Service - Fully Config-Driven
 *
 * Transforms parsed Railinc CLM data to database records using:
 * 1. railinc-schema.yml - defines field positions (parsing)
 * 2. railinc-mapping.yml - defines field transformations (mapping)
 *
 * NO JAVA CODE CHANGES NEEDED when adding new fields:
 * - Add field to schema YAML (position)
 * - Add field mapping to mapping YAML (transformation)
 * - Done!
 *
 * Example: Adding a new field "specialCode" at positions 71-72:
 *
 * 1. In railinc-schema.yml:
 *    - name: specialCode
 *      start: 71
 *      end: 73
 *
 * 2. In railinc-mapping.yml:
 *    - name: SPECIAL_CODE
 *      source: specialCode
 *      type: STRING
 *
 * No Java changes required!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RailincMappingService {

    private final RailincFileParser fileParser;
    private final EdiMappingEngine mappingEngine;
    private final RailincMappingConfigLoader mappingConfigLoader;

    /**
     * Process Railinc CLM file end-to-end.
     *
     * @param content   Raw CLM file content
     * @param fileName  File name for context
     * @param partnerId Partner ID for overrides (optional)
     * @return MappingResult with database records
     */
    public MappingResult process(String content, String fileName, String partnerId) {
        // 1. Parse fixed-width content using schema (positions from YAML)
        RailincParseResult parseResult = fileParser.parse(content);

        // 2. Validate parse result
        if (!parseResult.isRecordCountValid()) {
            return MappingResult.failed(List.of(
                    String.format("Record count mismatch: actual=%d, expected=%d",
                            parseResult.getRecordCount(), parseResult.getTrailerRecordCount())
            ));
        }

        // 3. Load mapping config (transformations from YAML)
        MappingConfig mappingConfig = mappingConfigLoader.loadConfig();

        // 4. Create processing context
        ProcessingContext context = ProcessingContext.builder()
                .fileName(fileName)
                .partnerId(partnerId)
                .timestamp(java.time.LocalDateTime.now())
                .build();
        context.setValue("ediType", "RAILINC");

        // 5. Transform to database records using mapping engine
        JsonNode sourceJson = parseResult.getSourceJson();
        MappingResult result = mappingEngine.transform(sourceJson, mappingConfig, partnerId, context);

        log.info("Processed {} records from {}", parseResult.getRecordCount(), fileName);
        return result;
    }

    /**
     * Process with default partner.
     */
    public MappingResult process(String content, String fileName) {
        return process(content, fileName, null);
    }

    /**
     * Get database records by table name.
     */
    public List<Map<String, Object>> getRecordsByTable(MappingResult result, String tableName) {
        return result.getRecords(tableName);
    }
}
