package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.common.parser.FixedWidthToJsonConverter;
import com.example.edicleanarch.common.schema.FixedWidthSchema;
import com.example.edicleanarch.railinc.domain.model.ContainerEvent;
import com.example.edicleanarch.railinc.domain.model.RailincFile;
import com.example.edicleanarch.railinc.domain.model.RailincParseResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain Service: Railinc File Parser
 * Parses fixed-width Railinc CLM files into domain objects.
 *
 * Uses schema configuration from railinc-schema.yml for field positions.
 */

/**
 * Domain Service: Railinc File Parser
 *
 * Parses fixed-width Railinc CLM files into dynamic records.
 * Field positions come from schema YAML - NO hardcoded positions.
 * Output is fully dynamic - new fields can be added without code changes.
 *
 * Flow:
 * 1. Load schema from YAML (field positions)
 * 2. Parse fixed-width content to JsonNode using schema
 * 3. Convert JsonNode to RailincParseResult (dynamic records)
 *
 * Adding a new field:
 * 1. Add field definition to railinc-schema.yml (name, start, end)
 * 2. Add field mapping to railinc-mapping.yml (source -> target)
 * 3. NO Java code changes needed!
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RailincFileParser {

    private final FixedWidthToJsonConverter jsonConverter;
    private final RailincSchemaLoader schemaLoader;

    /**
     * Parse Railinc CLM content using schema configuration.
     *
     * @param content Raw CLM file content
     * @return RailincParseResult with dynamic records
     */
    public RailincParseResult parse(String content) {
        // 1. Load schema from YAML
        FixedWidthSchema schema = schemaLoader.loadSchema();
        log.debug("Loaded schema: {} v{}", schema.getName(), schema.getVersion());

        // 2. Convert to JsonNode using schema positions
        JsonNode jsonNode = jsonConverter.convert(content, schema);

        // 3. Convert to dynamic result
        RailincParseResult result = RailincParseResult.fromJsonNode(jsonNode);

        log.debug("Parsed {} records from CLM file", result.getRecordCount());
        return result;
    }

    /**
     * Parse with specific schema (for partner overrides).
     *
     * @param content Raw CLM file content
     * @param schema Custom schema with partner-specific positions
     * @return RailincParseResult with dynamic records
     */
    public RailincParseResult parse(String content, FixedWidthSchema schema) {
        JsonNode jsonNode = jsonConverter.convert(content, schema);
        return RailincParseResult.fromJsonNode(jsonNode);
    }

    /**
     * Get current schema (for inspection/debugging).
     */
    public FixedWidthSchema getSchema() {
        return schemaLoader.loadSchema();
    }

    /**
     * Reload schema (for hot-reload without restart).
     */
    public void reloadSchema() {
        schemaLoader.reloadSchema();
        log.info("Schema reloaded");
    }
}
