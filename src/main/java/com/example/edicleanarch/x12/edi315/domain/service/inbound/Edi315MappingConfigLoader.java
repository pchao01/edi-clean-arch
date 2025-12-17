package com.example.edicleanarch.x12.edi315.domain.service.inbound;

import com.example.edicleanarch.common.mapping.FieldMapping;
import com.example.edicleanarch.common.mapping.MappingConfig;
import com.example.edicleanarch.common.mapping.TargetTableConfig;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Loads EDI 315 mapping configuration from YAML file.
 *
 * Config location: classpath:config/mappings/inbound/edi315-mapping.yml
 *
 * Fully dynamic - NO hardcoded fields.
 * Adding a new field only requires YAML changes.
 */
@Slf4j
@Component
public class Edi315MappingConfigLoader {

    @Value("${app.edi.edi315.mapping-path:classpath:config/mappings/inbound/edi315-mapping.yml}")
    private Resource mappingResource;

    private MappingConfig cachedConfig;

    @PostConstruct
    void init() {
        try {
            cachedConfig = loadConfigFromYaml();
            log.info("Loaded EDI 315 mapping config: {} v{}",
                    cachedConfig.getEdiType(), cachedConfig.getVersion());
        } catch (Exception e) {
            log.error("Failed to load EDI 315 mapping config: {}", e.getMessage());
        }
    }

    public MappingConfig loadConfig() {
        if (cachedConfig == null) {
            cachedConfig = loadConfigFromYaml();
        }
        return cachedConfig;
    }

    public MappingConfig reloadConfig() {
        cachedConfig = loadConfigFromYaml();
        log.info("Reloaded EDI 315 mapping config");
        return cachedConfig;
    }

    @SuppressWarnings("unchecked")
    private MappingConfig loadConfigFromYaml() {
        try (InputStream inputStream = mappingResource.getInputStream()) {
            Yaml yaml = new Yaml();
            Map<String, Object> yamlMap = yaml.load(inputStream);

            MappingConfig config = new MappingConfig();
            config.setEdiType((String) yamlMap.get("ediType"));
            config.setDescription((String) yamlMap.get("description"));
            config.setSourceFormat((String) yamlMap.get("sourceFormat"));
            config.setVersion((String) yamlMap.get("version"));

            // Parse validations
            List<Map<String, Object>> validationsMap =
                    (List<Map<String, Object>>) yamlMap.get("validations");
            config.setValidations(parseValidations(validationsMap));

            // Parse targets
            List<Map<String, Object>> targetsMap =
                    (List<Map<String, Object>>) yamlMap.get("targets");
            config.setTargets(parseTargets(targetsMap));

            // Parse partner overrides
            Map<String, Object> overridesMap =
                    (Map<String, Object>) yamlMap.get("partnerOverrides");
            config.setPartnerOverrides(parsePartnerOverrides(overridesMap));

            return config;

        } catch (Exception e) {
            log.error("Error loading mapping config from YAML: {}", e.getMessage());
            throw new RuntimeException("Failed to load EDI 315 mapping config", e);
        }
    }

    @SuppressWarnings("unchecked")
    private List<MappingConfig.ValidationRule> parseValidations(List<Map<String, Object>> list) {
        List<MappingConfig.ValidationRule> rules = new ArrayList<>();
        if (list == null) return rules;

        for (Map<String, Object> map : list) {
            MappingConfig.ValidationRule rule = new MappingConfig.ValidationRule();
            rule.setRule((String) map.get("rule"));
            rule.setField((String) map.get("field"));
            rule.setExpectedValue((String) map.get("expectedValue"));
            rule.setMessage((String) map.get("message"));
            rule.setActualField((String) map.get("actualField"));
            rule.setExpectedField((String) map.get("expectedField"));
            rule.setSegments((List<String>) map.get("segments"));
            rule.setFields((List<String>) map.get("fields"));
            rules.add(rule);
        }
        return rules;
    }

    @SuppressWarnings("unchecked")
    private List<TargetTableConfig> parseTargets(List<Map<String, Object>> list) {
        List<TargetTableConfig> targets = new ArrayList<>();
        if (list == null) return targets;

        for (Map<String, Object> map : list) {
            TargetTableConfig target = new TargetTableConfig();
            target.setTable((String) map.get("table"));
            target.setType((String) map.get("type"));
            target.setLoopPath((String) map.get("loopPath"));
            target.setCondition((String) map.get("condition"));
            target.setDescription((String) map.get("description"));
            target.setParentKeys((List<String>) map.get("parentKeys"));

            List<Map<String, Object>> fieldsMap = (List<Map<String, Object>>) map.get("fields");
            target.setFields(parseFieldMappings(fieldsMap));

            targets.add(target);
        }
        return targets;
    }

    @SuppressWarnings("unchecked")
    private List<FieldMapping> parseFieldMappings(List<Map<String, Object>> list) {
        List<FieldMapping> fields = new ArrayList<>();
        if (list == null) return fields;

        for (Map<String, Object> map : list) {
            FieldMapping field = new FieldMapping();
            field.setName((String) map.get("name"));
            field.setSource((String) map.get("source"));
            field.setType((String) map.get("type"));
            field.setRequired(Boolean.TRUE.equals(map.get("required")));
            field.setTransform((String) map.get("transform"));
            field.setValue((String) map.get("value"));
            field.setConcatWith((String) map.get("concatWith"));
            field.setConcatFields((List<String>) map.get("concatFields"));
            field.setSourceFields((Map<String, String>) map.get("sourceFields"));
            field.setFormat((String) map.get("format"));
            field.setLookupTable((String) map.get("lookupTable"));
            field.setLookupKeyColumn((String) map.get("lookupKeyColumn"));
            field.setLookupKeyExpr((String) map.get("lookupKeyExpr"));
            field.setLookupColumn((String) map.get("lookupColumn"));
            field.setLookupCondition((String) map.get("lookupCondition"));
            field.setLookupFallbackCondition((String) map.get("lookupFallbackCondition"));
            field.setCondition((String) map.get("condition"));
            field.setSources(parseSourceConfigs((List<Map<String, Object>>) map.get("sources")));
            fields.add(field);
        }
        return fields;
    }

    @SuppressWarnings("unchecked")
    private List<FieldMapping.SourceConfig> parseSourceConfigs(List<Map<String, Object>> list) {
        if (list == null) return null;
        List<FieldMapping.SourceConfig> sources = new ArrayList<>();
        for (Map<String, Object> map : list) {
            FieldMapping.SourceConfig config = new FieldMapping.SourceConfig();
            config.setSource((String) map.get("source"));
            config.setTransform((String) map.get("transform"));
            config.setConcatFields((List<String>) map.get("concatFields"));
            sources.add(config);
        }
        return sources;
    }

    @SuppressWarnings("unchecked")
    private Map<String, MappingConfig.PartnerOverride> parsePartnerOverrides(Map<String, Object> map) {
        Map<String, MappingConfig.PartnerOverride> overrides = new HashMap<>();
        if (map == null) return overrides;

        for (Map.Entry<String, Object> entry : map.entrySet()) {
            Map<String, Object> overrideMap = (Map<String, Object>) entry.getValue();

            MappingConfig.PartnerOverride override = new MappingConfig.PartnerOverride();

            List<Map<String, Object>> fieldOverrides =
                    (List<Map<String, Object>>) overrideMap.get("fieldOverrides");
            override.setFieldOverrides(parseFieldMappings(fieldOverrides));

            override.setSchemaOverrides((Map<String, Object>) overrideMap.get("schemaOverrides"));

            overrides.put(entry.getKey(), override);
        }
        return overrides;
    }
}
