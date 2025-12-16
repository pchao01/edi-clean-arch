package com.example.edicleanarch.common.transform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default LookupService implementation.
 * Uses in-memory cache for lookups. Can be extended to use database lookups.
 */
@Slf4j
@Service
public class DefaultLookupService implements LookupService {

    private final Map<String, Map<String, Map<String, Object>>> lookupCache = new ConcurrentHashMap<>();

    @Override
    public Object lookup(String tableName, String key, String targetColumn) {
        Map<String, Map<String, Object>> table = lookupCache.get(tableName);
        if (table == null) {
            log.debug("Lookup table not found: {}", tableName);
            return null;
        }

        Map<String, Object> row = table.get(key);
        if (row == null) {
            log.debug("Lookup key not found: {} in table {}", key, tableName);
            return null;
        }

        return row.get(targetColumn);
    }

    /**
     * Register a lookup table for in-memory lookups.
     */
    public void registerLookupTable(String tableName, Map<String, Map<String, Object>> data) {
        lookupCache.put(tableName, data);
        log.info("Registered lookup table: {} with {} entries", tableName, data.size());
    }

    /**
     * Clear all lookup caches.
     */
    public void clearCache() {
        lookupCache.clear();
    }
}
