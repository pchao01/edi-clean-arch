package com.example.edicleanarch.x12.common.parser;


import com.example.edicleanarch.x12.common.model.X12Envelope;

import java.util.Arrays;
import java.util.List;

/**
 * Base parser for all X12 message types.
 * Handles ISA/IEA, GS/GE, ST/SE envelope parsing.
 *
 * @param <T> The parsed file type (Edi315File, Edi310File, etc.)
 */
public abstract class X12BaseParser<T> {

    protected static final String SEGMENT_TERMINATOR = "~";
    protected static final String ELEMENT_SEPARATOR = "*";

    /**
     * Parse X12 content into typed file object.
     */
    public T parse(String content) {
        List<String> segments = splitSegments(content);
        X12Envelope envelope = parseEnvelope(segments);
        return parseTransactionSets(envelope, segments);
    }

    /**
     * Split content into segments.
     */
    protected List<String> splitSegments(String content) {
        return Arrays.stream(content.split(SEGMENT_TERMINATOR))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    /**
     * Parse ISA/GS envelope - shared across all X12 types.
     */
    protected X12Envelope parseEnvelope(List<String> segments) {
        X12Envelope.Builder builder = X12Envelope.builder();

        for (String segment : segments) {
            String[] elements = segment.split("\\" + ELEMENT_SEPARATOR);
            String segmentId = elements[0];

            if ("ISA".equals(segmentId) && elements.length >= 16) {
                builder.senderId(elements[6].trim())
                        .receiverId(elements[8].trim())
                        .date(elements[9])
                        .time(elements[10])
                        .isaControlNumber(elements[13]);
            } else if ("GS".equals(segmentId) && elements.length >= 9) {
                builder.gsControlNumber(elements[6])
                        .version(elements[8]);
            }
        }

        return builder.build();
    }

    /**
     * Parse transaction sets - implemented by each message type.
     */
    protected abstract T parseTransactionSets(X12Envelope envelope, List<String> segments);
}
