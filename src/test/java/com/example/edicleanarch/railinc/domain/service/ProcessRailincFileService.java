package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.common.annotation.UseCase;
import com.example.edicleanarch.common.model.ProcessingResult;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.common.port.in.ValidateEdiFileUseCase;
import com.example.edicleanarch.railinc.domain.model.RailincFile;
import com.example.edicleanarch.railinc.port.out.SaveRailincEventsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
@UseCase
@Transactional
public class ProcessRailincFileService implements
        ProcessEdiFileUseCase<ProcessRailincFileCommand>,
        ValidateEdiFileUseCase<ValidateRailincFileCommand> {

    private static final String MESSAGE_TYPE = "RAILINC_CLM";

    private final RailincFileParser parser;
    private final RailincFileValidator validator;
    private final SaveRailincEventsPort saveEventsPort;

    @Override
    public ProcessingResult processFile(ProcessRailincFileCommand command) {
        log.info("Processing Railinc file: {} for partner: {}",
                command.fileName(), command.partnerId());

        long startTime = System.currentTimeMillis();

        try {
            // Parse
            RailincFile file = parser.parse(command.content());

            // Validate
            List<String> errors = validator.validate(file);
            if (!errors.isEmpty()) {
                return ProcessingResult.validationFailed(
                        MESSAGE_TYPE, command.fileName(), command.partnerId(),
                        errors, System.currentTimeMillis() - startTime);
            }

            // Set file name on events
            file.getEvents().forEach(e -> e.setFileName(command.fileName()));

            // Save
            Map<String, Integer> insertCounts = saveEventsPort.saveEvents(
                    file.getEvents(), command.partnerId(), command.fileName());

            return ProcessingResult.success(
                    MESSAGE_TYPE, command.fileName(), command.partnerId(),
                    file.getEventCount(), insertCounts,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Error processing Railinc file: {}", command.fileName(), e);
            return ProcessingResult.error(
                    MESSAGE_TYPE, command.fileName(), command.partnerId(),
                    e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }

    @Override
    public ProcessingResult validateFile(ValidateRailincFileCommand command) {
        long startTime = System.currentTimeMillis();

        try {
            RailincFile file = parser.parse(command.content());
            List<String> errors = validator.validate(file);

            if (!errors.isEmpty()) {
                return ProcessingResult.validationFailed(
                        MESSAGE_TYPE, null, command.partnerId(),
                        errors, System.currentTimeMillis() - startTime);
            }

            return ProcessingResult.success(
                    MESSAGE_TYPE, null, command.partnerId(),
                    file.getEventCount(), Map.of(),
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            return ProcessingResult.error(
                    MESSAGE_TYPE, null, command.partnerId(),
                    e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
