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
 * control over actual serialization details. Minor changes are required to change
 * call pattern so that return value of "prefix" write needs to be passed to "suffix"
 * write.
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
     * Note that for structured types (Object, Array), this call will add
     * necessary start token so it should NOT be explicitly written, unlike
     * with non-type-id value writes.
     *<p>
     * See {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} for a complete
     * example of typical usage.
     *
     * @param g Generator to use for outputting type id and possible wrapping
     * @param typeId Details of what type id is to be written, how.
     *
     * @since 2.9
     */
    public abstract WritableTypeId writeTypePrefix(JsonGenerator g,
            WritableTypeId typeId) throws IOException;

    /**
     * Method that should be called after {@link #writeTypePrefix(JsonGenerator, WritableTypeId)}
     * and matching value write have been called, passing {@link WritableTypeId} returned.
     * Usual idiom is:
     *<pre>
     * // Indicator generator that type identifier may be needed; generator may write
     * // one as suggested, modify information, or take some other action
     * // NOTE! For Object/Array types, this will ALSO write start marker!
     * WritableTypeId typeIdDef = typeSer.writeTypePrefix(gen,
     *          typeSer.typeId(value, JsonToken.START_OBJECT));
     *
     * // serializing actual value for which TypeId may have been written... like
     * // NOTE: do NOT write START_OBJECT before OR END_OBJECT after:
     * g.writeStringField("message", "Hello, world!"
     *
     * // matching type suffix call to let generator chance to add suffix, if any
     * // NOTE! For Object/Array types, this will ALSO write end marker!
     * typeSer.writeTypeSuffix(gen, typeIdDef);
     *</pre>
     *
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
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, JsonToken.VALUE_STRING));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForScalar(Object value, JsonGenerator g) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.VALUE_STRING));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, JsonToken.START_OBJECT));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForObject(Object value, JsonGenerator g) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_OBJECT));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, JsonToken.START_ARRAY));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForArray(Object value, JsonGenerator g) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_ARRAY));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypeSuffix(g, typeId(value, JsonToken.VALUE_STRING));}.
     * See {@link #writeTypeSuffix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypeSuffixForScalar(Object value, JsonGenerator g) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.VALUE_STRING));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypeSuffix(g, typeId(value, JsonToken.START_OBJECT));}.
     * See {@link #writeTypeSuffix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypeSuffixForObject(Object value, JsonGenerator g) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_OBJECT));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypeSuffix(g, typeId(value, JsonToken.START_ARRAY));}.
     * See {@link #writeTypeSuffix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypeSuffixForArray(Object value, JsonGenerator g) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_ARRAY));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, type, JsonToken.VALUE_STRING));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForScalar(Object value, JsonGenerator g, Class<?> type) throws IOException {
        writeTypePrefix(g, typeId(value, type, JsonToken.VALUE_STRING));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, type, JsonToken.START_OBJECT));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeTypePrefixForObject(Object value, JsonGenerator g, Class<?> type) throws IOException {
        writeTypePrefix(g, typeId(value, type, JsonToken.START_OBJECT));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, type, JsonToken.START_ARRAY));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
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

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, JsonToken.VALUE_STRING, typeId));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeCustomTypePrefixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.VALUE_STRING, typeId));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, JsonToken.START_OBJECT, typeId));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeCustomTypePrefixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_OBJECT, typeId));
    }

    /**
     * DEPRECATED: now equivalent to:
     *{@code writeTypePrefix(g, typeId(value, JsonToken.START_ARRAY, typeId));}.
     * See {@link #writeTypePrefix} for more info.
     *
     * @deprecated Since 2.9 use {@link #writeTypePrefix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeCustomTypePrefixForArray(Object value, JsonGenerator g, String typeId) throws IOException {
        writeTypePrefix(g, typeId(value, JsonToken.START_ARRAY, typeId));
    }

    /**
     * @deprecated Since 2.9 use {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeCustomTypeSuffixForScalar(Object value, JsonGenerator g, String typeId) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.VALUE_STRING, typeId));
    }

    /**
     * @deprecated Since 2.9 use {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} instead
     */
    @Deprecated // since 2.9
    public void writeCustomTypeSuffixForObject(Object value, JsonGenerator g, String typeId) throws IOException {
        _writeLegacySuffix(g, typeId(value, JsonToken.START_OBJECT, typeId));
    }

    /**
     * @deprecated Since 2.9 use {@link #writeTypeSuffix(JsonGenerator, WritableTypeId)} instead
     */
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
