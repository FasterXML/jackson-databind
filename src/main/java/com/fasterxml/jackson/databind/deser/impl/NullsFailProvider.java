package com.fasterxml.jackson.databind.deser.impl;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.NullValueProvider;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.util.AccessPattern;

/**
 * Simple {@link NullValueProvider} that will always throw a
 * {@link InvalidNullException} when a null is encountered.
 */
public class NullsFailProvider
    implements NullValueProvider, java.io.Serializable
{
    private static final long serialVersionUID = 1L;

    protected final PropertyName _name;
    protected final JavaType _type;

    protected NullsFailProvider(PropertyName name, JavaType type) {
        _name = name;
        _type = type;
    }

    public static NullsFailProvider constructForProperty(BeanProperty prop) {
        return constructForProperty(prop, prop.getType());
    }

    // @since 2.10.2
    public static NullsFailProvider constructForProperty(BeanProperty prop, JavaType type) {
        return new NullsFailProvider(prop.getFullName(), type);
    }

    public static NullsFailProvider constructForRootValue(JavaType t) {
        return new NullsFailProvider(null, t);
    }

    @Override
    public AccessPattern getNullAccessPattern() {
        // Must be called every time to effect the exception...
        return AccessPattern.DYNAMIC;
    }

    @Override
    public Object getNullValue(DeserializationContext ctxt)
            throws JsonMappingException {
        throw InvalidNullException.from(ctxt, _name, _type);
    }
}
