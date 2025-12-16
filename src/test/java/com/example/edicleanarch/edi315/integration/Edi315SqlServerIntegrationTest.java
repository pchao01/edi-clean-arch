package com.example.edicleanarch.edi315.integration;

import com.example.edicleanarch.common.model.ProcessingResult;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.x12.edi315.domain.service.inbound.ProcessEdi315Command;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import javax.sql.DataSource;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration test demonstrating full Clean Architecture flow:
 *
 * Inbound Adapter (simulated) -> UseCase -> Outbound Adapter -> Database
 *
 * Flow:
 * 1. Simulate Kafka message by creating ProcessEdi315Command
 * 2. Call ProcessEdi315Service (UseCase) - same as Kafka adapter does
 * 3. Service internally:
 *    - Parses X12 to JSON (X12ToJsonConverter)
 *    - Transforms using mapping config (EdiMappingEngine)
 *    - Saves via outbound adapter (SaveEdi315EventsPort -> Edi315PersistenceAdapter)
 * 4. Verify data in database
 */
@SpringBootTest
class Edi315SqlServerIntegrationTest {

    @Autowired
    private DataSource dataSource;

    /**
     * The UseCase - same interface that the Kafka inbound adapter uses.
     * ProcessEdi315KafkaConsumer calls this same use case internally.
     */
    @Autowired
    private ProcessEdiFileUseCase<ProcessEdi315Command> processEdi315UseCase;

    private String sampleEdi315Content;

    private static final String EDI_FILE_PATH = "/edi315/CMDU/CMA-CGM_1109643418.txt";
    private static final String FILE_NAME = "CMA-CGM_1109643418.txt";
    private static final String PARTNER_ID = "CMDU";
    private static final String TARGET_TABLE = "CDB_EVENT";

    @BeforeEach
    void setUp() throws IOException {
        sampleEdi315Content = loadEdiFile(EDI_FILE_PATH);
    }

    private String loadEdiFile(String resourcePath) throws IOException {
        try (InputStream is = getClass().getResourceAsStream(resourcePath)) {
            if (is == null) {
                throw new IOException("Resource not found: " + resourcePath);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    @DisplayName("Full Clean Architecture flow: Inbound -> UseCase -> Outbound -> Database")
    void testFullCleanArchitectureFlow() throws Exception {
        System.out.println("=== Clean Architecture Integration Test ===\n");

        // 1. INBOUND ADAPTER (simulated)
        // In production, ProcessEdi315KafkaConsumer creates this command from Kafka message
        System.out.println("1. [Inbound Adapter] Creating command from EDI content...");
        ProcessEdi315Command command = new ProcessEdi315Command(
                sampleEdi315Content,
                PARTNER_ID,
                FILE_NAME
        );
        System.out.println("   File: " + FILE_NAME);
        System.out.println("   Partner: " + PARTNER_ID);
        System.out.println("   Content size: " + sampleEdi315Content.length() + " bytes");

        // 2. USE CASE (Core Domain)
        // ProcessEdi315Service handles:
        //   - X12 parsing (X12ToJsonConverter)
        //   - Mapping transformation (EdiMappingEngine + edi315-mapping.yml)
        //   - Persistence via outbound port (SaveEdi315EventsPort)
        System.out.println("\n2. [UseCase] Processing EDI 315 file...");
        ProcessingResult result = processEdi315UseCase.processFile(command);

        // 3. CHECK RESULT
        System.out.println("\n3. [Result] Processing completed:");
        System.out.println("   Status: " + result.getStatus());
        System.out.println("   Message Type: " + result.getMessageType());
        System.out.println("   Records Processed: " + result.getRecordCount());
        System.out.println("   Processing Time: " + result.getDurationMs() + " ms");

        if (!result.getValidationErrors().isEmpty()) {
            System.out.println("   Errors: " + result.getValidationErrors());
        }

        // Assert success
        assertEquals(ProcessingResult.Status.SUCCESS, result.getStatus(),
                "Processing should succeed. Errors: " + result.getValidationErrors());
        assertTrue(result.getRecordCount() > 0, "Should process at least one record");

        // 4. VERIFY IN DATABASE (via outbound adapter result)
        System.out.println("\n4. [Outbound Adapter] Verifying data in SQL Server CDB...");
        verifyDatabaseRecords();

        System.out.println("\n=== Test Completed Successfully ===");
    }

    private void verifyDatabaseRecords() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(
                     "SELECT TOP 5 CNTR_NO, MBL_NO, EVENT_CODE, EVENT_LOC, FILENAME " +
                     "FROM " + TARGET_TABLE + " " +
                     "WHERE FILENAME = '" + FILE_NAME + "' " +
                     "ORDER BY CREATE_DATE DESC")) {

            int count = 0;
            System.out.println("   Sample records from CDB_EVENT:");
            while (rs.next()) {
                count++;
                System.out.printf("     [%d] CNTR: %s, MBL: %s, EVENT: %s, LOC: %s%n",
                        count,
                        rs.getString("CNTR_NO"),
                        rs.getString("MBL_NO"),
                        rs.getString("EVENT_CODE"),
                        rs.getString("EVENT_LOC"));
            }

            assertTrue(count > 0, "Should have records in database");
            System.out.println("   Total verified: " + count + " records (showing top 5)");
        }
    }


}
