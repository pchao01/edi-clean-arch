package com.example.edicleanarch.common.transform;

import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Database-backed LookupService implementation with caching.
 * Queries the database for lookup values and caches results to avoid repeated queries.
 */
@Slf4j
@Service
public class DatabaseLookupService implements LookupService {

    private final NamedParameterJdbcTemplate jdbcTemplate;

    /**
     * Cache structure: tableName -> keyColumn -> keyValue -> columnName -> value
     */
    private final Map<String, Map<String, Map<String, Map<String, Object>>>> cache = new ConcurrentHashMap<>();

    public DatabaseLookupService(NamedParameterJdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public Object lookup(String tableName, String keyColumn, String keyValue, String targetColumn) {
        if (tableName == null || keyColumn == null || keyValue == null || targetColumn == null) {
            log.warn("Lookup skipped - null parameter: table={}, keyColumn={}, keyValue={}, targetColumn={}",
                    tableName, keyColumn, keyValue, targetColumn);
            return null;
        }

        // Trim key value to handle padded EDI fields
        keyValue = keyValue.trim();

        // Check cache first
        Object cachedValue = getCachedValue(tableName, keyColumn, keyValue, targetColumn);
        if (cachedValue != null) {
            log.trace("Cache hit: {}.{} where {}={}", tableName, targetColumn, keyColumn, keyValue);
            return cachedValue;
        }

        // Query database
        try {
            String sql = String.format("SELECT %s FROM %s WHERE %s = :keyValue",
                    targetColumn, tableName, keyColumn);

            MapSqlParameterSource params = new MapSqlParameterSource("keyValue", keyValue);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, params);

            if (results.isEmpty()) {
                log.info("Lookup not found: {}.{} where {}='{}' (no matching row)", tableName, targetColumn, keyColumn, keyValue);
                // Cache the miss as well to avoid repeated queries
                cacheValue(tableName, keyColumn, keyValue, targetColumn, NullMarker.INSTANCE);
                return null;
            }

            Object value = results.get(0).get(targetColumn);
            log.info("Lookup found: {}.{} where {}='{}' => {}", tableName, targetColumn, keyColumn, keyValue, value);

            // Cache the result
            cacheValue(tableName, keyColumn, keyValue, targetColumn, value);

            return value;

        } catch (Exception e) {
            log.error("Lookup failed: {}.{} where {}={}: {}",
                    tableName, targetColumn, keyColumn, keyValue, e.getMessage());
            return null;
        }
    }

    /**
     * Get cached value if present.
     */
    private Object getCachedValue(String tableName, String keyColumn, String keyValue, String targetColumn) {
        Map<String, Map<String, Map<String, Object>>> tableCache = cache.get(tableName);
        if (tableCache == null) return null;

        Map<String, Map<String, Object>> keyColumnCache = tableCache.get(keyColumn);
        if (keyColumnCache == null) return null;

        Map<String, Object> rowCache = keyColumnCache.get(keyValue);
        if (rowCache == null) return null;

        Object value = rowCache.get(targetColumn);
        if (value instanceof NullMarker) return null;

        return value;
    }

    /**
     * Cache a lookup result.
     */
    private void cacheValue(String tableName, String keyColumn, String keyValue, String targetColumn, Object value) {
        cache.computeIfAbsent(tableName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(keyColumn, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(keyValue, k -> new ConcurrentHashMap<>())
                .put(targetColumn, value != null ? value : NullMarker.INSTANCE);
    }

    /**
     * Clear all cached lookup values.
     */
    public void clearCache() {
        cache.clear();
        log.info("Lookup cache cleared");
    }

    /**
     * Clear cached values for a specific table.
     */
    public void clearCache(String tableName) {
        cache.remove(tableName);
        log.info("Lookup cache cleared for table: {}", tableName);
    }

    /**
     * Cache for condition-based lookups: tableName -> condition -> columnName -> value
     */
    private final Map<String, Map<String, Map<String, Object>>> conditionCache = new ConcurrentHashMap<>();

    @Override
    public Object lookupWithCondition(String tableName, String whereCondition, String targetColumn) {
        if (tableName == null || whereCondition == null || targetColumn == null) {
            log.warn("Lookup skipped - null parameter: table={}, condition={}, targetColumn={}",
                    tableName, whereCondition, targetColumn);
            return null;
        }

        // Check condition cache first
        Object cachedValue = getConditionCachedValue(tableName, whereCondition, targetColumn);
        if (cachedValue != null) {
            if (cachedValue instanceof NullMarker) return null;
            log.trace("Cache hit: {}.{} where {}", tableName, targetColumn, whereCondition);
            return cachedValue;
        }

        // Query database
        try {
            String sql = String.format("SELECT %s FROM %s WHERE %s",
                    targetColumn, tableName, whereCondition);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql, new MapSqlParameterSource());

            if (results.isEmpty()) {
                log.info("Lookup not found: {}.{} where {} (no matching row)", tableName, targetColumn, whereCondition);
                cacheConditionValue(tableName, whereCondition, targetColumn, NullMarker.INSTANCE);
                return null;
            }

            Object value = results.get(0).get(targetColumn);
            log.info("Lookup found: {}.{} where {} => {}", tableName, targetColumn, whereCondition, value);

            cacheConditionValue(tableName, whereCondition, targetColumn, value);
            return value;

        } catch (Exception e) {
            log.error("Lookup failed: {}.{} where {}: {}",
                    tableName, targetColumn, whereCondition, e.getMessage());
            return null;
        }
    }

    private Object getConditionCachedValue(String tableName, String condition, String targetColumn) {
        Map<String, Map<String, Object>> tableCache = conditionCache.get(tableName);
        if (tableCache == null) return null;

        Map<String, Object> condCache = tableCache.get(condition);
        if (condCache == null) return null;

        return condCache.get(targetColumn);
    }

    private void cacheConditionValue(String tableName, String condition, String targetColumn, Object value) {
        conditionCache.computeIfAbsent(tableName, k -> new ConcurrentHashMap<>())
                .computeIfAbsent(condition, k -> new ConcurrentHashMap<>())
                .put(targetColumn, value != null ? value : NullMarker.INSTANCE);
    }

    /**
     * Marker class to distinguish cached null values from cache misses.
     */
    private static class NullMarker {
        static final NullMarker INSTANCE = new NullMarker();
    }
}
