package com.example.edicleanarch.x12.edi315.domain.service.outbound;


/**
 * Result of outbound EDI 315 generation.
 */
public record GenerateEdi315Result(
        Status status,
        String fileName,
        int eventCount,
        String message,
        long processingTimeMs
) {
    public enum Status {
        SUCCESS,
        NO_EVENTS,
        SEND_FAILED,
        ERROR
    }

    public static GenerateEdi315Result success(String fileName, int eventCount, long processingTimeMs) {
        return new GenerateEdi315Result(Status.SUCCESS, fileName, eventCount,
                "Successfully sent " + eventCount + " events", processingTimeMs);
    }

    public static GenerateEdi315Result noEvents(long processingTimeMs) {
        return new GenerateEdi315Result(Status.NO_EVENTS, null, 0,
                "No pending events to send", processingTimeMs);
    }

    public static GenerateEdi315Result sendFailed(String fileName, String error, long processingTimeMs) {
        return new GenerateEdi315Result(Status.SEND_FAILED, fileName, 0,
                "Failed to send: " + error, processingTimeMs);
    }

    public static GenerateEdi315Result error(String error, long processingTimeMs) {
        return new GenerateEdi315Result(Status.ERROR, null, 0, error, processingTimeMs);
    }
}
