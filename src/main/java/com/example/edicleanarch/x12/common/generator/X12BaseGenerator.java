package com.example.edicleanarch.x12.common.generator;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Base generator for all X12 message types.
 * Handles ISA/IEA, GS/GE, ST/SE envelope generation.
 *
 * @param <T> The input data type to generate from
 */
public abstract class X12BaseGenerator<T> {

    protected static final String SEGMENT_TERMINATOR = "~\n";
    protected static final String ELEMENT_SEPARATOR = "*";

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyMMdd");
    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HHmm");

    private final AtomicInteger controlNumberSequence = new AtomicInteger(1);

    /**
     * Generate X12 content from data.
     */
    public String generate(T data, String senderId, String receiverId) {
        StringBuilder sb = new StringBuilder();
        LocalDateTime now = LocalDateTime.now();
        String controlNumber = generateControlNumber();

        // ISA - Interchange Header
        sb.append(generateIsa(senderId, receiverId, now, controlNumber));

        // GS - Functional Group Header
        sb.append(generateGs(senderId, receiverId, now, controlNumber));

        // ST/SE - Transaction Sets (implemented by subclass)
        int transactionCount = appendTransactionSets(sb, data, controlNumber);

        // GE - Functional Group Trailer
        sb.append(generateGe(transactionCount, controlNumber));

        // IEA - Interchange Trailer
        sb.append(generateIea(controlNumber));

        return sb.toString();
    }

    /**
     * Generate ISA segment.
     */
    protected String generateIsa(String senderId, String receiverId, LocalDateTime dateTime, String controlNumber) {
        return "ISA" + ELEMENT_SEPARATOR +
                "00" + ELEMENT_SEPARATOR +          // Authorization Info Qualifier
                "          " + ELEMENT_SEPARATOR +  // Authorization Information (10 spaces)
                "00" + ELEMENT_SEPARATOR +          // Security Info Qualifier
                "          " + ELEMENT_SEPARATOR +  // Security Information (10 spaces)
                "ZZ" + ELEMENT_SEPARATOR +          // Interchange ID Qualifier
                padRight(senderId, 15) + ELEMENT_SEPARATOR +
                "ZZ" + ELEMENT_SEPARATOR +          // Interchange ID Qualifier
                padRight(receiverId, 15) + ELEMENT_SEPARATOR +
                dateTime.format(DATE_FORMAT) + ELEMENT_SEPARATOR +
                dateTime.format(TIME_FORMAT) + ELEMENT_SEPARATOR +
                "U" + ELEMENT_SEPARATOR +           // Interchange Control Standards ID
                "00401" + ELEMENT_SEPARATOR +       // Interchange Control Version Number
                padLeft(controlNumber, 9, '0') + ELEMENT_SEPARATOR +
                "0" + ELEMENT_SEPARATOR +           // Acknowledgment Requested
                "P" + ELEMENT_SEPARATOR +           // Usage Indicator (P=Production)
                ">" + SEGMENT_TERMINATOR;           // Component Element Separator
    }

    /**
     * Generate GS segment.
     */
    protected String generateGs(String senderId, String receiverId, LocalDateTime dateTime, String controlNumber) {
        return "GS" + ELEMENT_SEPARATOR +
                getFunctionalIdentifierCode() + ELEMENT_SEPARATOR +
                senderId + ELEMENT_SEPARATOR +
                receiverId + ELEMENT_SEPARATOR +
                dateTime.format(DateTimeFormatter.ofPattern("yyyyMMdd")) + ELEMENT_SEPARATOR +
                dateTime.format(TIME_FORMAT) + ELEMENT_SEPARATOR +
                controlNumber + ELEMENT_SEPARATOR +
                "X" + ELEMENT_SEPARATOR +           // Responsible Agency Code
                "004010" + SEGMENT_TERMINATOR;      // Version
    }

    /**
     * Generate GE segment.
     */
    protected String generateGe(int transactionCount, String controlNumber) {
        return "GE" + ELEMENT_SEPARATOR +
                transactionCount + ELEMENT_SEPARATOR +
                controlNumber + SEGMENT_TERMINATOR;
    }

    /**
     * Generate IEA segment.
     */
    protected String generateIea(String controlNumber) {
        return "IEA" + ELEMENT_SEPARATOR +
                "1" + ELEMENT_SEPARATOR +
                padLeft(controlNumber, 9, '0') + SEGMENT_TERMINATOR;
    }

    /**
     * Get functional identifier code (QO for 315, etc.)
     */
    protected abstract String getFunctionalIdentifierCode();

    /**
     * Append transaction sets - implemented by each message type.
     * @return number of transaction sets appended
     */
    protected abstract int appendTransactionSets(StringBuilder sb, T data, String controlNumber);

    /**
     * Generate control number.
     */
    protected String generateControlNumber() {
        return String.valueOf(controlNumberSequence.getAndIncrement());
    }

    protected String padRight(String value, int length) {
        if (value == null) value = "";
        return String.format("%-" + length + "s", value).substring(0, length);
    }

    protected String padLeft(String value, int length, char padChar) {
        if (value == null) value = "";
        StringBuilder sb = new StringBuilder();
        for (int i = value.length(); i < length; i++) {
            sb.append(padChar);
        }
        sb.append(value);
        return sb.toString();
    }
}
