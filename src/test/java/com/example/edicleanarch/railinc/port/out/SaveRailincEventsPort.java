package com.example.edicleanarch.railinc.port.out;

import com.example.edicleanarch.railinc.domain.model.ContainerEvent;

import java.util.List;
import java.util.Map;

/**
 * Output Port: Save Railinc Events
 */
public interface SaveRailincEventsPort {

    Map<String, Integer> saveEvents(List<ContainerEvent> events, String partnerId, String fileName);
}
