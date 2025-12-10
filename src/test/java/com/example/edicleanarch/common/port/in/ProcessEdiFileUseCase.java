package com.example.edicleanarch.common.port.in;

import com.example.edicleanarch.common.model.ProcessingResult;

/**
 * Common Input Port: Process EDI File Use Case
 * Generic interface for processing any EDI file type.
 *
 * @param <C> The command type
 */
public interface ProcessEdiFileUseCase<C extends ProcessEdiFileCommand> {

    /**
     * Process an EDI file.
     *
     * @param command The processing command
     * @return Processing result
     */
    ProcessingResult processFile(C command);
}
