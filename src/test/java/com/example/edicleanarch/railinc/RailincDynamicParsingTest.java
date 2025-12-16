package com.example.edicleanarch.railinc;

import com.example.edicleanarch.common.parser.FixedWidthToJsonConverter;
import com.example.edicleanarch.common.schema.FieldDefinition;
import com.example.edicleanarch.common.schema.FixedWidthSchema;
import com.example.edicleanarch.railinc.domain.model.RailincParseResult;
import com.example.edicleanarch.railinc.domain.model.RailincRecord;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Railinc Dynamic Parsing Tests")
class RailincDynamicParsingTest {

    private FixedWidthToJsonConverter jsonConverter;
    private FixedWidthSchema schema;
    private ObjectMapper yamlMapper;

    // Test file content - loaded from resources
    private String testFileContent;

    @BeforeEach
    void setUp() throws IOException {
        jsonConverter = new FixedWidthToJsonConverter();
        yamlMapper = new ObjectMapper(new YAMLFactory());

        // Load schema from YAML
        schema = loadSchemaFromYaml();

        // Load test file content
        testFileContent = loadTestFile();
    }

    /**
     * Load schema from railinc-schema.yml in config/mappings/inbound
     */
    private FixedWidthSchema loadSchemaFromYaml() throws IOException {
        ClassPathResource resource = new ClassPathResource("config/mappings/inbound/railinc-schema.yml");
        try (InputStream is = resource.getInputStream()) {
            return yamlMapper.readValue(is, FixedWidthSchema.class);
        }
    }

    /**
     * Load test file OECGROUP_CLM.multiple_records.txt from resources
     */
    private String loadTestFile() throws IOException {
        ClassPathResource resource = new ClassPathResource("railinc/OECGROUP_CLM.multiple_records.txt");
        try (InputStream is = resource.getInputStream()) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /**
     * Parse test file using schema and return result.
     */
    private RailincParseResult parseTestFile() {
        JsonNode jsonNode = jsonConverter.convert(testFileContent, schema);
        return RailincParseResult.fromJsonNode(jsonNode);
    }

    /**
     * Parse with custom schema.
     */
    private RailincParseResult parseWithSchema(FixedWidthSchema customSchema) {
        JsonNode jsonNode = jsonConverter.convert(testFileContent, customSchema);
        return RailincParseResult.fromJsonNode(jsonNode);
    }

    @Nested
    @DisplayName("Parse 11 Records from Test File")
    class ParseMultipleRecordsTests {

        @Test
        @DisplayName("Should parse all 11 records from test file")
        void shouldParseAll11Records() {
            RailincParseResult result = parseTestFile();

            assertEquals(11, result.getRecords().size());
            assertEquals(11, result.getRecordCount());
            assertEquals(11, result.getTrailerRecordCount());
            assertTrue(result.isRecordCountValid());
        }

        @Test
        @DisplayName("Should parse each container number correctly")
        void shouldParseEachContainerNumberCorrectly() {
            RailincParseResult result = parseTestFile();
            List<RailincRecord> records = result.getRecords();

            // Expected container initials from test file
            String[] expectedInitials = {
                    "BMOU", "TCKU", "TRHU", "CMAU", "GESU",
                    "TRHU", "TCNU", "CMAU", "TCLU", "TCNU", "SLSU"
            };

            String[] expectedNumbers = {
                    "564902", "452817", "102238", "041095", "111864",
                    "868439", "531673", "449289", "949468", "817764", "604129"
            };

            String[] expectedCheckDigits = {
                    "0", "3", "5", "1", "6", "9", "8", "7", "5", "1", "2"
            };

            for (int i = 0; i < 11; i++) {
                RailincRecord record = records.get(i);
                assertEquals(expectedInitials[i], record.get("equipmentInitial"),
                        "Record " + i + " equipmentInitial mismatch");
                assertEquals(expectedNumbers[i], record.get("equipmentNumber"),
                        "Record " + i + " equipmentNumber mismatch");
                assertEquals(expectedCheckDigits[i], record.get("equipmentCheckDigit"),
                        "Record " + i + " equipmentCheckDigit mismatch");
            }
        }

        @Test
        @DisplayName("Should parse each MBL number correctly")
        void shouldParseEachMblNumberCorrectly() {
            RailincParseResult result = parseTestFile();
            List<RailincRecord> records = result.getRecords();

            String[] expectedMblNumbers = {
                    "CMDUGGZ2010179", "CMDUSHZ5804346", "CMDUSHZ5846987",
                    "CMDUXIA1230499", "CMDUAYN1070567", "CMDUSGN1971220",
                    "CMDUCHN0698633", "CMDUSGN1969674", "MEDUUQ430602",
                    "CMDUCHN0673815", "CMDUSGN1960438"
            };

            for (int i = 0; i < 11; i++) {
                String actualMbl = records.get(i).get("mblNo").trim();
                assertEquals(expectedMblNumbers[i], actualMbl,
                        "Record " + i + " mblNo mismatch");
            }
        }

        @Test
        @DisplayName("Should parse SCAC codes correctly")
        void shouldParseScacCodesCorrectly() {
            RailincParseResult result = parseTestFile();
            List<RailincRecord> records = result.getRecords();

            // Records 0-7 and 9-10 are CMDU, record 8 is MEDU
            for (int i = 0; i < 11; i++) {
                String expectedScac = (i == 8) ? "MEDU" : "CMDU";
                assertEquals(expectedScac, records.get(i).get("scac"),
                        "Record " + i + " scac mismatch");
            }
        }

        @Test
        @DisplayName("Should parse destination locations correctly")
        void shouldParseDestinationLocationsCorrectly() {
            RailincParseResult result = parseTestFile();
            List<RailincRecord> records = result.getRecords();

            // Check destination UNLOCDE for various records
            assertEquals("USMEM", records.get(0).get("destinationLocationUnlocde")); // MEMPHIS
            assertEquals("USSTL", records.get(1).get("destinationLocationUnlocde")); // STLOUIS
            assertEquals("USHXC", records.get(8).get("destinationLocationUnlocde")); // ALLIANCE
        }
    }

    @Nested
    @DisplayName("Dynamic Field Access Tests")
    class DynamicFieldAccessTests {

        @Test
        @DisplayName("Should access any field by name from schema")
        void shouldAccessAnyFieldByNameFromSchema() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            // Access fields dynamically by name (from schema)
            assertEquals("BMOU", record.get("equipmentInitial"));
            assertEquals("564902", record.get("equipmentNumber"));
            assertEquals("6006", record.get("eventTypeCode"));
            assertEquals("CMDU", record.get("scac"));
            assertEquals("BNSF", record.get("reportingRailroadScac"));
            assertEquals("RRDC", record.get("headerRailroadScac"));
        }

        @Test
        @DisplayName("Should return empty string for non-existent field")
        void shouldReturnEmptyStringForNonExistentField() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            assertEquals("", record.get("nonExistentField"));
            assertEquals("default", record.get("nonExistentField", "default"));
        }

        @Test
        @DisplayName("Should list all field names from record")
        void shouldListAllFieldNamesFromRecord() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            // All fields from schema should be accessible
            assertTrue(record.fieldNames().contains("equipmentInitial"));
            assertTrue(record.fieldNames().contains("mblNo"));
            assertTrue(record.fieldNames().contains("eventTypeCode"));
            assertTrue(record.fieldNames().contains("scac"));
        }

        @Test
        @DisplayName("Should check field existence with hasField")
        void shouldCheckFieldExistenceWithHasField() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            assertTrue(record.hasField("equipmentInitial"));
            assertTrue(record.hasField("mblNo"));
            assertFalse(record.hasField("nonExistentField"));
        }
    }

    @Nested
    @DisplayName("Adding New Field Tests - No Code Change Required")
    class AddingNewFieldTests {

        @Test
        @DisplayName("Should parse new field when added to schema only")
        void shouldParseNewFieldWhenAddedToSchemaOnly() throws IOException {
            // Add a NEW field - aeiReidIndicator at positions 71-72
            FixedWidthSchema customSchema = loadSchemaFromYaml();
            customSchema.getDataFields().add(createField("aeiReidIndicator", 71, 72));

            RailincParseResult result = parseWithSchema(customSchema);
            RailincRecord record = result.getRecords().get(0);

            // New field is automatically available!
            assertTrue(record.hasField("aeiReidIndicator"));
            assertNotNull(record.get("aeiReidIndicator"));
        }

        @Test
        @DisplayName("Should parse multiple new fields without code changes")
        void shouldParseMultipleNewFieldsWithoutCodeChanges() throws IOException {
            FixedWidthSchema customSchema = loadSchemaFromYaml();

            // Add multiple NEW fields
            customSchema.getDataFields().add(createField("clmSource", 72, 73));
            customSchema.getDataFields().add(createField("etaDestEventCode", 113, 114));

            RailincParseResult result = parseWithSchema(customSchema);
            RailincRecord record = result.getRecords().get(0);

            // All new fields automatically available
            assertTrue(record.hasField("clmSource"));
            assertTrue(record.hasField("etaDestEventCode"));
            assertTrue(record.hasField("sightingSplc")); // Already in schema

            // Verify sightingSplc value from test file (already in schema)
            assertEquals("785121000", record.get("sightingSplc"));
        }

        @Test
        @DisplayName("Should handle field position changes via schema only")
        void shouldHandleFieldPositionChangesViaSchemaOnly() throws IOException {
            // Original mblNo at 145-165 (20 chars)
            RailincParseResult result1 = parseTestFile();
            String originalMbl = result1.getRecords().get(0).get("mblNo");

            // Modified schema with shorter mblNo (145-155, 10 chars - only the SCAC prefix)
            FixedWidthSchema customSchema = loadSchemaFromYaml();
            customSchema.getDataFields().removeIf(f -> "mblNo".equals(f.getName()));
            customSchema.getDataFields().add(createField("mblNo", 145, 155));

            RailincParseResult result2 = parseWithSchema(customSchema);
            String shorterMbl = result2.getRecords().get(0).get("mblNo");

            // Different positions yield different values - no code change!
            // Original: "CMDUGGZ2010179" (trimmed), shorter: "CMDUGGZ201" (10 chars)
            assertNotEquals(originalMbl, shorterMbl);
            assertTrue(originalMbl.startsWith(shorterMbl),
                    "Original '" + originalMbl + "' should start with shorter '" + shorterMbl + "'");
        }
    }

    @Nested
    @DisplayName("Header and Trailer Tests")
    class HeaderTrailerTests {

        @Test
        @DisplayName("Should parse header fields correctly")
        void shouldParseHeaderFieldsCorrectly() {
            RailincParseResult result = parseTestFile();

            assertEquals("CLM", result.getHeaderField("recordType"));
            assertEquals("LFR", result.getHeaderField("messageType"));
            assertEquals("RRDC", result.getHeaderField("railroadCode"));
            assertEquals("OECGROUP", result.getHeaderField("partnerName"));
        }

        @Test
        @DisplayName("Should parse trailer fields correctly")
        void shouldParseTrailerFieldsCorrectly() {
            RailincParseResult result = parseTestFile();

            assertEquals("EOM", result.getTrailerField("recordType"));
            assertEquals(11, result.getTrailerRecordCount());
        }

        @Test
        @DisplayName("Should validate record count matches trailer")
        void shouldValidateRecordCountMatchesTrailer() {
            RailincParseResult result = parseTestFile();

            assertTrue(result.isRecordCountValid());
            assertEquals(result.getRecordCount(), result.getTrailerRecordCount());
        }

        @Test
        @DisplayName("Should parse header sequenceNumber from schema")
        void shouldParseHeaderSequenceNumberFromSchema() {
            RailincParseResult result = parseTestFile();

            // sequenceNumber is defined in railinc-schema.yml at positions 24-37
            assertTrue(result.getHeader().containsKey("sequenceNumber"));
            assertEquals("2312282200001", result.getHeaderField("sequenceNumber"));
        }
    }

    @Nested
    @DisplayName("Sighting Date/Time Tests")
    class SightingDateTimeTests {

        @Test
        @DisplayName("Should parse sighting date components correctly")
        void shouldParseSightingDateComponentsCorrectly() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            // Sighting: 2023-12-28 20:11
            assertEquals("20", record.get("sightingCentury"));
            assertEquals("23", record.get("sightingYear"));
            assertEquals("12", record.get("sightingMonth"));
            assertEquals("28", record.get("sightingDay"));
            assertEquals("20", record.get("sightingHour"));
            assertEquals("11", record.get("sightingMinute"));
        }

        @Test
        @DisplayName("Should parse ETA date components correctly")
        void shouldParseEtaDateComponentsCorrectly() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            // ETA: 2023-12-27 23:xx
            assertEquals("20", record.get("etaCentury"));
            assertEquals("23", record.get("etaYear"));
            assertEquals("12", record.get("etaMonth"));
            assertEquals("27", record.get("etaDay"));
            assertEquals("23", record.get("etaHour"));
        }

        @Test
        @DisplayName("Should handle different ETA dates across records")
        void shouldHandleDifferentEtaDatesAcrossRecords() {
            RailincParseResult result = parseTestFile();

            // Record 8 (TCLU) has different ETA: 2023-12-30 01:xx
            RailincRecord record8 = result.getRecords().get(8);
            assertEquals("30", record8.get("etaDay"));
            assertEquals("01", record8.get("etaHour"));

            // Other records have ETA: 2023-12-27 23:xx
            RailincRecord record0 = result.getRecords().get(0);
            assertEquals("27", record0.get("etaDay"));
            assertEquals("23", record0.get("etaHour"));
        }
    }

    @Nested
    @DisplayName("JsonNode Access Tests")
    class JsonNodeAccessTests {

        @Test
        @DisplayName("Should provide raw JsonNode for advanced access")
        void shouldProvideRawJsonNodeForAdvancedAccess() {
            RailincParseResult result = parseTestFile();

            JsonNode sourceJson = result.getSourceJson();
            assertNotNull(sourceJson);
            assertTrue(sourceJson.has("header"));
            assertTrue(sourceJson.has("records"));
            assertTrue(sourceJson.has("trailer"));
            assertTrue(sourceJson.has("_metadata"));

            // Verify metadata
            assertEquals(11, sourceJson.get("_metadata").get("recordCount").asInt());
        }

        @Test
        @DisplayName("Record should retain source JsonNode")
        void recordShouldRetainSourceJsonNode() {
            RailincParseResult result = parseTestFile();
            RailincRecord record = result.getRecords().get(0);

            JsonNode sourceNode = record.getSourceNode();
            assertNotNull(sourceNode);
            assertEquals("BMOU", sourceNode.get("equipmentInitial").asText());
        }
    }

    @Nested
    @DisplayName("Schema Reload Simulation Tests")
    class SchemaReloadTests {

        @Test
        @DisplayName("Should support different schemas for same content")
        void shouldSupportDifferentSchemasForSameContent() throws IOException {
            // Parse with original schema
            RailincParseResult result1 = parseTestFile();
            int fieldCount1 = result1.getRecords().get(0).fieldNames().size();

            // Parse with extended schema (simulating hot-reload)
            FixedWidthSchema extendedSchema = loadSchemaFromYaml();
            extendedSchema.getDataFields().add(createField("newField1", 71, 72));
            extendedSchema.getDataFields().add(createField("newField2", 72, 73));

            RailincParseResult result2 = parseWithSchema(extendedSchema);
            int fieldCount2 = result2.getRecords().get(0).fieldNames().size();

            // More fields available with extended schema
            assertEquals(fieldCount1 + 2, fieldCount2);
        }
    }

    @Nested
    @DisplayName("Event Type Code Tests")
    class EventTypeCodeTests {

        @Test
        @DisplayName("Should parse event type code correctly for all records")
        void shouldParseEventTypeCodeCorrectlyForAllRecords() {
            RailincParseResult result = parseTestFile();

            // All records have event type code 6006
            for (RailincRecord record : result.getRecords()) {
                assertEquals("6006", record.get("eventTypeCode"));
            }
        }
    }

    @Nested
    @DisplayName("Location UNLOCDE Tests")
    class LocationUnlocdeTests {

        @Test
        @DisplayName("Should parse current location UNLOCDE correctly")
        void shouldParseCurrentLocationUnlocdeCorrectly() {
            RailincParseResult result = parseTestFile();

            // All records originate from USBLN (Belen, NM)
            for (RailincRecord record : result.getRecords()) {
                assertEquals("USBLN", record.get("currentLocationUnlocde"));
            }
        }

        @Test
        @DisplayName("Should parse various destination UNLOCDEs")
        void shouldParseVariousDestinationUnlocdes() {
            RailincParseResult result = parseTestFile();
            List<RailincRecord> records = result.getRecords();

            // MEMPHIS destinations
            assertEquals("USMEM", records.get(0).get("destinationLocationUnlocde"));
            assertEquals("USMEM", records.get(2).get("destinationLocationUnlocde"));

            // STLOUIS destinations
            assertEquals("USSTL", records.get(1).get("destinationLocationUnlocde"));
            assertEquals("USSTL", records.get(4).get("destinationLocationUnlocde"));

            // ALLIANCE destination (record 8)
            assertEquals("USHXC", records.get(8).get("destinationLocationUnlocde"));
        }
    }

    // Helper method to create field definition for test customizations

    private FieldDefinition createField(String name, int start, int end) {
        FieldDefinition field = new FieldDefinition();
        field.setName(name);
        field.setStart(start);
        field.setEnd(end);
        field.setTrim(true);
        return field;
    }
}
