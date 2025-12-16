package com.example.edicleanarch.x12.edi315.domain.service;

import com.example.edicleanarch.common.mapping.EdiMappingEngine;
import com.example.edicleanarch.common.mapping.MappingConfig;
import com.example.edicleanarch.common.mapping.MappingResult;
import com.example.edicleanarch.common.mapping.ProcessingContext;
import com.example.edicleanarch.common.parser.X12ToJsonConverter;
import com.example.edicleanarch.x12.edi315.domain.service.inbound.Edi315MappingConfigLoader;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * EDI 315 Mapping Service - Fully Config-Driven
 *
 * Transforms parsed EDI 315 X12 data to database records using:
 * 1. X12ToJsonConverter - parses raw X12 to JsonNode
 * 2. edi315-mapping.yml - defines field transformations
 *
 * NO JAVA CODE CHANGES NEEDED when adding new fields:
 * - Add field mapping to YAML
 * - Done!
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class Edi315MappingService {

    private final X12ToJsonConverter x12Converter;
    private final EdiMappingEngine mappingEngine;
    private final Edi315MappingConfigLoader mappingConfigLoader;

    /**
     * Process EDI 315 file end-to-end.
     *
     * @param content   Raw EDI 315 X12 content
     * @param fileName  File name for context
     * @param partnerId Partner ID for overrides (optional)
     * @return MappingResult with database records
     */
    public MappingResult process(String content, String fileName, String partnerId) {
        // 1. Parse X12 content to JsonNode
        JsonNode ediJson = x12Converter.convert(content);

        // 2. Load mapping config
        MappingConfig mappingConfig = mappingConfigLoader.loadConfig();

        // 3. Create processing context
        ProcessingContext context = ProcessingContext.builder()
                .fileName(fileName)
                .partnerId(partnerId)
                .ediType("EDI_315")
                .timestamp(LocalDateTime.now())
                .build();

        // 4. Transform to database records using mapping engine
        MappingResult result = mappingEngine.transform(ediJson, mappingConfig, partnerId, context);

        log.info("Processed EDI 315 file {} - {} records", fileName, result.getTotalRecords());
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
