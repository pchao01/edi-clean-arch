package com.example.edicleanarch.x12.edi315.port.out;

import java.util.List;
import java.util.Map;

/**
 * Output Port: Load events for outbound EDI generation.
 *
 * Config-driven approach - uses dynamic Map<String, Object> instead of typed model.
 */
public interface LoadEventsPort {

    /**
     * Find events by IDs for outbound generation.
     *
     * @param eventIds List of event IDs
     * @return List of events as dynamic maps
     */
    List<Map<String, Object>> findByIds(List<Long> eventIds);

    /**
     * Find pending events to send for a specific SCAC.
     *
     * @param scac  SCAC code
     * @param limit Maximum number of events to return
     * @return List of events as dynamic maps
     */
    List<Map<String, Object>> findPendingBySenderScac(String scac, int limit);

    /**
     * Mark events as sent.
     *
     * @param eventIds List of event IDs to mark as sent
     */
    void markAsSent(List<Long> eventIds);
}
