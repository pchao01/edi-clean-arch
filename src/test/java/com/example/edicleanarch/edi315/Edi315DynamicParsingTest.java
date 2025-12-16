package com.example.edicleanarch.edi315;

import com.example.edicleanarch.common.parser.X12ToJsonConverter;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class Edi315DynamicParsingTest {

    private X12ToJsonConverter x12Converter;
    private ObjectMapper objectMapper;
    private String sampleEdi315;

    private static final String EDI_FILE_PATH = "/edi315/CMDU/CMA-CGM_1109643418.txt";

    @BeforeEach
    void setUp() throws IOException {
        x12Converter = new X12ToJsonConverter();
        objectMapper = new ObjectMapper();
        sampleEdi315 = loadEdiFile(EDI_FILE_PATH);
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
    @DisplayName("X12ToJsonConverter should convert EDI 315 file to JsonNode")
    void testX12ToJsonConversion() {
        JsonNode result = x12Converter.convert(sampleEdi315);
        assertNotNull(result);
        assertTrue(result.has("envelope"));
        assertTrue(result.has("transactions"));
        JsonNode envelope = result.get("envelope");
        assertNotNull(envelope.get("ISA"));
        JsonNode isa = envelope.get("ISA");
        assertEquals("CMACGM", isa.get("06").asText().trim());
        assertEquals("OECGROUP", isa.get("08").asText().trim());
        assertEquals("000012250", isa.get("13").asText());
        JsonNode transactions = result.get("transactions");
        assertTrue(transactions.size() > 0);
    }

    @Test
    @DisplayName("Should parse B4 segment fields correctly")
    void testB4SegmentParsing() {
        JsonNode result = x12Converter.convert(sampleEdi315);
        JsonNode transaction = result.get("transactions").get(0);
        JsonNode b4 = transaction.get("B4");
        assertNotNull(b4);
        assertEquals("VD", b4.get("03").asText());
        assertEquals("20220109", b4.get("04").asText());
        assertEquals("CNSHA", b4.get("06").asText());
        assertEquals("CAXU", b4.get("07").asText());
        assertEquals("336841", b4.get("08").asText());
    }

    @Test
    @DisplayName("Should parse N9 segments")
    void testN9SegmentParsing() {
        JsonNode result = x12Converter.convert(sampleEdi315);
        JsonNode transaction = result.get("transactions").get(0);
        JsonNode n9 = transaction.get("N9");
        assertNotNull(n9);
        assertTrue(n9.isArray());
        assertEquals("BM", n9.get(0).get("01").asText());
        assertEquals("CMDUYGOC010284", n9.get(0).get("02").asText());
    }

    @Test
    @DisplayName("Should parse Q2 segment")
    void testQ2SegmentParsing() {
        JsonNode result = x12Converter.convert(sampleEdi315);
        JsonNode transaction = result.get("transactions").get(0);
        JsonNode q2 = transaction.get("Q2");
        assertNotNull(q2);
        assertEquals("9674517", q2.get("01").asText());
        assertEquals("APL DANUBE", q2.get("13").asText());
    }

    @Test
    @DisplayName("Should parse R4 segments")
    void testR4SegmentParsing() {
        JsonNode result = x12Converter.convert(sampleEdi315);
        JsonNode transaction = result.get("transactions").get(0);
        JsonNode r4 = transaction.get("R4");
        assertNotNull(r4);
        assertTrue(r4.isArray());
    }

    @Test
    @DisplayName("Should handle multiple transactions")
    void testMultipleTransactions() {
        JsonNode result = x12Converter.convert(sampleEdi315);
        JsonNode transactions = result.get("transactions");
        assertTrue(transactions.size() > 1);
        JsonNode tx1 = transactions.get(0);
        assertEquals("VD", tx1.get("B4").get("03").asText());
        JsonNode tx2 = transactions.get(1);
        assertEquals("AE", tx2.get("B4").get("03").asText());
    }
}
