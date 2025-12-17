package com.example.edicleanarch.common.transform;


/**
 * Interface for database lookups during transformation.
 */
public interface LookupService {

    /**
     * Lookup a value from a table using single key column.
     *
     * @param tableName    The lookup table name
     * @param keyColumn    The column to match the key against
     * @param keyValue     The lookup key value
     * @param targetColumn The column to return
     * @return The looked up value or null
     */
    Object lookup(String tableName, String keyColumn, String keyValue, String targetColumn);

    /**
     * Lookup a value from a table using a WHERE condition.
     *
     * @param tableName      The lookup table name
     * @param whereCondition The WHERE clause condition (already resolved, no placeholders)
     * @param targetColumn   The column to return
     * @return The looked up value or null
     */
    Object lookupWithCondition(String tableName, String whereCondition, String targetColumn);
}