package com.example.edicleanarch.common.transform;


/**
 * Interface for database lookups during transformation.
 */
public interface LookupService {

    /**
     * Lookup a value from a table.
     *
     * @param tableName    The lookup table name
     * @param key          The lookup key
     * @param targetColumn The column to return
     * @return The looked up value or null
     */
    Object lookup(String tableName, String key, String targetColumn);
}