package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.common.port.in.ProcessEdiFileCommand;
import com.example.edicleanarch.common.validation.SelfValidating;
import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;

/**
 * Command: Process Railinc File
 */
public record ProcessRailincFileCommand(

        @NotNull @NotBlank
        String content,

        @NotNull @NotBlank
        String partnerId,

        @NotNull @NotBlank
        String fileName

) implements ProcessEdiFileCommand {

    public ProcessRailincFileCommand(String content, String partnerId, String fileName) {
        this.content = content;
        this.partnerId = partnerId;
        this.fileName = fileName;
        SelfValidating.validate(this);
    }
}
