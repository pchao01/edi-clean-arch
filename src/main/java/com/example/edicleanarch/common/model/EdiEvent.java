package com.example.edicleanarch.common.model;

import java.time.LocalDateTime;

/**
 * Common Interface: EDI Event
 * Base interface for all container/shipment events.
 */
public interface EdiEvent {

    /**
     * Get the container/equipment number.
     */
    String getContainerNumber();

    /**
     * Get the event code.
     */
    String getEventCode();

    /**
     * Get the event date/time.
     */
    LocalDateTime getEventDateTime();

    /**
     * Get the event location code.
     */
    String getEventLocation();

    /**
     * Get the source file name.
     */
    String getFileName();
}
