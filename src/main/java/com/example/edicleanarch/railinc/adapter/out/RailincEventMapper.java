package com.example.edicleanarch.railinc.adapter.out;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

import java.util.Map;

@Mapper
interface RailincEventMapper {

    @Insert("""
            INSERT INTO T_CONTAINER_EVENT (
                EQUIPMENT_INITIAL, EQUIPMENT_NUMBER, EQUIPMENT_CHECK_DIGIT,
                MBL_NO, SCAC, EVENT_TYPE_CODE,
                SIGHTING_DATE_TIME, ETA_DATE_TIME,
                SIGHTING_CITY, SIGHTING_STATE, SIGHTING_SPLC,
                DESTINATION_CITY, DESTINATION_STATE, DESTINATION_SPLC,
                CURRENT_LOCATION_UNLOCDE, DESTINATION_LOCATION_UNLOCDE,
                LOAD_EMPTY_STATUS, TRAIN_ID,
                HEADER_RAILROAD_SCAC, REPORTING_RAILROAD_SCAC,
                PARTNER_ID, FILE_NAME, CREATED_DATE
            ) VALUES (
                #{equipmentInitial}, #{equipmentNumber}, #{equipmentCheckDigit},
                #{mblNo}, #{scac}, #{eventTypeCode},
                #{sightingDateTime}, #{etaDateTime},
                #{sightingCity}, #{sightingState}, #{sightingSplc},
                #{destinationCity}, #{destinationState}, #{destinationSplc},
                #{currentLocationUnlocde}, #{destinationLocationUnlocde},
                #{loadEmptyStatus}, #{trainId},
                #{headerRailroadScac}, #{reportingRailroadScac},
                #{partnerId}, #{sourceFileName}, #{createdDate}
            )
            """)
    void insertRecord(Map<String, Object> record);
}
