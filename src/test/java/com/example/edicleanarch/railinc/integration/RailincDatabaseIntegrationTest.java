package com.example.edicleanarch.railinc.integration;

import com.example.edicleanarch.common.mapping.EdiMappingEngine;
import com.example.edicleanarch.common.mapping.MappingConfig;
import com.example.edicleanarch.common.mapping.MappingResult;
import com.example.edicleanarch.common.mapping.ProcessingContext;
import com.example.edicleanarch.common.parser.FixedWidthToJsonConverter;
import com.example.edicleanarch.common.schema.FixedWidthSchema;
import com.example.edicleanarch.railinc.port.out.SaveRailincEventsPort;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test: Load OECGROUP_CLM.multiple_records.txt,
 * parse with schema, transform with mapping, insert to CDB_EVENT table.
 *
 * Prerequisites:
 * - Local SQL Server running on localhost
 * - Database: CDB
 * - Table: CDB_EVENT must exist
 * - User: sa / 1111
 */
@SpringBootTest
@ActiveProfiles("test")
@DisplayName("Railinc Database Integration Test")
class RailincDatabaseIntegrationTest {

    private static final String TEST_FILENAME = "OECGROUP_CLM.multiple_records.txt";

    @Autowired
    private FixedWidthToJsonConverter fixedWidthConverter;

    @Autowired
    private EdiMappingEngine mappingEngine;

    @Autowired
    private SaveRailincEventsPort saveEventsPort;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private ObjectMapper yamlMapper;
    private String testFileContent;
    private FixedWidthSchema schema;
    private MappingConfig mappingConfig;

    @BeforeEach
    void setUp() throws IOException {
        yamlMapper = new ObjectMapper(new YAMLFactory());

        // Load test file
        testFileContent = loadTestFile();

        // Load schema
        schema = loadSchema();

        // Load mapping config
        mappingConfig = loadMappingConfig();

        // Clean up test data before each test
        cleanupTestData();
    }

    /**
     * Delete test records from previous runs to avoid duplicate errors.
     */
    private void cleanupTestData() {
        try {
            int deleted = jdbcTemplate.update(
                    "DELETE FROM CDB_EVENT WHERE FILENAME = ?", TEST_FILENAME);
            if (deleted > 0) {
                System.out.println("Cleaned up " + deleted + " existing test records");
            }
        } catch (Exception e) {
            System.out.println("Cleanup skipped (table may not exist): " + e.getMessage());
        }
    }

    private String loadTestFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("railinc/OECGROUP_CLM.multiple_records.txt");
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private FixedWidthSchema loadSchema() throws IOException {
        ClassPathResource resource = new ClassPathResource("config/mappings/inbound/railinc-schema.yml");
        try (InputStream is = resource.getInputStream()) {
            return yamlMapper.readValue(is, FixedWidthSchema.class);
        }
    }

    private MappingConfig loadMappingConfig() throws IOException {
        ClassPathResource resource = new ClassPathResource("config/mappings/inbound/railinc-mapping.yml");
        try (InputStream is = resource.getInputStream()) {
            return yamlMapper.readValue(is, MappingConfig.class);
        }
    }

    @Test
    @DisplayName("Should parse CLM file and insert 11 records into CDB_EVENT")
    void shouldParseAndInsertRecordsToCdbEvent() {
        // 1. Parse fixed-width to JsonNode
        JsonNode railincJson = fixedWidthConverter.convert(testFileContent, schema);

        assertNotNull(railincJson);
        assertTrue(railincJson.has("records"));
        assertEquals(11, railincJson.get("_metadata").get("recordCount").asInt());

        System.out.println("Parsed " + railincJson.get("_metadata").get("recordCount").asInt() + " records from CLM file");

        // 2. Create processing context
        ProcessingContext context = ProcessingContext.builder()
                .partnerId("OECGROUP")
                .fileName("OECGROUP_CLM.multiple_records.txt")
                .timestamp(LocalDateTime.now())
                .build();

        // 3. Transform using mapping engine
        MappingResult mappingResult = mappingEngine.transform(railincJson, mappingConfig, "OECGROUP", context);

        assertTrue(mappingResult.isSuccess(), "Mapping should succeed. Errors: " + mappingResult.getErrors());

        // 4. Get CDB_EVENT records
        List<Map<String, Object>> cdbEvents = mappingResult.getRecords("CDB_EVENT");

        assertNotNull(cdbEvents);
        assertFalse(cdbEvents.isEmpty(), "Should have CDB_EVENT records");

        System.out.println("Transformed to " + cdbEvents.size() + " CDB_EVENT records");

        // Print first record for debugging
        if (!cdbEvents.isEmpty()) {
            System.out.println("First record: " + cdbEvents.get(0));
        }

        // 5. Insert to database
        Map<String, Integer> insertCounts = saveEventsPort.saveRecords(cdbEvents, "OECGROUP_CLM.multiple_records.txt");

        assertNotNull(insertCounts);
        assertTrue(insertCounts.containsKey("CDB_EVENT"));

        int inserted = insertCounts.get("CDB_EVENT");
        System.out.println("Inserted " + inserted + " records into CDB_EVENT table");

        assertTrue(inserted > 0, "Should insert at least one record");
    }

    @Test
    @DisplayName("Should verify parsed field values before insert")
    void shouldVerifyParsedFieldValues() {
        // 1. Parse
        JsonNode railincJson = fixedWidthConverter.convert(testFileContent, schema);

        // 2. Transform
        ProcessingContext context = ProcessingContext.builder()
                .partnerId("OECGROUP")
                .fileName("test.txt")
                .timestamp(LocalDateTime.now())
                .build();

        MappingResult mappingResult = mappingEngine.transform(railincJson, mappingConfig, "OECGROUP", context);

        assertTrue(mappingResult.isSuccess());

        List<Map<String, Object>> cdbEvents = mappingResult.getRecords("CDB_EVENT");

        // Verify first record fields
        Map<String, Object> firstRecord = cdbEvents.get(0);

        System.out.println("=== First Record Fields ===");
        firstRecord.forEach((key, value) -> System.out.println(key + " = " + value));

        // Basic assertions on expected fields
        assertNotNull(firstRecord.get("MBL_NO"), "MBL_NO should not be null");
        assertNotNull(firstRecord.get("CNTR_NO"), "CNTR_NO should not be null");
    }
}
