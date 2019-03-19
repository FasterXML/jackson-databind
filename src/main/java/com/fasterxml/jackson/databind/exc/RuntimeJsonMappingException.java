package com.fasterxml.jackson.databind.exc;

import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * Wrapper used when interface does not allow throwing a checked
 * {@link JsonMappingException}
 */
@SuppressWarnings("serial")
public class RuntimeJsonMappingException extends RuntimeException
{
    public RuntimeJsonMappingException(JsonMappingException cause) {
        super(cause);
    }

    public RuntimeJsonMappingException(String message) {
        super(message);
    }

    public RuntimeJsonMappingException(String message, JsonMappingException cause) {
        super(message, cause);
    }
}
