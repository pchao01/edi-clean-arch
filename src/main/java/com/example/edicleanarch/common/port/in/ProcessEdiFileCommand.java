package com.example.edicleanarch.common.port.in;

/**
 * Common Interface: Process EDI File Command
 * Base interface for all EDI processing commands.
 */

public interface ProcessEdiFileCommand {
    /**
     * Get the raw content of the EDI file.
     */
    String content();

    /**
     * Get the partner/sender identifier.
     */
    String partnerId();

    /**
     * Get the file name.
     */
    String fileName();
}
