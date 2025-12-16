package com.example.edicleanarch.railinc.domain.service;


import com.example.edicleanarch.common.schema.FieldDefinition;
import com.example.edicleanarch.common.schema.FixedWidthSchema;
import jakarta.annotation.PostConstruct;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Loads Railinc CLM schema configuration from YAML file.
 *
 * Schema location: classpath:config/mappings/inbound/railinc-schema.yml
 */
@Slf4j
@Component

public class RailincSchemaLoader {
    @Value("${app.edi.railinc.schema-path:classpath:config/mappings/inbound/railinc-schema.yml}")
    private Resource schemaResource;

    private FixedWidthSchema cachedSchema;

    /**
     * Load schema on startup for validation.
     */
    @PostConstruct
    void init() {
        try {
            cachedSchema = loadSchemaFromYaml();
            log.info("Loaded Railinc schema: {} v{}", cachedSchema.getName(), cachedSchema.getVersion());
        } catch (Exception e) {
            log.error("Failed to load Railinc schema: {}", e.getMessage());
        }
    }

    /**
     * Get the loaded schema (cached).
     */
    public FixedWidthSchema loadSchema() {
        if (cachedSchema == null) {
            cachedSchema = loadSchemaFromYaml();
        }
        return cachedSchema;
    }

    /**
     * Reload schema from YAML (useful for hot-reload).
     */
    public FixedWidthSchema reloadSchema() {
        cachedSchema = loadSchemaFromYaml();
        log.info("Reloaded Railinc schema: {} v{}", cachedSchema.getName(), cachedSchema.getVersion());
        return cachedSchema;
    }

    /**
     * Load and parse schema from YAML file.
     */
    @SuppressWarnings("unchecked")
    private FixedWidthSchema loadSchemaFromYaml() {
        try (InputStream inputStream = schemaResource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(inputStream);

            FixedWidthSchema schema = new FixedWidthSchema();
            schema.setName((String) yamlMap.get("name"));
            schema.setVersion((String) yamlMap.get("version"));

            // Parse header fields
            List<Map<String, Object>> headerFieldsMap = (List<Map<String, Object>>) yamlMap.get("headerFields");
            schema.setHeaderFields(parseFieldDefinitions(headerFieldsMap));

            // Parse data fields
            List<Map<String, Object>> dataFieldsMap = (List<Map<String, Object>>) yamlMap.get("dataFields");
            schema.setDataFields(parseFieldDefinitions(dataFieldsMap));

            // Parse trailer fields
            List<Map<String, Object>> trailerFieldsMap = (List<Map<String, Object>>) yamlMap.get("trailerFields");
            schema.setTrailerFields(parseFieldDefinitions(trailerFieldsMap));

            return schema;

        } catch (Exception e) {
            log.error("Error loading schema from YAML: {}", e.getMessage());
            throw new RuntimeException("Failed to load Railinc schema", e);
        }
    }

    /**
     * Parse list of field definitions from YAML map.
     */
    private List<FieldDefinition> parseFieldDefinitions(List<Map<String, Object>> fieldsMap) {
        List<FieldDefinition> fields = new ArrayList<>();
        if (fieldsMap == null) {
            return fields;
        }

        for (Map<String, Object> fieldMap : fieldsMap) {
            FieldDefinition field = new FieldDefinition();
            field.setName((String) fieldMap.get("name"));
            field.setStart((Integer) fieldMap.get("start"));
            field.setEnd((Integer) fieldMap.get("end"));

            // Optional fields with defaults
            if (fieldMap.containsKey("trim")) {
                field.setTrim((Boolean) fieldMap.get("trim"));
            } else {
                field.setTrim(true); // default
            }

            if (fieldMap.containsKey("required")) {
                field.setRequired((Boolean) fieldMap.get("required"));
            }

            if (fieldMap.containsKey("description")) {
                field.setDescription((String) fieldMap.get("description"));
            }

            fields.add(field);
        }

        return fields;
    }

}
