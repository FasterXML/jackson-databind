package tools.jackson.databind.ser.jdk;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatTypes;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;
import tools.jackson.databind.type.TypeFactory;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.BeanUtil;

/**
 * Standard serializer used for <code>String[]</code> values.
 */
@JacksonStdImpl
public class StringArraySerializer
    extends ArraySerializerBase<String[]>
{
    protected final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /* Note: not clean in general, but we are betting against
     * anyone re-defining properties of String.class here...
     */
    private final static JavaType VALUE_TYPE = TypeFactory.unsafeSimpleType(String.class);

    public final static StringArraySerializer instance = new StringArraySerializer();

    /**
     * Value serializer to use, if it's not the standard one
     * (if it is we can optimize serialization significantly)
     */
    protected final ValueSerializer<Object> _elementSerializer;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    protected StringArraySerializer() {
        super(String[].class);
        _elementSerializer = null;
    }

    /**
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    public StringArraySerializer(StringArraySerializer src,
            BeanProperty prop, ValueSerializer<?> ser, Boolean unwrapSingle,
            Object suppressableValue, boolean suppressNulls) {
        super(src, prop, unwrapSingle, suppressableValue, suppressNulls);
        _elementSerializer = (ValueSerializer<Object>) ser;
    }

    @Override
    public StringArraySerializer _withResolved(BeanProperty prop, Boolean unwrapSingle,
            Object suppressableValue, boolean suppressNulls) {
        return new StringArraySerializer(this, prop, _elementSerializer, unwrapSingle,
                suppressableValue, suppressNulls);
    }

    /**
     * Strings never add type info; hence, even if type serializer is suggested,
     * we'll ignore it...
     */
    @Override
    public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return this;
    }

    /*
    /**********************************************************************
    /* Post-processing
    /**********************************************************************
     */

    @Override
    public ValueSerializer<?> createContextual(SerializationContext ctxt,
            BeanProperty property)
    {
        // 29-Sep-2012, tatu: Actually, we need to do much more contextual
        //    checking here since we finally know for sure the property,
        //    and it may have overrides
        ValueSerializer<?> ser = null;

        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            final AnnotationIntrospector ai = ctxt.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                ser = ctxt.serializerInstance(m,
                        ai.findContentSerializer(ctxt.getConfig(), m));
            }
        }
        // but since formats have both property overrides and global per-type defaults,
        // need to do that separately
        Boolean unwrapSingle = findFormatFeature(ctxt, property, String[].class,
                JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        if (ser == null) {
            ser = _elementSerializer;
        }
        // May have a content converter
        ser = findContextualConvertingSerializer(ctxt, property, ser);
        if (ser == null) {
            ser = ctxt.findContentValueSerializer(String.class, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            ser = null;
        }

        // [databind#5515]: Handle content inclusion for arrays
        JsonInclude.Value inclV = findIncludeOverrides(ctxt, property, handledType());
        Object valueToSuppress = _suppressableValue;
        boolean suppressNulls = _suppressNulls;

        if (inclV != null) {
            JsonInclude.Include incl = inclV.getContentInclusion();
            if (incl != JsonInclude.Include.USE_DEFAULTS) {
                switch (incl) {
                    case NON_DEFAULT:
                        valueToSuppress = BeanUtil.propertyDefaultValue(ctxt, VALUE_TYPE);
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
        // note: will never have TypeSerializer, because Strings are "natural" type
        return new StringArraySerializer(this, property, ser, unwrapSingle,
                valueToSuppress, suppressNulls);
    }

    /*
    /**********************************************************************
    /* Simple accessors
    /**********************************************************************
     */

    @Override
    public JavaType getContentType() {
        return VALUE_TYPE;
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    @Override
    public boolean isEmpty(SerializationContext prov, String[] value) {
        return (value.length == 0);
    }

    @Override
    public boolean hasSingleElement(String[] value) {
        return (value.length == 1);
    }

    /*
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public final void serialize(String[] value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.length;
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, g, ctxt);
                return;
            }
        }
        g.writeStartArray(value, len);
        serializeContents(value, g, ctxt);
        g.writeEndArray();
    }

    @Override
    public void serializeContents(String[] value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        if (_elementSerializer != null) {
            serializeContentsSlow(value, g, ctxt, _elementSerializer);
            return;
        }
        final boolean filtered = _needToCheckFiltering(ctxt);
        for (int i = 0; i < len; ++i) {
            String str = value[i];
            if (str == null) {
                if (filtered && _suppressNulls) {
                    continue;
                }
                g.writeNull();
            } else {
                // Check if this element should be suppressed (only in filtered mode)
                if (filtered && !_shouldSerializeElement(ctxt, str, null)) {
                    continue;
                }
                g.writeString(str);
            }
        }
    }

    private void serializeContentsSlow(String[] value, JsonGenerator g,
            SerializationContext ctxt, ValueSerializer<Object> ser)
        throws JacksonException
    {
        final boolean filtered = _needToCheckFiltering(ctxt);
        for (int i = 0, len = value.length; i < len; ++i) {
            String str = value[i];
            if (str == null) {
                if (filtered && _suppressNulls) {
                    continue;
                }
                ctxt.defaultSerializeNullValue(g);
            } else {
                // Check if this element should be suppressed (only in filtered mode)
                if (filtered && !_shouldSerializeElement(ctxt, str, ser)) {
                    continue;
                }
                ser.serialize(str, g, ctxt);
            }
        }
    }

    /*
    /**********************************************************************
    /* Helper methods for content filtering
    /**********************************************************************
     */

    /**
     * Common utility method for checking if an element should be filtered/suppressed
     * based on @JsonInclude settings. Returns {@code true} if element should be serialized,
     * {@code false} if it should be skipped.
     *
     * @param ctxt Serialization context
     * @param elem Element to check for suppression
     * @param serializer Serializer for the element (may be null for strings)
     * @return true if element should be serialized, false if suppressed
     *
     * @since 3.1
     */
    protected boolean _shouldSerializeElement(SerializationContext ctxt,
            String elem, ValueSerializer<Object> serializer)
    {
        if (_suppressableValue == null) {
            return true;
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            if (serializer != null) {
                return !serializer.isEmpty(ctxt, elem);
            }
            // For strings, check emptiness directly
            return !elem.isEmpty();
        }
        return !_suppressableValue.equals(elem);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        visitArrayFormat(visitor, typeHint, JsonFormatTypes.STRING);
    }
}
