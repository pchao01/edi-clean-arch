package com.example.edicleanarch.x12.edi315.domain.service.inbound;

import com.example.edicleanarch.common.annotation.UseCase;
import com.example.edicleanarch.common.mapping.EdiMappingEngine;
import com.example.edicleanarch.common.mapping.MappingConfig;
import com.example.edicleanarch.common.mapping.MappingResult;
import com.example.edicleanarch.common.mapping.ProcessingContext;
import com.example.edicleanarch.common.model.ProcessingResult;
import com.example.edicleanarch.common.parser.X12ToJsonConverter;
import com.example.edicleanarch.common.port.in.ProcessEdiFileUseCase;
import com.example.edicleanarch.x12.edi315.port.out.SaveEdi315EventsPort;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Domain Service: Process EDI 315 File (Fully Dynamic / Config-Driven)
 *
 * NO hardcoded field mappings.
 * All field definitions come from edi315-mapping.yml.
 *
 * Flow:
 * 1. Convert X12 EDI to JsonNode (intermediate format)
 * 2. Load mapping configuration from YAML
 * 3. Transform JsonNode to database records using EdiMappingEngine
 * 4. Save records via dynamic outbound adapter
 *
 * Adding a new field:
 * 1. Add to edi315-mapping.yml (field transformation)
 * 2. NO Java code changes needed!
 */
@Slf4j
@UseCase
@RequiredArgsConstructor
@Transactional
public class ProcessEdi315Service implements ProcessEdiFileUseCase<ProcessEdi315Command> {

    private static final String MESSAGE_TYPE = "315";

    private final X12ToJsonConverter x12Converter;
    private final EdiMappingEngine mappingEngine;
    private final Edi315MappingConfigLoader mappingConfigLoader;
    private final SaveEdi315EventsPort saveEventsPort;

    @Override
    public ProcessingResult processFile(ProcessEdi315Command command) {
        log.info("Processing EDI 315 file: {} for partner: {}",
                command.fileName(), command.partnerId());

        long startTime = System.currentTimeMillis();

        try {
            // 1. Convert X12 EDI to JsonNode
            JsonNode ediJson = x12Converter.convert(command.content());
            int transactionCount = ediJson.get("transactions").size();
            log.debug("Converted EDI 315 to JsonNode: {} transactions", transactionCount);

            // 2. Load mapping configuration from YAML
            MappingConfig config = mappingConfigLoader.loadConfig();

            // 3. Create processing context
            ProcessingContext context = new ProcessingContext();
            context.setPartnerId(command.partnerId());
            context.setFileName(command.fileName());
            context.setEdiType("EDI_315");

            // 4. Transform using mapping engine
            MappingResult mappingResult = mappingEngine.transform(
                    ediJson, config, command.partnerId(), context);

            if (!mappingResult.isSuccess()) {
                return ProcessingResult.validationFailed(MESSAGE_TYPE, command.fileName(),
                        command.partnerId(), mappingResult.getErrors(),
                        System.currentTimeMillis() - startTime);
            }

            // 5. Save to database (fully dynamic)
            List<Map<String, Object>> cdbEvents = mappingResult.getRecords("CDB_EVENT");
            Map<String, Integer> insertCounts = saveEventsPort.saveRecords(cdbEvents, command.fileName());

            log.info("Processed EDI 315 file {}: {} records saved",
                    command.fileName(), mappingResult.getTotalRecords());

            return ProcessingResult.success(MESSAGE_TYPE, command.fileName(),
                    command.partnerId(), mappingResult.getTotalRecords(), insertCounts,
                    System.currentTimeMillis() - startTime);

        } catch (Exception e) {
            log.error("Error processing EDI 315 file: {}", command.fileName(), e);
            return ProcessingResult.error(MESSAGE_TYPE, command.fileName(),
                    command.partnerId(), e.getMessage(),
                    System.currentTimeMillis() - startTime);
        }
    }
}
