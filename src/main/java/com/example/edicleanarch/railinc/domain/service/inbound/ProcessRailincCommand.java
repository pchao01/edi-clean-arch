package com.example.edicleanarch.railinc.domain.service.inbound;

import com.example.edicleanarch.common.port.in.ProcessEdiFileCommand;

/**
 * Command for processing Railinc CLM inbound files.
 */
public record ProcessRailincCommand(
        String content,
        String partnerId,
        String fileName
) implements ProcessEdiFileCommand {
}
