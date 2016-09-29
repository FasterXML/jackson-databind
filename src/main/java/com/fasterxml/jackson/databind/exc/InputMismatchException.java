package com.fasterxml.jackson.databind.exc;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonLocation;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonMappingException;

/**
 * General exception type used as the base class for all {@link JsonMappingException}s
 * that are due to input not mapping to target definition; these are typically
 * considered "client errors" since target type definition itself is not the root cause
 * but mismatching input. This is in contrast to {@link InvalidDefinitionException} which
 * signals a problem with target type definition and not input.
 *<p>
 * This type is used as-is for some input problems, but in most cases there should be
 * more explicit subtypes to use.
 *
 * @since 2.9
 */
@SuppressWarnings("serial")
public class InputMismatchException
    extends JsonMappingException
{
    protected InputMismatchException(JsonParser p, String msg) {
        super(p, msg);
    }

    protected InputMismatchException(JsonParser p, String msg, JsonLocation loc) {
        super(p, msg, loc);
    }

    public static InputMismatchException from(JsonParser p, String msg) {
        return new InputMismatchException(p, msg);
    }

    @SuppressWarnings("deprecation")
    public static InputMismatchException fromUnexpectedIOE(IOException src) {
        return new InputMismatchException(null,
                String.format("Unexpected IOException (of type %s): %s",
                        src.getClass().getName(), src.getMessage()));
    }
}
