package com.example.edicleanarch.railinc.adapter.out;


import lombok.Data;

import java.time.LocalDateTime;

/**
 * JPA Entity for Railinc container events.
 */
@Data
class RailincEventEntity {
    private Long id;
    private String equipmentId;
    private String equipmentInitial;
    private String equipmentNumber;
    private String equipmentType;
    private String billOfLading;
    private String eventCode;
    private String eventCodeDesc;
    private String eventDateTime;
    private String etaDateTime;
    private String eventCity;
    private String eventState;
    private String eventSplc;
    private String originCity;
    private String originState;
    private String originSplc;
    private String destinationCity;
    private String destinationState;
    private String destinationSplc;
    private String loadEmptyStatus;
    private String trainSymbol;
    private String reportingRailroad;
    private String interlineRailroad;
    private String vesselEtaDateTime;
    private String portOfLoad;
    private String portOfDischarge;
    private String shipperCode;
    private String partnerId;
    private String fileName;
    private LocalDateTime createdDate;
}
