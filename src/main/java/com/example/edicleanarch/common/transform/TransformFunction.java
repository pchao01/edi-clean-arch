package com.example.edicleanarch.common.transform;


/**
 * Functional interface for transform operations.
 */
@FunctionalInterface
public interface TransformFunction {
    Object apply(TransformContext context);
}