package com.example.edicleanarch.x12.edi315.domain.service.inbound;

import com.example.edicleanarch.common.port.in.ProcessEdiFileCommand;

/**
 * Command for processing EDI 315 inbound files.
 */
public record ProcessEdi315Command(
        String content,
        String partnerId,
        String fileName
) implements ProcessEdiFileCommand {
}
