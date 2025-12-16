package com.example.edicleanarch.railinc.domain.service.inbound;


import com.example.edicleanarch.common.annotation.UseCase;
import com.example.edicleanarch.common.mapping.EdiMappingEngine;
import com.example.edicleanarch.common.mapping.MappingConfig;
import com.example.edicleanarch.common.mapping.MappingResult;
import com.example.edicleanarch.common.mapping.ProcessingContext;
import com.example.edicleanarch.common.model.ProcessingResult;
import com.example.edicleanarch.common.parser.FixedWidthToJsonConverter;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.common.schema.FixedWidthSchema;
import com.example.edicleanarch.railinc.port.out.SaveRailincEventsPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.transaction.annotation.Transactional;

import java.io.InputStream;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Domain Service: Process Railinc CLM File using config-driven approach.
 *
 * Flow:
 * 1. Load fixed-width schema configuration
 * 2. Convert fixed-width content to JsonNode (intermediate format)
 * 3. Load mapping configuration (YAML or DB)
 * 4. Transform JsonNode to database records using EdiMappingEngine
 * 5. Save records via outbound adapter
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class ProcessRailincService implements ProcessEdiFileUseCase<ProcessRailincCommand> {

    private static final String MESSAGE_TYPE = "RAILINC";

    private final FixedWidthToJsonConverter fixedWidthConverter;
    private final EdiMappingEngine mappingEngine;
    private final SaveRailincEventsPort saveEventsPort;

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final Map<String, FixedWidthSchema> schemaCache = new ConcurrentHashMap<>();
    private final Map<String, MappingConfig> configCache = new ConcurrentHashMap<>();

    @Override
    public ProcessingResult processFile(ProcessRailincCommand command) {
        log.info("Processing Railinc file: {} for partner: {}",
                command.fileName(), command.partnerId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Load fixed-width schema
            FixedWidthSchema schema = loadSchema(command.partnerId());

            // 2. Convert fixed-width to JsonNode
            JsonNode railincJson = fixedWidthConverter.convert(command.content(), schema);
            int recordCount = railincJson.get("_metadata").get("recordCount").asInt();
            log.debug("Converted Railinc to JsonNode: {} records", recordCount);

            // 3. Load mapping configuration
            MappingConfig config = loadMappingConfig(command.partnerId());

            // 4. Create processing context
            ProcessingContext context = ProcessingContext.builder()
                    .partnerId(command.partnerId())
                    .fileName(command.fileName())
                    .timestamp(LocalDateTime.now())
                    .build();

            // 5. Transform using mapping engine
            MappingResult mappingResult = mappingEngine.transform(
                    railincJson, config, command.partnerId(), context);

            if (!mappingResult.isSuccess()) {
                return ProcessingResult.validationFailed(MESSAGE_TYPE, command.fileName(),
                        command.partnerId(), mappingResult.getErrors(),
                        System.currentTimeMillis() - startTime);
            }

            // 6. Save to database
            List<Map<String, Object>> cdbEvents = mappingResult.getRecords("CDB_EVENT");
            Map<String, Integer> insertCounts = saveEventsPort.saveRecords(cdbEvents, command.fileName());

            log.info("Processed Railinc file {}: {} records saved",
                    command.fileName(), mappingResult.getTotalRecords());

            return ProcessingResult.success(MESSAGE_TYPE, command.fileName(),
                    command.partnerId(), mappingResult.getTotalRecords(), insertCounts,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Error processing Railinc file: {}", command.fileName(), e);
            return ProcessingResult.error(MESSAGE_TYPE, command.fileName(),
                    command.partnerId(), e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }

    /**
     * Load fixed-width schema (with partner override support).
     */
    private FixedWidthSchema loadSchema(String partnerId) {
        String cacheKey = "RAILINC_CLM_" + (partnerId != null ? partnerId : "DEFAULT");

        return schemaCache.computeIfAbsent(cacheKey, k -> {
            try {
                ClassPathResource resource = new ClassPathResource("edi/config/mappings/inbound/railinc-schema.yml");
                try (InputStream is = resource.getInputStream()) {
                    return yamlMapper.readValue(is, FixedWidthSchema.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load Railinc schema", e);
            }
        });
    }

    /**
     * Load mapping configuration (with partner override support).
     */
    private MappingConfig loadMappingConfig(String partnerId) {
        String cacheKey = "RAILINC_" + (partnerId != null ? partnerId : "DEFAULT");

        return configCache.computeIfAbsent(cacheKey, k -> {
            try {
                ClassPathResource resource = new ClassPathResource("edi/config/mappings/inbound/railinc-mapping.yml");
                try (InputStream is = resource.getInputStream()) {
                    return yamlMapper.readValue(is, MappingConfig.class);
                }
            } catch (Exception e) {
                throw new RuntimeException("Failed to load Railinc mapping config", e);
            }
        });
    }
}
