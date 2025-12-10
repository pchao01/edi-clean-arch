package com.example.edicleanarch.railinc.domain.service;

import com.example.edicleanarch.railinc.domain.model.ContainerEvent;
import com.example.edicleanarch.railinc.domain.model.RailincFile;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * Domain Service: Railinc File Validator
 * Validates parsed Railinc CLM files.
 */
@Slf4j
@Component
class RailincFileValidator {

    List<String> validate(RailincFile file) {
        List<String> errors = new ArrayList<>();

        if (file.getHeader() == null) {
            errors.add("CLM header record is required");
        } else {
            if (!"CLM".equals(file.getHeader().recordType())) {
                errors.add("Header must be CLM record type");
            }
            if (isEmpty(file.getHeader().senderScac())) {
                errors.add("Sender SCAC is required in header");
            }
        }

        if (file.getTrailer() == null) {
            errors.add("EOM trailer record is required");
        }

        if (!file.validateRecordCount()) {
            errors.add(String.format("Record count mismatch: actual=%d, expected=%d",
                    file.getEventCount(),
                    file.getTrailer() != null ? file.getTrailer().recordCount() : 0));
        }

        if (file.getEvents() == null || file.getEvents().isEmpty()) {
            errors.add("No data records found");
        } else {
            validateEvents(file.getEvents(), errors);
        }

        return errors;
    }

    private void validateEvents(List<ContainerEvent> events, List<String> errors) {
        for (int i = 0; i < Math.min(events.size(), 5); i++) {
            var event = events.get(i);
            int recordNum = i + 1;

            if (event.getEquipmentId() == null) {
                errors.add(String.format("Record %d: Equipment ID is required", recordNum));
                continue;
            }

            if (isEmpty(event.getEquipmentId().initial())) {
                errors.add(String.format("Record %d: Equipment initial is required", recordNum));
            }

            if (isEmpty(event.getEquipmentId().number())) {
                errors.add(String.format("Record %d: Equipment number is required", recordNum));
            }
        }
    }

    private boolean isEmpty(String value) {
        return value == null || value.trim().isEmpty();
    }
}
