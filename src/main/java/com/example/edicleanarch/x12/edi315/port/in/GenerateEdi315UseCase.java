package com.example.edicleanarch.x12.edi315.port.in;


import com.example.edicleanarch.x12.edi315.domain.service.outbound.GenerateEdi315Command;
import com.example.edicleanarch.x12.edi315.domain.service.outbound.GenerateEdi315Result;

/**
 * Input Port: Generate and send EDI 315 outbound.
 * Triggered by REST API or Kafka consumer.
 */
public interface GenerateEdi315UseCase {

    /**
     * Generate EDI 315 content and send to partner.
     *
     * @param command The generation command
     * @return Generation result
     */
    GenerateEdi315Result generate(GenerateEdi315Command command);
}
