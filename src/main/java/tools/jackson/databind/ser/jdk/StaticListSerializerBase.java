package tools.jackson.databind.ser.jdk;

import java.util.*;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.BeanUtil;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 */
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends StdSerializer<T>
{
    // since 3.1
    protected final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     */
    protected final Boolean _unwrapSingle;

    /**
     * @since 3.1
     */
    protected final Class<?> _rawElementType;

    /**
     * Value that indicates suppression mechanism to use for
     * content values (elements of container), if any; null
     * for no filtering.
     *
     * @since 3.1
     */
    protected final Object _suppressableValue;

    /**
     * Flag that indicates whether nulls should be suppressed.
     *
     * @since 3.1
     */
    protected final boolean _suppressNulls;

    @Deprecated // since 3.1
    protected StaticListSerializerBase(Class<?> cls) {
        this(cls, String.class);
    }

    protected StaticListSerializerBase(Class<?> rawCollectionType,
            Class<?> rawElementType) {
        super(rawCollectionType);
        _rawElementType = rawElementType;
        _unwrapSingle = null;
        _suppressableValue = null;
        _suppressNulls = false;
    }

    @Deprecated // since 3.1
    protected StaticListSerializerBase(StaticListSerializerBase<?> src,
            Boolean unwrapSingle) {
        this(src, unwrapSingle, src._suppressableValue, src._suppressNulls);
    }

    /**
     * @since 3.1
     */
    protected StaticListSerializerBase(StaticListSerializerBase<?> src,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        super(src);
        _rawElementType = src._rawElementType;
        _unwrapSingle = unwrapSingle;
        _suppressableValue = suppressableValue;
        _suppressNulls = suppressNulls;
    }

    @Deprecated // since 3.1
    public abstract ValueSerializer<?> _withResolved(BeanProperty prop,
            Boolean unwrapSingle);

    /**
     * To support `@JsonInclude`.
     * Default implementation fallback to {@link StaticListSerializerBase#_withResolved(BeanProperty, Boolean, Object, boolean)}
     * @since 3.1
     */
    @SuppressWarnings("deprecation")
    public ValueSerializer<?> _withResolved(BeanProperty prop,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls
    ) {
        return _withResolved(prop, unwrapSingle);
    }

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt,
            BeanProperty property)
    {
        ValueSerializer<?> ser = null;

        if (property != null) {
            final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                ser = ctxt.serializerInstance(m,
                        intr.findContentSerializer(ctxt.getConfig(), m));
            }
        }
        Boolean unwrapSingle = null;
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(ctxt, property, ser);
        if (ser == null) {
            ser = ctxt.findContentValueSerializer(_rawElementType, property);
        }
        // Handle content inclusion (similar to MapSerializer lines 560-609)
        JsonInclude.Value inclV = findIncludeOverrides(ctxt, property, handledType());
        Object valueToSuppress = _suppressableValue;
        boolean suppressNulls = _suppressNulls;

        if (inclV != null) {
            JsonInclude.Include incl = inclV.getContentInclusion();
            if (incl != JsonInclude.Include.USE_DEFAULTS) {
                switch (incl) {
                    case NON_DEFAULT:
                        valueToSuppress = BeanUtil.getDefaultValue(ctxt.constructType(_rawElementType));
                        suppressNulls = true;
                        if (valueToSuppress != null) {
                            if (valueToSuppress.getClass().isArray()) {
                                valueToSuppress = ArrayBuilders.getArrayComparator(valueToSuppress);
                            }
                        }
                        break;
                    case NON_ABSENT:
                        suppressNulls = true;
                        valueToSuppress = MARKER_FOR_EMPTY;
                        break;
                    case NON_EMPTY:
                        suppressNulls = true;
                        valueToSuppress = MARKER_FOR_EMPTY;
                        break;
                    case CUSTOM:
                        valueToSuppress = ctxt.includeFilterInstance(null, inclV.getContentFilter());
                        if (valueToSuppress == null) {
                            suppressNulls = true;
                        } else {
                            suppressNulls = ctxt.includeFilterSuppressNulls(valueToSuppress);
                        }
                        break;
                    case NON_NULL:
                        valueToSuppress = null;
                        suppressNulls = true;
                        break;
                    case ALWAYS:
                    default:
                        valueToSuppress = null;
                        suppressNulls = false;
                        break;
                }
            }
        }

        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            if (Objects.equals(unwrapSingle, _unwrapSingle)
                && Objects.equals(valueToSuppress, _suppressableValue)
                && suppressNulls == _suppressNulls
            ) {
                return this;
            }
            return _withResolved(property, unwrapSingle, valueToSuppress, suppressNulls);
        }
        // otherwise...
        // note: will never have TypeSerializer, because Strings are "natural" type
        return new CollectionSerializer(ctxt.constructType(String.class),
                true, /*TypeSerializer*/ null, (ValueSerializer<Object>) ser);
    }

    @Override
    public boolean isEmpty(SerializationContext provider, T value) {
        return (value == null) || (value.isEmpty());
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint) {
        JsonArrayFormatVisitor v2 = visitor.expectArrayFormat(typeHint);
        if (v2 != null) {
            acceptContentVisitor(v2);
        }
    }

    /*
    /**********************************************************
    /* Abstract methods for sub-classes to implement
    /**********************************************************
     */

    protected abstract JsonNode contentSchema();

    protected abstract void acceptContentVisitor(JsonArrayFormatVisitor visitor);

    // just to make sure it gets implemented:
    @Override
    public abstract void serializeWithType(T value, JsonGenerator g,
            SerializationContext ctxt, TypeSerializer typeSer) throws JacksonException;

    /**
     * Common utility method for checking if an element should be filtered/suppressed
     * based on @JsonInclude settings. Returns {@code true} if element should be serialized,
     * {@code false} if it should be skipped.
     *
     * @param elem Element to check for suppression
     * @param serializer Serializer for the element (may be null for strings)
     * @param ctxt {@link SerializationContext}
     * @return true if element should be serialized, false if suppressed
     *
     * @since 3.1
     */
    protected final boolean _shouldSerializeElement(Object elem, ValueSerializer<Object> serializer,
        SerializationContext ctxt) throws JacksonException
    {
        if (_suppressableValue == null) {
            return true;
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            if (serializer != null) {
                return !serializer.isEmpty(ctxt, elem);
            }
            // For strings and primitives, check emptiness directly
            if (elem instanceof String str) {
                return !str.isEmpty();
            }
            return true;
        }
        return !_suppressableValue.equals(elem);
    }
}
