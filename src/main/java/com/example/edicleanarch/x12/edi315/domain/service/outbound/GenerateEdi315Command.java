package com.example.edicleanarch.x12.edi315.domain.service.outbound;

import java.util.List;

/**
 * Command for generating outbound EDI 315.
 */
public record GenerateEdi315Command(
        String scac,
        String partnerId,
        List<Long> eventIds    // Optional: specific events to send. If empty, find pending.
) {
    /**
     * Create command for specific events.
     */
    public static GenerateEdi315Command forEvents(String scac, String partnerId, List<Long> eventIds) {
        return new GenerateEdi315Command(scac, partnerId, eventIds);
    }

    /**
     * Create command to find and send pending events.
     */
    public static GenerateEdi315Command forPending(String scac, String partnerId) {
        return new GenerateEdi315Command(scac, partnerId, List.of());
    }

    public boolean hasSpecificEvents() {
        return eventIds != null && !eventIds.isEmpty();
    }
}
