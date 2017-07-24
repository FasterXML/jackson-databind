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
     * 
     * @since 2.9
     */
    public abstract WritableTypeId writeTypePrefix(JsonGenerator g,
            WritableTypeId typeId) throws IOException;

    /**
     * @since 2.9
     */
    public abstract WritableTypeId writeTypeSuffix(JsonGenerator g,
            WritableTypeId typeId) throws IOException;

    /*
    /**********************************************************
    /* Legacy type serialization methods
    /**********************************************************
     */

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as scalar JSON value (not as JSON
     * Object or Array).
     * This means that the context after call cannot be that of JSON Object;
     * it may be Array or root context.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param g Generator to use for writing type information
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForScalar(Object value, JsonGenerator g) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.VALUE_STRING));
    }

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Object value (not as JSON
     * Array or scalar).
     * This means that context after call must be JSON Object, meaning that
     * caller can then proceed to output field entries.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param g Generator to use for writing type information
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForObject(Object value, JsonGenerator g) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_OBJECT));
    }

    /**
     * Method called to write initial part of type information for given
     * value, when it will be output as JSON Array value (not as JSON
     * Object or scalar).
     * This means that context after call must be JSON Array, that is, there
     * must be an open START_ARRAY to write contents in.
     * 
     * @param value Value that will be serialized, for which type information is
     *   to be written
     * @param g Generator to use for writing type information
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForArray(Object value, JsonGenerator g) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_ARRAY));
    }

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForScalar}.
     * Actual action to take may depend on various factors, but has to match with
     * action {@link #writeTypePrefixForScalar} did (close array or object; or do nothing).
     */
    @Deprecated // since 2.9
    public void writeTypeSuffixForScalar(Object value, JsonGenerator g) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.VALUE_STRING));
    }

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypePrefixForObject}.
     * It needs to write closing END_OBJECT marker, and any other decoration
     * that needs to be matched.
     */
    @Deprecated // since 2.9
    public void writeTypeSuffixForObject(Object value, JsonGenerator g) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_OBJECT));
    }

    /**
     * Method called after value has been serialized, to close any scopes opened
     * by earlier matching call to {@link #writeTypeSuffixForScalar}.
     * It needs to write closing END_ARRAY marker, and any other decoration
     * that needs to be matched.
     */
    @Deprecated // since 2.9
    public void writeTypeSuffixForArray(Object value, JsonGenerator g) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_ARRAY));
    }

    /**
     * Alternative version of the prefix-for-scalar method, which is given
     * actual type to use (instead of using exact type of the value); typically
     * a super type of actual value type
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForScalar(Object value, JsonGenerator g, Class<?> type) throws IOException {
        writeTypePrefix(g, typeId(value, type, JsonToken.VALUE_STRING));
    }

    /**
     * Alternative version of the prefix-for-object method, which is given
     * actual type to use (instead of using exact type of the value); typically
     * a super type of actual value type
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForObject(Object value, JsonGenerator g, Class<?> type) throws IOException {
        writeTypePrefix(g, typeId(value, type, JsonToken.START_OBJECT));
    }

    /**
     * Alternative version of the prefix-for-array method, which is given
     * actual type to use (instead of using exact type of the value); typically
     * a super type of actual value type
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForArray(Object value, JsonGenerator g, Class<?> type) throws IOException {
        writeTypePrefix(g, typeId(value, type, JsonToken.START_ARRAY));
    }

    /*
    /**********************************************************
    /* Type serialization methods with type id override
    /**********************************************************
     */

    @Deprecated // since 2.9
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.VALUE_STRING, typeId));
    }

    @Deprecated // since 2.9
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_OBJECT, typeId));
    }

    @Deprecated // since 2.9
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_ARRAY, typeId));
    }

    @Deprecated // since 2.9
    public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.VALUE_STRING, typeId));
    }

    @Deprecated // since 2.9
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_OBJECT, typeId));
    }

    @Deprecated // since 2.9
    public void writeCustomTypeSuffixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_ARRAY, typeId));
    }

    /**
     * Helper method needed for backwards compatibility: since original type id
     * can not be routed through completely, we have to reverse-engineer likely
     * setting before calling suffix.
     *
     * @since 2.9
     */
    protected final void _writeLegacySuffix(JsonGenerator g,
            WritableTypeId typeId) throws IOException
    {
        // most likely logic within generator is this:
        typeId.wrapperWritten = !g.canWriteTypeId();
        writeTypeSuffix(g, typeId);
    }
}
