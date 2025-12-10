package com.example.edicleanarch.common.model;

import lombok.Getter;

import java.util.Collections;
import java.util.List;
import java.util.Map;

@Getter
public class ProcessingResult {
    public enum Status {
        SUCCESS,
        VALIDATION_FAILED,
        PARTIAL_SUCCESS,
        ERROR
    }
    private final Status status;
    private final String messageType;      // "315", "RAILINC", "300", "310"
    private final String fileName;
    private final String partnerId;
    private final int recordCount;
    private final int successCount;
    private final int failedCount;
    private final Map<String, Integer> insertCounts;
    private final List<String> validationErrors;
    private final List<RecordError> recordErrors;
    private final String errorMessage;
    private final long durationMs;
    private ProcessingResult(Builder builder) {
        this.status = builder.status;
        this.messageType = builder.messageType;
        this.fileName = builder.fileName;
        this.partnerId = builder.partnerId;
        this.recordCount = builder.recordCount;
        this.successCount = builder.successCount;
        this.failedCount = builder.failedCount;
        this.insertCounts = builder.insertCounts != null ? builder.insertCounts : Collections.emptyMap();
        this.validationErrors = builder.validationErrors != null ? builder.validationErrors : Collections.emptyList();
        this.recordErrors = builder.recordErrors != null ? builder.recordErrors : Collections.emptyList();
        this.errorMessage = builder.errorMessage;
        this.durationMs = builder.durationMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static ProcessingResult success(
            String messageType,
            String fileName,
            String partnerId,
            int recordCount,
            Map<String, Integer> insertCounts,
            long durationMs) {
        return builder()
                .status(Status.SUCCESS)
                .messageType(messageType)
                .fileName(fileName)
                .partnerId(partnerId)
                .recordCount(recordCount)
                .successCount(recordCount)
                .insertCounts(insertCounts)
                .durationMs(durationMs)
                .build();
    }

    public static ProcessingResult partialSuccess(
            String messageType,
            String fileName,
            String partnerId,
            int recordCount,
            int successCount,
            int failedCount,
            Map<String, Integer> insertCounts,
            List<RecordError> recordErrors,
            long durationMs) {
        return builder()
                .status(Status.PARTIAL_SUCCESS)
                .messageType(messageType)
                .fileName(fileName)
                .partnerId(partnerId)
                .recordCount(recordCount)
                .successCount(successCount)
                .failedCount(failedCount)
                .insertCounts(insertCounts)
                .recordErrors(recordErrors)
                .durationMs(durationMs)
                .build();
    }

    public static ProcessingResult validationFailed(
            String messageType,
            String fileName,
            String partnerId,
            List<String> validationErrors,
            long durationMs) {
        return builder()
                .status(Status.VALIDATION_FAILED)
                .messageType(messageType)
                .fileName(fileName)
                .partnerId(partnerId)
                .validationErrors(validationErrors)
                .durationMs(durationMs)
                .build();
    }

    public static ProcessingResult error(
            String messageType,
            String fileName,
            String partnerId,
            String errorMessage,
            long durationMs) {
        return builder()
                .status(Status.ERROR)
                .messageType(messageType)
                .fileName(fileName)
                .partnerId(partnerId)
                .errorMessage(errorMessage)
                .durationMs(durationMs)
                .build();
    }

    public boolean isSuccess() {
        return status == Status.SUCCESS;
    }

    public boolean isPartialSuccess() {
        return status == Status.PARTIAL_SUCCESS;
    }

    public boolean hasErrors() {
        return status == Status.ERROR || status == Status.VALIDATION_FAILED;
    }

    /**
     * Value Object: Record-level Error
     */
    public record RecordError(
            String recordId,
            String errorDescription,
            String rawContent
    ) {}

    /**
     * Builder for ProcessingResult
     */
    public static class Builder {
        private Status status;
        private String messageType;
        private String fileName;
        private String partnerId;
        private int recordCount;
        private int successCount;
        private int failedCount;
        private Map<String, Integer> insertCounts;
        private List<String> validationErrors;
        private List<RecordError> recordErrors;
        private String errorMessage;
        private long durationMs;

        public Builder status(Status status) { this.status = status; return this; }
        public Builder messageType(String messageType) { this.messageType = messageType; return this; }
        public Builder fileName(String fileName) { this.fileName = fileName; return this; }
        public Builder partnerId(String partnerId) { this.partnerId = partnerId; return this; }
        public Builder recordCount(int recordCount) { this.recordCount = recordCount; return this; }
        public Builder successCount(int successCount) { this.successCount = successCount; return this; }
        public Builder failedCount(int failedCount) { this.failedCount = failedCount; return this; }
        public Builder insertCounts(Map<String, Integer> insertCounts) { this.insertCounts = insertCounts; return this; }
        public Builder validationErrors(List<String> validationErrors) { this.validationErrors = validationErrors; return this; }
        public Builder recordErrors(List<RecordError> recordErrors) { this.recordErrors = recordErrors; return this; }
        public Builder errorMessage(String errorMessage) { this.errorMessage = errorMessage; return this; }
        public Builder durationMs(long durationMs) { this.durationMs = durationMs; return this; }

        public ProcessingResult build() {
            return new ProcessingResult(this);
        }
    }
}
