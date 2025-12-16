package com.example.edicleanarch.x12.edi315.adapter.out;

import com.example.edicleanarch.common.annotation.PersistenceAdapter;
import com.example.edicleanarch.x12.edi315.port.out.SaveEdi315EventsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Outbound Adapter: EDI 315 Event Persistence (Fully Dynamic)
 *
 * Config-driven approach - NO hardcoded field mappings.
 * Column names come directly from edi315-mapping.yml.
 *
 * Adding a new field:
 * 1. Add to edi315-mapping.yml (field transformation)
 * 2. NO Java code changes needed!
 */
@Slf4j
@PersistenceAdapter
@RequiredArgsConstructor
class Edi315PersistenceAdapter implements SaveEdi315EventsPort {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Save records dynamically using config-driven approach.
     * Column names come directly from edi315-mapping.yml field definitions.
     *
     * Flow:
     * 1. edi315-mapping.yml defines: name: MBL_NO, source: B4.11
     * 2. EdiMappingEngine transforms: {MBL_NO: "value", CNTR_NO: "ABCD1234567", ...}
     * 3. This method builds: INSERT INTO CDB_EVENT (MBL_NO, CNTR_NO, ...) VALUES (:MBL_NO, :CNTR_NO, ...)
     *
     * @param records  List of records where keys are column names from YAML mapping
     * @param fileName Source file name for logging
     * @return Map of table name to insert count
     */
    @Override
    public Map<String, Integer> saveRecords(List<Map<String, Object>> records, String fileName) {
        Map<String, Integer> insertCounts = new LinkedHashMap<>();

        if (records == null || records.isEmpty()) {
            insertCounts.put("CDB_EVENT", 0);
            return insertCounts;
        }

        try {
            // Build dynamic INSERT statement from first record's keys
            Map<String, Object> firstRecord = records.get(0);
            String sql = buildInsertSql("CDB_EVENT", firstRecord.keySet());

            log.debug("Dynamic SQL: {}", sql);

            // Batch insert all records
            MapSqlParameterSource[] batchParams = records.stream()
                    .map(MapSqlParameterSource::new)
                    .toArray(MapSqlParameterSource[]::new);

            int[] results = jdbcTemplate.batchUpdate(sql, batchParams);
            int totalInserted = results.length;

            insertCounts.put("CDB_EVENT", totalInserted);
            log.info("Inserted {} records into CDB_EVENT from {}", totalInserted, fileName);

        } catch (Exception e) {
            log.error("Error saving records to CDB_EVENT: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to save records: " + e.getMessage(), e);
        }

        return insertCounts;
    }

    /**
     * Build dynamic INSERT SQL from column names.
     *
     * Example output:
     * INSERT INTO CDB_EVENT (MBL_NO, CNTR_NO, EVENT_CODE, EVENT_DATE, ...)
     * VALUES (:MBL_NO, :CNTR_NO, :EVENT_CODE, :EVENT_DATE, ...)
     *
     * @param tableName Target table name
     * @param columns   Column names from mapping config
     * @return Dynamic INSERT SQL statement
     */
    private String buildInsertSql(String tableName, java.util.Set<String> columns) {
        String columnList = String.join(", ", columns);
        String paramList = columns.stream()
                .map(col -> ":" + col)
                .collect(Collectors.joining(", "));

        return String.format("INSERT INTO %s (%s) VALUES (%s)", tableName, columnList, paramList);
    }
}
