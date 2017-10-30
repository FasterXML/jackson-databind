package com.fasterxml.jackson.databind.jsontype;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.core.util.VersionUtil;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

/**
 * Interface for serializing type information regarding instances of specified
 * base type (super class), so that exact subtype can be properly deserialized
 * later on. These instances are to be called by regular
 * {@link com.fasterxml.jackson.databind.JsonSerializer}s using proper contextual
 * calls, to add type information using mechanism type serializer was
 * configured with.
 *<p>
 * NOTE: version 2.9 contains significant attempt at simplifying interface,
 * as well as giving format implementation (via {@link JsonGenerator}) more
 * control over actual serialization details.
 */
public abstract class TypeSerializer
{
    /*
    /**********************************************************
    /* Initialization
    /**********************************************************
     */

    /**
     * Method called to create contextual version, to be used for
     * values of given property. This may be the type itself
     * (as is the case for bean properties), or values contained
     * (for {@link java.util.Collection} or {@link java.util.Map}
     * valued properties).
     * 
     * @since 2.0
     */
    public abstract TypeSerializer forProperty(BeanProperty prop);

    /*
    /**********************************************************
    /* Introspection
    /**********************************************************
     */

    /**
     * Accessor for type information inclusion method
     * that serializer uses; indicates how type information
     * is embedded in resulting JSON.
     */
    public abstract JsonTypeInfo.As getTypeInclusion();

    /**
     * Name of property that contains type information, if
     * property-based inclusion is used.
     */
    public abstract String getPropertyName();

    /**
     * Accessor for object that handles conversions between
     * types and matching type ids.
     */
    public abstract TypeIdResolver getTypeIdResolver();

    /*
    /**********************************************************
    /* Type serialization methods: new (2.9)
    /**********************************************************
     */

    /**
     * Factory method for constructing type id value object to pass to
     * {@link #writeTypePrefix}.
     */
    public WritableTypeId typeId(Object value, JsonToken valueShape) {
        WritableTypeId typeIdDef = new WritableTypeId(value, valueShape);
        switch (getTypeInclusion()) {
        case EXISTING_PROPERTY:
            typeIdDef.include = WritableTypeId.Inclusion.PAYLOAD_PROPERTY;
            typeIdDef.asProperty = getPropertyName();
            break;
        case EXTERNAL_PROPERTY:
            typeIdDef.include = WritableTypeId.Inclusion.PARENT_PROPERTY;
            typeIdDef.asProperty = getPropertyName();
            break;
        case PROPERTY:
            typeIdDef.include = WritableTypeId.Inclusion.METADATA_PROPERTY;
            typeIdDef.asProperty = getPropertyName();
            break;
        case WRAPPER_ARRAY:
            typeIdDef.include = WritableTypeId.Inclusion.WRAPPER_ARRAY;
            break;
        case WRAPPER_OBJECT:
            typeIdDef.include = WritableTypeId.Inclusion.WRAPPER_OBJECT;
            break;
        default:
            VersionUtil.throwInternal();
        }
        return typeIdDef;
    }

    public WritableTypeId typeId(Object value, JsonToken valueShape,
            Object id) {
        WritableTypeId typeId = typeId(value, valueShape);
        typeId.id = id;
        return typeId;
    }

    public WritableTypeId typeId(Object value, Class<?> typeForId,
            JsonToken valueShape) {
        WritableTypeId typeId = typeId(value, valueShape);
        typeId.forValueType = typeForId;
        return typeId;
    }

    /**
     * Method called to write initial part of type information for given
     * value, along with possible wrapping to use: details are specified
     * by `typeId` argument.
     *
     * @param g Generator to use for outputting type id and possible wrapping
     * @param typeId Details of what type id is to be written, how.
     */
    public abstract WritableTypeId writeTypePrefix(JsonGenerator g,
            WritableTypeId typeId) throws IOException;

    public abstract WritableTypeId writeTypeSuffix(JsonGenerator g,
            WritableTypeId typeId) throws IOException;

    /**
     * Helper method needed for backwards compatibility: since original type id
     * can not be routed through completely, we have to reverse-engineer likely
     * setting before calling suffix.
     */
    protected final void _writeLegacySuffix(JsonGenerator g,
            WritableTypeId typeId) throws IOException
    {
        // most likely logic within generator is this:
        typeId.wrapperWritten = !g.canWriteTypeId();
        writeTypeSuffix(g, typeId);
    }
}
