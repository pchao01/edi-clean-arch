package com.example.edicleanarch.common.port.out;

import com.example.edicleanarch.common.model.EdiEvent;

import java.util.List;
import java.util.Map;

/**
 * Common Output Port: Save Events
 * Persists EDI events to database.
 *
 * @param <E> The event type
 */
public interface SaveEventsPort<E extends EdiEvent> {

    /**
     * Batch save events.
     *
     * @param events List of events to save
     * @param fileName Source file name
     * @return Map of table names to insert counts
     */
    Map<String, Integer> saveEvents(List<E> events, String fileName);
}
