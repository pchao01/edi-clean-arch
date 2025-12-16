package com.example.edicleanarch.railinc.port.out;

import com.example.edicleanarch.railinc.domain.model.ContainerEvent;

import java.util.List;
import java.util.Map;

/**
 * Output Port: Save Railinc Events
 *
 * Supports two modes:
 * 1. Typed model (ContainerEvent) - legacy approach
 * 2. Dynamic records (Map<String, Object>) - config-driven approach
 */
public interface SaveRailincEventsPort {


    /**
     * Save records dynamically using Map (config-driven approach).
     * Field names come from railinc-mapping.yml targets.
     *
     * @param records List of records where keys are column names from YAML mapping
     * @param fileName Source file name for tracking
     * @return Map of table name to insert count
     */
    Map<String, Integer> saveRecords(List<Map<String, Object>> records, String fileName);
}
