package com.example.edicleanarch.common.model;

/**
 * Common Interface: EDI File
 * Base interface for all EDI file types.
 */
public interface EdiFile {

    /**
     * Get the message type (e.g., "315", "RAILINC_CLM", "300", "310")
     */
    String getMessageType();

    /**
     * Get the record count in this file.
     */
    int getRecordCount();

    /**
     * Validate the record count matches the trailer.
     */
    boolean validateRecordCount();
}
