package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.common.annotation.UseCase;
import com.example.edicleanarch.common.model.ProcessingResult;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.common.port.in.ValidateEdiFileUseCase;
import com.example.edicleanarch.railinc.domain.model.RailincParseResult;
import com.example.edicleanarch.railinc.port.out.SaveRailincEventsPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Domain Service: Process Railinc CLM File
 *
 * Flow:
 * 1. Parse fixed-width content to RailincParseResult (dynamic records)
 * 2. Validate the parse result
 * 3. Save records via outbound adapter
 */
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
            // 1. Parse to dynamic result
            RailincParseResult parseResult = parser.parse(command.content());
            log.debug("Parsed {} records from CLM file", parseResult.getRecordCount());

            // 2. Validate
            List<String> errors = validator.validate(parseResult);
            if (!errors.isEmpty()) {
                return ProcessingResult.validationFailed(
                        MESSAGE_TYPE, command.fileName(), command.partnerId(),
                        errors, System.currentTimeMillis() - startTime);
            }

            // 3. Convert records to maps for saving
            List<Map<String, Object>> recordMaps = new ArrayList<>();
            parseResult.getRecords().forEach(record -> {
                Map<String, Object> recordMap = new java.util.LinkedHashMap<>();
                record.getFields().forEach(recordMap::put);
                recordMap.put("fileName", command.fileName());
                recordMap.put("partnerId", command.partnerId());
                recordMaps.add(recordMap);
            });

            // 4. Save
            Map<String, Integer> insertCounts = saveEventsPort.saveRecords(
                    recordMaps, command.fileName());

            return ProcessingResult.success(
                    MESSAGE_TYPE, command.fileName(), command.partnerId(),
                    parseResult.getRecordCount(), insertCounts,
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
            RailincParseResult parseResult = parser.parse(command.content());
            List<String> errors = validator.validate(parseResult);

            if (!errors.isEmpty()) {
                return ProcessingResult.validationFailed(
                        MESSAGE_TYPE, null, command.partnerId(),
                        errors, System.currentTimeMillis() - startTime);
            }

            return ProcessingResult.success(
                    MESSAGE_TYPE, null, command.partnerId(),
                    parseResult.getRecordCount(), Map.of(),
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            return ProcessingResult.error(
                    MESSAGE_TYPE, null, command.partnerId(),
                    e.getMessage(), System.currentTimeMillis() - startTime);
        }
    }
}
