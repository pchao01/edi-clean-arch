# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build and Test Commands

```bash
# Build
./gradlew build

# Run tests
./gradlew test

# Run a single test class
./gradlew test --tests "com.example.edicleanarch.edi315.integration.Edi315SqlServerIntegrationTest"

# Compile without tests
./gradlew compileJava compileTestJava
```

## Architecture Overview

This is a Spring Boot 4.0 application implementing **Clean Architecture** for EDI (Electronic Data Interchange) file processing. The system processes X12 EDI messages (e.g., EDI 315) and fixed-width formats (Railinc) using a **config-driven mapping approach**.

### Clean Architecture Layers

```
Inbound Adapters (Kafka/Web) → Use Cases (Domain Services) → Outbound Adapters (Persistence)
```

- **Inbound Adapters** (`adapter/in/`): Kafka consumers and REST controllers that receive EDI files
- **Domain Services** (`domain/service/`): Business logic orchestrating parsing, mapping, and persistence
- **Outbound Adapters** (`adapter/out/`): Database persistence using dynamic SQL generation
- **Ports** (`port/in/`, `port/out/`): Interfaces defining boundaries between layers

### Config-Driven Mapping System

The core innovation is that **field mappings are defined in YAML**, not Java code:

1. **Parsers** (`X12ToJsonConverter`, `FixedWidthToJsonConverter`): Convert EDI to intermediate JsonNode
2. **Mapping Config** (`src/main/resources/config/mappings/`): YAML files define field transformations
3. **EdiMappingEngine**: Applies transformations from config to produce database records
4. **Transform Functions** (`TransformFunctions.java`): Pluggable transformation registry (DIRECT, CONCAT, LOOKUP, QUALIFIED_SEGMENT, etc.)

**To add a new field**: Only modify the YAML mapping file (e.g., `edi315-mapping.yml`). No Java changes needed.

### Module Structure

- `common/`: Shared infrastructure (mapping engine, parsers, transforms, annotations)
- `x12/edi315/`: EDI 315 Status Details processing
- `x12/common/`: Shared X12 utilities
- `railinc/`: Fixed-width Railinc file processing

### Key Annotations

Custom stereotype annotations mark architectural components:
- `@UseCase`: Domain service implementing a use case
- `@PersistenceAdapter`: Outbound adapter for database operations
- `@KafkaAdapter`, `@WebAdapter`: Inbound adapter markers

### Transform Functions

Available transforms in `TransformFunctions.java`:

| Transform | Description | Example |
|-----------|-------------|---------|
| `DIRECT` | Direct value extraction (default) | `source: "B4.03"` |
| `CONSTANT` | Return constant value | `value: "A"` |
| `CONCAT` | Concatenate fields | `concatFields: ["B4.07", "B4.08"]` |
| `COALESCE` | First non-empty from multiple sources | `sources: [{source: "N9[01=BM].02", transform: QUALIFIED_SEGMENT}]` |
| `QUALIFIED_SEGMENT` | Extract from qualified segment | `source: "N9[01=BM].02"` |
| `LOOKUP` | Database lookup with fallback | See below |
| `BUILD_DATETIME` | Build datetime from components | `sourceFields: {century, year, month, day, hour}` |
| `TRIM_OR_NULL` | Trim and return null if empty | |
| `BOOKNO_FLAG` | "" if N9_BM exists, "X" if N9_BN | EDI 315 specific |
| `ID_MAP_FLAG` | Map R4.01 to D/T flag | EDI 315 specific |

### Database Lookup Configuration

Lookups support multi-column conditions with fallback:

```yaml
# Single-column lookup
- name: SCAC
  transform: LOOKUP
  lookupTable: SCACCODE
  lookupKeyColumn: sender
  lookupKeyExpr: "${envelope.ISA.06}"
  lookupColumn: code

# Multi-column lookup with fallback
- name: EVENT_LOC_NAME
  transform: LOOKUP
  lookupTable: CNTR_EVENT_LOCATENAME_MAP
  lookupCondition: "SCAC_CD = 'RRDC' AND LOCATE_TYPE = 'UN' AND EVENT_LOCATE = '${currentLocationUnlocde}'"
  lookupFallbackCondition: "SCAC_CD = '' AND LOCATE_TYPE = 'UN' AND EVENT_LOCATE = '${currentLocationUnlocde}'"
  lookupColumn: EVENT_LOCATE_NAME
```

### Lookup Tables

| Table | Used By | Purpose |
|-------|---------|---------|
| `SCACCODE` | EDI 315 | ISA sender ID to SCAC code mapping |
| `OEVENTCODE` | EDI 315, Railinc | Partner event code to OEC event code/description |
| `CNTR_EVENT_LOCATENAME_MAP` | Railinc | UN/LOCODE to location name mapping |

### Mapping Configuration Example

```yaml
# edi315-mapping.yml
targets:
  - table: CDB_EVENT
    type: DETAIL
    loopPath: R4
    fields:
      - name: PRTNR_EVENT_CD
        source: "B4.03"
        type: STRING

      - name: MBL_NO               # COALESCE: N9_BM or N9_BN
        transform: COALESCE
        sources:
          - source: "N9[01=BM].02"
            transform: QUALIFIED_SEGMENT
          - source: "N9[01=BN].02"
            transform: QUALIFIED_SEGMENT

      - name: EVENT_CODE           # Database lookup
        transform: LOOKUP
        lookupTable: OEVENTCODE
        lookupCondition: "SCAC_CD = '${scac}' AND PRTNR_EVENT_CD = '${PRTNR_EVENT_CD}' AND DATE_TYPE = 'A'"
        lookupColumn: OEC_EVENT_CD
```

### Database

- Production: SQL Server (CDB database)
- Tests: SQL Server with test profile
- Persistence: NamedParameterJdbcTemplate for dynamic SQL with caching for lookups
