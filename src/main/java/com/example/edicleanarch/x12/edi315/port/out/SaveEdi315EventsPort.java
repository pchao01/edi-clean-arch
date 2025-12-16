package com.example.edicleanarch.x12.edi315.port.out;


import java.util.List;
import java.util.Map;

/**
 * Output Port: Save EDI 315 Events (Fully Dynamic)
 *
 * Config-driven approach - field names come from edi315-mapping.yml.
 * NO hardcoded CdbEvent model.
 *
 * Adding a new field:
 * 1. Add to edi315-mapping.yml (field transformation)
 * 2. NO Java code changes needed!
 */
public interface SaveEdi315EventsPort {

    /**
     * Save records dynamically using Map (config-driven approach).
     * Field names come from edi315-mapping.yml targets.
     *
     * @param records  List of records where keys are column names from YAML mapping
     * @param fileName Source file name for tracking
     * @return Map of table name to insert count
     */
    Map<String, Integer> saveRecords(List<Map<String, Object>> records, String fileName);
}
