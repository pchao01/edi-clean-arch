package com.example.edicleanarch.common.parser;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

@Component
public class X12ToJsonConverter {

    private final ObjectMapper mapper = new ObjectMapper();

    /**
     * Convert X12 EDI content to JsonNode.
     */
    public JsonNode convert(String ediContent) {
        ObjectNode root = mapper.createObjectNode();

        // Parse delimiters from ISA segment
        EdiDelimiters delimiters = parseDelimiters(ediContent);

        // Normalize content
        String normalized = ediContent.replace("\r", "").replace("\n", "");

        // Split into segments
        String[] segments = normalized.split(Pattern.quote(delimiters.getSegmentTerminator()));

        // Build JSON structure
        ObjectNode envelope = mapper.createObjectNode();
        ArrayNode transactions = mapper.createArrayNode();

        ObjectNode currentTransaction = null;

        for (String segment : segments) {
            if (segment.isBlank()) continue;

            String[] elements = segment.split(Pattern.quote(delimiters.getElementSeparator()), -1);
            String segmentId = elements[0].trim();

            ObjectNode segmentNode = createSegmentNode(elements);

            switch (segmentId) {
                case "ISA" -> envelope.set("ISA", segmentNode);
                case "GS" -> envelope.set("GS", segmentNode);
                case "ST" -> {
                    currentTransaction = mapper.createObjectNode();
                    currentTransaction.set("ST", segmentNode);
                }
                case "SE" -> {
                    if (currentTransaction != null) {
                        currentTransaction.set("SE", segmentNode);
                        transactions.add(currentTransaction);
                        currentTransaction = null;
                    }
                }
                case "GE" -> envelope.set("GE", segmentNode);
                case "IEA" -> envelope.set("IEA", segmentNode);
                default -> {
                    if (currentTransaction != null) {
                        addSegmentToTransaction(currentTransaction, segmentId, segmentNode);
                    }
                }
            }
        }

        root.set("envelope", envelope);
        root.set("transactions", transactions);

        // Add metadata
        ObjectNode metadata = mapper.createObjectNode();
        metadata.put("transactionCount", transactions.size());
        metadata.put("elementSeparator", delimiters.getElementSeparator());
        metadata.put("segmentTerminator", delimiters.getSegmentTerminator());
        root.set("_metadata", metadata);

        return root;
    }

    /**
     * Parse ISA segment to extract delimiters.
     */
    private EdiDelimiters parseDelimiters(String content) {
        // Element separator is at position 3
        String elementSeparator = String.valueOf(content.charAt(3));

        // Segment terminator is at position 105 (after ISA16)
        String segmentTerminator = "~"; // Default
        if (content.length() > 105) {
            segmentTerminator = String.valueOf(content.charAt(105));
        }

        return new EdiDelimiters(elementSeparator, segmentTerminator);
    }

    /**
     * Create segment node with positional element keys (01, 02, 03...).
     */
    private ObjectNode createSegmentNode(String[] elements) {
        ObjectNode node = mapper.createObjectNode();
        for (int i = 1; i < elements.length; i++) {
            String position = String.format("%02d", i);
            node.put(position, elements[i].trim());
        }
        return node;
    }

    /**
     * Add segment to transaction, handling repeating segments as arrays.
     */
    private void addSegmentToTransaction(ObjectNode transaction, String segmentId, ObjectNode segmentNode) {
        if (transaction.has(segmentId)) {
            JsonNode existing = transaction.get(segmentId);
            if (existing.isArray()) {
                ((ArrayNode) existing).add(segmentNode);
            } else {
                ArrayNode array = mapper.createArrayNode();
                array.add(existing);
                array.add(segmentNode);
                transaction.set(segmentId, array);
            }
        } else {
            transaction.set(segmentId, segmentNode);
        }
    }

    /**
     * EDI delimiters holder.
     */
    private record EdiDelimiters(String elementSeparator, String segmentTerminator) {
        public String getElementSeparator() { return elementSeparator; }
        public String getSegmentTerminator() { return segmentTerminator; }
    }
}
