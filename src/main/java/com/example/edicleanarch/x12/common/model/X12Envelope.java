package com.example.edicleanarch.x12.common.model;


/**
 * Shared X12 envelope structure for all message types (300, 310, 315, etc.)
 */
public record X12Envelope(
        String senderId,
        String receiverId,
        String isaControlNumber,
        String gsControlNumber,
        String date,
        String time,
        String version
) {
    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String senderId;
        private String receiverId;
        private String isaControlNumber;
        private String gsControlNumber;
        private String date;
        private String time;
        private String version;

        public Builder senderId(String senderId) { this.senderId = senderId; return this; }
        public Builder receiverId(String receiverId) { this.receiverId = receiverId; return this; }
        public Builder isaControlNumber(String isaControlNumber) { this.isaControlNumber = isaControlNumber; return this; }
        public Builder gsControlNumber(String gsControlNumber) { this.gsControlNumber = gsControlNumber; return this; }
        public Builder date(String date) { this.date = date; return this; }
        public Builder time(String time) { this.time = time; return this; }
        public Builder version(String version) { this.version = version; return this; }

        public X12Envelope build() {
            return new X12Envelope(senderId, receiverId, isaControlNumber, gsControlNumber, date, time, version);
        }
    }
}
