package com.example.edicleanarch.x12.edi315.adapter.in.outbound;

import com.example.edicleanarch.common.annotation.WebAdapter;
import com.example.edicleanarch.x12.edi315.domain.service.outbound.GenerateEdi315Command;
import com.example.edicleanarch.x12.edi315.domain.service.outbound.GenerateEdi315Result;
import com.example.edicleanarch.x12.edi315.port.in.GenerateEdi315UseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Inbound Adapter: REST API trigger for EDI 315 outbound generation.
 */
@WebAdapter
@RestController
@RequestMapping("/api/v1/edi315/outbound")
@RequiredArgsConstructor
class GenerateEdi315WebController {

    private final GenerateEdi315UseCase generateEdi315UseCase;

    /**
     * Send specific events.
     */
    @PostMapping("/send")
    ResponseEntity<GenerateEdi315Result> sendEvents(@RequestBody SendEventsRequest request) {
        GenerateEdi315Command command = GenerateEdi315Command.forEvents(
                request.scac(), request.partnerId(), request.eventIds());
        return ResponseEntity.ok(generateEdi315UseCase.generate(command));
    }

    /**
     * Send all pending events for a SCAC.
     */
    @PostMapping("/send-pending")
    ResponseEntity<GenerateEdi315Result> sendPending(@RequestBody SendPendingRequest request) {
        GenerateEdi315Command command = GenerateEdi315Command.forPending(
                request.scac(), request.partnerId());
        return ResponseEntity.ok(generateEdi315UseCase.generate(command));
    }

    record SendEventsRequest(String scac, String partnerId, List<Long> eventIds) {}
    record SendPendingRequest(String scac, String partnerId) {}
}
