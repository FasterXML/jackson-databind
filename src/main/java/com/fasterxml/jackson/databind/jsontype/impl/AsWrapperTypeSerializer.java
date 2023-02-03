package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.jsontype.TypeIdResolver;
import com.fasterxml.jackson.databind.util.ClassUtil;

/**
 * Type wrapper that tries to use an extra JSON Object, with a single
 * entry that has type name as key, to serialize type information.
 * If this is not possible (value is serialize as array or primitive),
 * will use {@link As#WRAPPER_ARRAY} mechanism as fallback: that is,
 * just use a wrapping array with type information as the first element
 * and value as second.
 */
public class AsWrapperTypeSerializer extends TypeSerializerBase
{
    public AsWrapperTypeSerializer(TypeIdResolver idRes, BeanProperty property) {
        super(idRes, property);
    }

    @Override
    public AsWrapperTypeSerializer forProperty(BeanProperty prop) {
        return (_property == prop) ? this : new AsWrapperTypeSerializer(_idResolver, prop);
    }

    @Override
    public As getTypeInclusion() { return As.WRAPPER_OBJECT; }

    /*
    /**********************************************************
    /* Internal helper methods
    /**********************************************************
     */

    /**
     * Helper method used to ensure that intended type id is output as something that is valid:
     * currently only used to ensure that `null` output is converted to an empty String.
     *
     * @since 2.6
     */
    protected String _validTypeId(String typeId) {
        return ClassUtil.nonNullString(typeId);
    }

    // @since 2.9
    protected final void _writeTypeId(JsonGenerator g, String typeId) throws IOException
    {
        if (typeId != null) {
            g.writeTypeId(typeId);
        }
    }
}
