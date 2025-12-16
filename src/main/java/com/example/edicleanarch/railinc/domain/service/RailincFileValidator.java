package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.railinc.domain.model.RailincParseResult;
import com.example.edicleanarch.railinc.domain.model.RailincRecord;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain Service: Railinc File Validator
 * Validates parsed Railinc CLM files (dynamic records).
 */
@Slf4j
@Component
class RailincFileValidator {

    List<String> validate(RailincParseResult parseResult) {
        List<String> errors = new ArrayList<>();

        // Validate header
        if (parseResult.getHeader() == null || parseResult.getHeader().isEmpty()) {
            errors.add("CLM header record is required");
        } else {
            String recordType = parseResult.getHeaderField("recordType");
            if (!"CLM".equals(recordType)) {
                errors.add("Header must be CLM record type");
            }
            if (isEmpty(parseResult.getHeaderField("railroadCode"))) {
                errors.add("Railroad code is required in header");
            }
        }

        // Validate trailer
        if (parseResult.getTrailer() == null || parseResult.getTrailer().isEmpty()) {
            errors.add("EOM trailer record is required");
        }

        // Validate record count
        if (!parseResult.isRecordCountValid()) {
            errors.add(String.format("Record count mismatch: actual=%d, expected=%d",
                    parseResult.getRecordCount(),
                    parseResult.getTrailerRecordCount()));
        }

        // Validate records
        List<RailincRecord> records = parseResult.getRecords();
        if (records == null || records.isEmpty()) {
            errors.add("No data records found");
        } else {
            validateRecords(records, errors);
        }

        return errors;
    }

    private void validateRecords(List<RailincRecord> records, List<String> errors) {
        // Validate first few records to catch systematic issues
        for (int i = 0; i < Math.min(records.size(), 5); i++) {
            RailincRecord record = records.get(i);
            int recordNum = i + 1;

            // Equipment ID validation (dynamic field access)
            if (isEmpty(record.get("equipmentInitial"))) {
                errors.add(String.format("Record %d: Equipment initial is required", recordNum));
            }

            if (isEmpty(record.get("equipmentNumber"))) {
                errors.add(String.format("Record %d: Equipment number is required", recordNum));
            }
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
