package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.common.validation.SelfValidating;
import jakarta.validation.constraints.NotBlank;
import org.jetbrains.annotations.NotNull;

/**
 * Command: Validate Railinc File
 */
public record ValidateRailincFileCommand(

        @NotNull @NotBlank
        String content,

        @NotNull @NotBlank
        String partnerId

) {
    public ValidateRailincFileCommand(String content, String partnerId) {
        this.content = content;
        this.partnerId = partnerId;
        SelfValidating.validate(this);
    }
}
