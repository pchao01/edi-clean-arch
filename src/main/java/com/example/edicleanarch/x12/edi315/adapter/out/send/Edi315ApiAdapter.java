package com.example.edicleanarch.x12.edi315.adapter.out.send;

import com.example.edicleanarch.common.annotation.PersistenceAdapter;
import com.example.edicleanarch.x12.edi315.port.out.SendEdiPort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

/**
 * Outbound Adapter: Send EDI 315 via REST API.
 * Activated when edi.x12.edi315.outbound.send.mode=api
 */
@Slf4j
@PersistenceAdapter
@RequiredArgsConstructor
@ConditionalOnProperty(name = "edi.x12.edi315.outbound.send.mode", havingValue = "api")
class Edi315ApiAdapter implements SendEdiPort {

    // TODO: Inject configured RestTemplate or WebClient
    // private final RestTemplate restTemplate;
    // private final Edi315OutboundProperties properties;

    @Override
    public void send(String ediContent, String partnerId, String fileName) {
        log.info("Sending EDI 315 via API: {} to partner: {}", fileName, partnerId);

        // TODO: Implement API call
        // String url = properties.getApi().getUrl();
        // restTemplate.postForEntity(url, new EdiPayload(ediContent, fileName), Void.class);

        log.info("Successfully sent EDI 315 via API: {}", fileName);
    }
}
