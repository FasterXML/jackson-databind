package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.annotation.JsonIgnoreIf;

/**
 * Abstract class which .class reference will be used to
 * validate if a property annoated with {@link JsonIgnoreIf}
 * should be ignored or not.
 */
public abstract class JsonIgnoreValidator {

    /**
     * When instantiating this class this abstract method should be defined.
     * The logic will be executed and checked if property should be ignored
     * or not.
     *
     * @return When returning true the property will not be parsed. When returning false not.
     */
    public abstract boolean ignore();

}