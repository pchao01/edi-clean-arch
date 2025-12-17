# EDI Clean Architecture System Diagrams

## 1. High-Level Architecture

```mermaid
flowchart TB
    subgraph External["External Systems"]
        KAFKA[("Kafka")]
        REST["REST API"]
        DB[("SQL Server<br/>CDB Database")]
    end

    subgraph LookupTables["Lookup Tables"]
        SCAC["SCACCODE"]
        OEC["OEVENTCODE"]
        LOC["CNTR_EVENT_LOCATENAME_MAP"]
    end

    subgraph Adapters["Adapters Layer"]
        subgraph Inbound["Inbound Adapters"]
            KC["Kafka Consumer<br/>@KafkaAdapter"]
            WC["Web Controller<br/>@WebAdapter"]
        end
        subgraph Outbound["Outbound Adapters"]
            PA["Persistence Adapter<br/>@PersistenceAdapter"]
        end
    end

    subgraph Domain["Domain Layer"]
        UC["Use Case Service<br/>@UseCase"]
        ME["Mapping Engine"]
        TF["Transform Functions"]
        LS["DatabaseLookupService"]
    end

    subgraph Config["Configuration"]
        YAML["YAML Mapping Files<br/>edi315-mapping.yml<br/>railinc-mapping.yml"]
    end

    KAFKA --> KC
    REST --> WC
    KC --> UC
    WC --> UC
    UC --> ME
    ME --> TF
    TF --> LS
    LS --> SCAC & OEC & LOC
    YAML -.-> ME
    UC --> PA
    PA --> DB
```

## 2. EDI 315 Processing Flow

```mermaid
sequenceDiagram
    participant K as Kafka
    participant KA as KafkaConsumer
    participant UC as ProcessEdi315Service
    participant P as X12ToJsonConverter
    participant ME as EdiMappingEngine
    participant TF as TransformFunctions
    participant PA as PersistenceAdapter
    participant DB as SQL Server

    K->>KA: EDI 315 Message
    KA->>UC: ProcessEdi315Command
    UC->>P: X12 Content
    P-->>UC: JsonNode
    UC->>ME: transform(JsonNode, config)
    ME->>TF: apply transforms
    TF-->>ME: transformed values
    ME-->>UC: MappingResult
    UC->>PA: saveRecords(records)
    PA->>DB: Dynamic INSERT
    DB-->>PA: Success
    PA-->>UC: Insert counts
    UC-->>KA: ProcessingResult
```

## 3. Clean Architecture Layers

```mermaid
flowchart LR
    subgraph External["External"]
        EXT1["Kafka"]
        EXT2["HTTP"]
        EXT3["Database"]
    end

    subgraph Inbound["Inbound Adapters"]
        IA1["ProcessEdi315KafkaConsumer"]
        IA2["GenerateEdi315WebController"]
        IA3["RailincKafkaConsumerAdapter"]
    end

    subgraph Ports_In["Ports (In)"]
        PI1["ProcessEdiFileUseCase"]
        PI2["GenerateEdi315UseCase"]
    end

    subgraph UseCases["Domain Services"]
        UC1["ProcessEdi315Service"]
        UC2["GenerateEdi315Service"]
        UC3["ProcessRailincService"]
    end

    subgraph Ports_Out["Ports (Out)"]
        PO1["SaveEdi315EventsPort"]
        PO2["SendEdiPort"]
        PO3["SaveRailincEventsPort"]
    end

    subgraph Outbound["Outbound Adapters"]
        OA1["Edi315PersistenceAdapter"]
        OA2["Edi315KafkaProducerAdapter"]
        OA3["RailincPersistenceAdapter"]
    end

    EXT1 --> IA1
    EXT2 --> IA2
    IA1 --> PI1
    IA2 --> PI2
    PI1 --> UC1
    PI2 --> UC2
    IA3 --> UC3
    UC1 --> PO1
    UC2 --> PO2
    UC3 --> PO3
    PO1 --> OA1
    PO2 --> OA2
    PO3 --> OA3
    OA1 --> EXT3
    OA3 --> EXT3
```

## 4. Config-Driven Mapping System

```mermaid
flowchart TB
    subgraph Input["Input Formats"]
        X12["X12 EDI<br/>(315, etc.)"]
        FW["Fixed-Width<br/>(Railinc)"]
    end

    subgraph Parsers["Parsers"]
        X12P["X12ToJsonConverter"]
        FWP["FixedWidthToJsonConverter"]
    end

    subgraph Intermediate["Intermediate Format"]
        JSON["JsonNode<br/>(Normalized Structure)"]
    end

    subgraph Config["YAML Configuration"]
        MC["MappingConfig"]
        FM["FieldMapping[]"]
        TT["TargetTableConfig"]
    end

    subgraph Engine["Mapping Engine"]
        ME["EdiMappingEngine"]
        TF["TransformFunctions"]
    end

    subgraph Transforms["Available Transforms"]
        T1["DIRECT"]
        T2["CONCAT"]
        T3["LOOKUP (with fallback)"]
        T4["QUALIFIED_SEGMENT"]
        T5["CONSTANT"]
        T6["CURRENT_TIMESTAMP"]
        T7["BUILD_DATETIME"]
        T8["TRIM_OR_NULL"]
        T9["COALESCE"]
        T10["BOOKNO_FLAG"]
        T11["ID_MAP_FLAG"]
    end

    subgraph Output["Output"]
        REC["Map&lt;String, Object&gt;<br/>Database Records"]
    end

    X12 --> X12P
    FW --> FWP
    X12P --> JSON
    FWP --> JSON
    JSON --> ME
    MC --> ME
    FM --> MC
    TT --> MC
    ME --> TF
    TF --> T1 & T2 & T3 & T4 & T5 & T6 & T7 & T8
    ME --> REC
```

## 5. Module Structure

```mermaid
flowchart TB
    subgraph Root["com.example.edicleanarch"]
        APP["EdiCleanArchApplication"]

        subgraph Common["common"]
            CA["adapter/in/kafka"]
            CM["mapping"]
            CP["parser"]
            CT["transform"]
            CPT["port/in, port/out"]
            CAN["annotation"]
        end

        subgraph X12["x12"]
            subgraph EDI315["edi315"]
                E_IN["adapter/in"]
                E_OUT["adapter/out"]
                E_DOM["domain/service"]
                E_PORT["port/in, port/out"]
            end
            X12C["common"]
        end

        subgraph Railinc["railinc"]
            R_IN["adapter/in"]
            R_OUT["adapter/out"]
            R_DOM["domain/service"]
            R_PORT["port/out"]
        end

        subgraph Config["config"]
            CFG["EdiKafkaConfig<br/>EdiModuleConfig<br/>JacksonConfig"]
        end
    end

    Common --> X12
    Common --> Railinc
    X12C --> EDI315
```

## 6. Data Transformation Pipeline

```mermaid
flowchart LR
    subgraph Stage1["Stage 1: Parse"]
        RAW["Raw EDI<br/>ISA*00*...~"]
        JSON1["JsonNode<br/>{envelope, transactions}"]
    end

    subgraph Stage2["Stage 2: Configure"]
        YAML["edi315-mapping.yml"]
        CFG["MappingConfig<br/>+ TargetTableConfig<br/>+ FieldMapping[]"]
    end

    subgraph Stage3["Stage 3: Transform"]
        ENG["EdiMappingEngine"]
        RES["MappingResult<br/>{table: [records]}"]
    end

    subgraph Stage4["Stage 4: Persist"]
        SQL["Dynamic SQL<br/>INSERT INTO CDB_EVENT..."]
        DB[("Database")]
    end

    RAW -->|X12ToJsonConverter| JSON1
    JSON1 --> ENG
    YAML -->|Edi315MappingConfigLoader| CFG
    CFG --> ENG
    ENG --> RES
    RES -->|PersistenceAdapter| SQL
    SQL --> DB
```

## 7. YAML Mapping Structure

```mermaid
classDiagram
    class MappingConfig {
        +String ediType
        +String sourceFormat
        +String version
        +List~ValidationRule~ validations
        +List~TargetTableConfig~ targets
        +Map~String,PartnerOverride~ partnerOverrides
    }

    class TargetTableConfig {
        +String table
        +String type
        +String loopPath
        +String condition
        +List~String~ parentKeys
        +List~FieldMapping~ fields
    }

    class FieldMapping {
        +String name
        +String source
        +String type
        +String transform
        +String format
        +String value
        +String condition
        +String lookupTable
        +String lookupKeyColumn
        +String lookupKeyExpr
        +String lookupColumn
        +String lookupCondition
        +String lookupFallbackCondition
        +List~String~ concatFields
        +List~SourceConfig~ sources
    }

    class SourceConfig {
        +String source
        +String transform
        +List~String~ concatFields
    }

    FieldMapping "1" --> "*" SourceConfig

    class ValidationRule {
        +String rule
        +String field
        +String message
        +List~String~ segments
    }

    MappingConfig "1" --> "*" TargetTableConfig
    MappingConfig "1" --> "*" ValidationRule
    TargetTableConfig "1" --> "*" FieldMapping
```
