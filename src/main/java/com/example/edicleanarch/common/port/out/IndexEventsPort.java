package com.example.edicleanarch.common.port.out;

import com.example.edicleanarch.common.model.EdiEvent;

import java.util.List;

public interface IndexEventsPort<E extends EdiEvent> {

    /**
     * Bulk index events.
     *
     * @param events List of events to index
     * @param indexName Target index name
     * @param fileName Source file name (used as document ID prefix)
     */
    void indexEvents(List<E> events, String indexName, String fileName);
}
