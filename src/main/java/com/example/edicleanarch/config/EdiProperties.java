package com.example.edicleanarch.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "edi")
public class EdiProperties {

    private RailincProperties railinc = new RailincProperties();
    private Edi315Properties edi315 = new Edi315Properties();
    private KafkaProperties kafka = new KafkaProperties();

    @Data
    public static class RailincProperties {
        private boolean enabled = true;
        private KafkaTopicProperties kafka = new KafkaTopicProperties();

        @Data
        public static class KafkaTopicProperties {
            private String topic = "railinc-inbound";
            private String groupId = "railinc-processor-group";
            private int concurrency = 1;
        }
    }

    @Data
    public static class Edi315Properties {
        private boolean enabled = true;
        private KafkaTopicProperties kafka = new KafkaTopicProperties();

        @Data
        public static class KafkaTopicProperties {
            private String topic = "edi315-inbound";
            private String groupId = "edi315-processor-group";
            private int concurrency = 1;
        }
    }

    @Data
    public static class KafkaProperties {
        private String autoOffsetReset = "earliest";
        private boolean enableAutoCommit = false;
    }
}
