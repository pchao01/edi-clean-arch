package com.example.edicleanarch.x12.edi315.domain.service.outbound;

import com.example.edicleanarch.x12.common.generator.X12BaseGenerator;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Domain Service: Generate EDI 315 X12 content.
 *
 * Config-driven approach - uses dynamic Map<String, Object> instead of typed model.
 * Field names come from database columns defined in edi315-mapping.yml.
 */
@Component
public class Edi315Generator extends X12BaseGenerator<List<Map<String, Object>>> {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    @Override
    protected String getFunctionalIdentifierCode() {
        return "QO";  // 315 Status Details
    }

    @Override
    protected int appendTransactionSets(StringBuilder sb, List<Map<String, Object>> events, String controlNumber) {
        AtomicInteger stControlNumber = new AtomicInteger(1);

        for (Map<String, Object> event : events) {
            String stCtrl = String.valueOf(stControlNumber.getAndIncrement());
            int segmentCount = 0;

            // ST - Transaction Set Header
            sb.append("ST").append(ELEMENT_SEPARATOR)
                    .append("315").append(ELEMENT_SEPARATOR)
                    .append(padLeft(stCtrl, 4, '0'))
                    .append(SEGMENT_TERMINATOR);
            segmentCount++;

            // B4 - Beginning Segment
            sb.append("B4").append(ELEMENT_SEPARATOR)
                    .append(ELEMENT_SEPARATOR)  // Special Handling Code
                    .append(getString(event, "EVENT_CODE")).append(ELEMENT_SEPARATOR)
                    .append(formatDate(event)).append(ELEMENT_SEPARATOR)
                    .append(formatTime(event)).append(ELEMENT_SEPARATOR)
                    .append(ELEMENT_SEPARATOR)  // Status Time
                    .append(getString(event, "EVENT_LOC")).append(ELEMENT_SEPARATOR)
                    .append(getString(event, "CNTR_NO")).append(ELEMENT_SEPARATOR)
                    .append(ELEMENT_SEPARATOR)  // Equipment Type
                    .append(ELEMENT_SEPARATOR)  // Commodity Code
                    .append(getString(event, "MBL_NO"))
                    .append(SEGMENT_TERMINATOR);
            segmentCount++;

            // N9 - Reference Identification (Booking Number)
            String bkgNo = getString(event, "BKG_NO");
            if (!bkgNo.isEmpty()) {
                sb.append("N9").append(ELEMENT_SEPARATOR)
                        .append("BN").append(ELEMENT_SEPARATOR)  // Booking Number
                        .append(bkgNo)
                        .append(SEGMENT_TERMINATOR);
                segmentCount++;
            }

            // Q2 - Status Details (Vessel)
            String vessel = getString(event, "VESSEL");
            String voyage = getString(event, "VOYAGE");
            if (!vessel.isEmpty() || !voyage.isEmpty()) {
                sb.append("Q2").append(ELEMENT_SEPARATOR)
                        .append(getString(event, "VESSEL_CD")).append(ELEMENT_SEPARATOR)
                        .append(ELEMENT_SEPARATOR)  // Country Code
                        .append(ELEMENT_SEPARATOR)  // Date
                        .append(ELEMENT_SEPARATOR)  // Date
                        .append(ELEMENT_SEPARATOR)  // Date
                        .append(ELEMENT_SEPARATOR)  // Lading Quantity
                        .append(ELEMENT_SEPARATOR)  // Weight
                        .append(ELEMENT_SEPARATOR)  // Weight Qualifier
                        .append(voyage).append(ELEMENT_SEPARATOR)
                        .append(ELEMENT_SEPARATOR)  // Reference ID Qualifier
                        .append(ELEMENT_SEPARATOR)  // Reference ID
                        .append(vessel)
                        .append(SEGMENT_TERMINATOR);
                segmentCount++;
            }

            // R4 - Port/Location
            String eventLoc = getString(event, "EVENT_LOC");
            if (!eventLoc.isEmpty()) {
                sb.append("R4").append(ELEMENT_SEPARATOR)
                        .append("L").append(ELEMENT_SEPARATOR)  // Port Function Code
                        .append("UN").append(ELEMENT_SEPARATOR) // Location Qualifier
                        .append(eventLoc).append(ELEMENT_SEPARATOR)
                        .append(getString(event, "EVENT_LOC_NAME"))
                        .append(SEGMENT_TERMINATOR);
                segmentCount++;
            }

            // SE - Transaction Set Trailer
            sb.append("SE").append(ELEMENT_SEPARATOR)
                    .append(segmentCount + 1).append(ELEMENT_SEPARATOR)  // +1 for SE itself
                    .append(padLeft(stCtrl, 4, '0'))
                    .append(SEGMENT_TERMINATOR);
        }

        return events.size();
    }

    private String formatDate(Map<String, Object> event) {
        Object eventDate = event.get("EVENT_DATE");
        if (eventDate instanceof LocalDateTime ldt) {
            return ldt.format(DATE_FORMAT);
        }
        return "";
    }

    private String formatTime(Map<String, Object> event) {
        Object eventDate = event.get("EVENT_DATE");
        if (eventDate instanceof LocalDateTime ldt) {
            return ldt.format(TIME_FORMAT);
        }
        return "";
    }

    private String getString(Map<String, Object> event, String key) {
        Object value = event.get(key);
        return value != null ? value.toString() : "";
    }
}
