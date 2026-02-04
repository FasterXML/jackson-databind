package tools.jackson.databind.ser.jdk;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;

import tools.jackson.core.*;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;
import tools.jackson.databind.util.ArrayBuilders;
import tools.jackson.databind.util.BeanUtil;

/**
 * Generic serializer for Object arrays (<code>Object[]</code>).
 */
@JacksonStdImpl
public class ObjectArraySerializer
    extends ArraySerializerBase<Object[]>
{
    protected final static Object MARKER_FOR_EMPTY = JsonInclude.Include.NON_EMPTY;

    /**
     * Whether we are using static typing (using declared types, ignoring
     * runtime type) or not for elements.
     */
    protected final boolean _staticTyping;

    /**
     * Declared type of element entries
     */
    protected final JavaType _elementType;

    /**
     * Type serializer to use for values, if any.
     */
    protected final TypeSerializer _valueTypeSerializer;

    /**
     * Value serializer to use, if it can be statically determined.
     */
    protected ValueSerializer<Object> _elementSerializer;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public ObjectArraySerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts, ValueSerializer<Object> elementSerializer)
    {
        super(Object[].class);
        _elementType = elemType;
        _staticTyping = staticTyping;
        _valueTypeSerializer = vts;
        _elementSerializer = elementSerializer;
    }

    public ObjectArraySerializer(ObjectArraySerializer src, TypeSerializer vts)
    {
        super(src);
        _elementType = src._elementType;
        _valueTypeSerializer = vts;
        _staticTyping = src._staticTyping;
        _elementSerializer = src._elementSerializer;
    }

    @Deprecated // since 3.1
    public ObjectArraySerializer(ObjectArraySerializer src,
            BeanProperty property, TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle)
    {
        this(src, property, vts, elementSerializer, unwrapSingle, null, false);
    }

    /**
     * @since 3.1
     */
    @SuppressWarnings("unchecked")
    public ObjectArraySerializer(ObjectArraySerializer src,
            BeanProperty property, TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls)
    {
        super(src, property, unwrapSingle, suppressableValue, suppressNulls);
        _elementType = src._elementType;
        _valueTypeSerializer = vts;
        _staticTyping = src._staticTyping;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
    }

    @Override
    public ObjectArraySerializer _withResolved(BeanProperty prop, Boolean unwrapSingle,
            Object suppressableValue, boolean suppressNulls) {
        return new ObjectArraySerializer(this, prop,
                _valueTypeSerializer, _elementSerializer, unwrapSingle,
                suppressableValue, suppressNulls);
    }

    @Override
    public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new ObjectArraySerializer(_elementType, _staticTyping, vts, _elementSerializer);
    }

    /**
     * @since 3.1
     */
    protected ObjectArraySerializer _withResolved(BeanProperty prop,
            TypeSerializer vts, ValueSerializer<?> elementSer, Boolean unwrapSingle,
            Object suppressableValue, boolean suppressNulls) {
        if ((_property == prop)
                && (_valueTypeSerializer == vts)
                && (_elementSerializer == elementSer)
                && Objects.equals(_unwrapSingle, unwrapSingle)
                && Objects.equals(_suppressableValue, suppressableValue)
                && (_suppressNulls == suppressNulls)
        ) {
            return this;
        }
        return new ObjectArraySerializer(this, prop, vts, elementSer, unwrapSingle,
                suppressableValue, suppressNulls);
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
        TypeSerializer vts = _valueTypeSerializer;
        if (vts != null) { // need to contextualize
            vts = vts.forProperty(ctxt, property);
        }
        ValueSerializer<?> ser = null;
        Boolean unwrapSingle = null;

        // First: if we have a property, may have property-annotation overrides
        if (property != null) {
            AnnotatedMember m = property.getMember();
            final AnnotationIntrospector intr = ctxt.getAnnotationIntrospector();
            if (m != null) {
                ser = ctxt.serializerInstance(m,
                        intr.findContentSerializer(ctxt.getConfig(), m));
            }
        }
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        if (ser == null) {
            ser = _elementSerializer;
        }
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(ctxt, property, ser);
        if (ser == null) {
            // 30-Sep-2012, tatu: One more thing -- if explicit content type is annotated,
            //   we can consider it a static case as well.
            if (_elementType != null) {
                if (_staticTyping && !_elementType.isJavaLangObject()) {
                    ser = ctxt.findContentValueSerializer(_elementType, property);
                }
            }
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
                        valueToSuppress = BeanUtil.propertyDefaultValue(ctxt, _elementType);
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

        return _withResolved(property, vts, ser, unwrapSingle, valueToSuppress, suppressNulls);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public JavaType getContentType() {
        return _elementType;
    }

    @Override
    public ValueSerializer<?> getContentSerializer() {
        return _elementSerializer;
    }

    @Override
    public boolean isEmpty(SerializationContext prov, Object[] value) {
        return value.length == 0;
    }

    @Override
    public boolean hasSingleElement(Object[] value) {
        return (value.length == 1);
    }

    /*
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public final void serialize(Object[] value, JsonGenerator g, SerializationContext ctxt)
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

    // [databind#3194]: Override to check whether element type info should be
    // written after the outer array's type ID has been written. When the outer
    // type ID determines a concrete element type that is "final" (per default
    // typing rules), inner element type IDs are redundant and cause
    // deserialization failures.
    @Override
    public void serializeWithType(Object[] value, JsonGenerator g, SerializationContext ctxt,
            TypeSerializer typeSer)
        throws JacksonException
    {
        WritableTypeId typeIdDef = typeSer.writeTypePrefix(g, ctxt,
                typeSer.typeId(value, JsonToken.START_ARRAY));
        g.assignCurrentValue(value);
        if (_valueTypeSerializer != null && _elementSerializer == null
                && _shouldSkipElementTypeId(value, ctxt)) {
            _serializeDynamicContents(value, g, ctxt);
        } else {
            serializeContents(value, g, ctxt);
        }
        typeSer.writeTypeSuffix(g, ctxt, typeIdDef);
    }

    /**
     * Helper method for [databind#3194]: checks if the actual runtime array
     * element type does NOT require type serialization based on default typing
     * rules. For example, {@code String[]} has final component type {@code String},
     * so with {@code NON_FINAL} typing it does not need a type wrapper.
     */
    private boolean _shouldSkipElementTypeId(Object[] value, SerializationContext ctxt) {
        Class<?> actualComponentType = value.getClass().getComponentType();
        // If actual component type matches declared element type, definitely
        // need type info (it was already determined to need it at construction time)
        if (actualComponentType == _elementType.getRawClass()) {
            return false;
        }
        // Check if the actual component type would get a TypeSerializer;
        // if not, its type info is redundant since the outer type ID already
        // determines the concrete element type.
        return ctxt.findTypeSerializer(ctxt.constructType(actualComponentType)) == null;
    }

    @Override
    public void serializeContents(Object[] value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        if (_elementSerializer != null) {
            serializeContentsUsing(value, g, ctxt, _elementSerializer);
            return;
        }
        if (_valueTypeSerializer != null) {
            serializeTypedContents(value, g, ctxt);
            return;
        }
        _serializeDynamicContents(value, g, ctxt);
    }

    /**
     * Serialize array elements using dynamically resolved per-element serializers,
     * WITHOUT element type wrappers.
     * Extracted from {@link #serializeContents} for reuse from
     * {@link #serializeWithType} (see [databind#3194]).
     */
    protected void _serializeDynamicContents(Object[] value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.length;
        if (len == 0) {
            return;
        }
        final boolean filtered = _needToCheckFiltering(ctxt);
        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                    continue;
                }
                Class<?> cc = elem.getClass();
                ValueSerializer<Object> serializer = _dynamicValueSerializers.serializerFor(cc);
                if (serializer == null) {
                    if (_elementType.hasGenericTypes()) {
                        serializer = _findAndAddDynamic(ctxt,
                                ctxt.constructSpecializedType(_elementType, cc));
                    } else {
                        serializer = _findAndAddDynamic(ctxt, cc);
                    }
                }
                // Check if this element should be suppressed (only in filtered mode)
                if (filtered && !_shouldSerializeElement(ctxt, elem, serializer)) {
                    continue;
                }
                serializer.serialize(elem, g, ctxt);
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, elem, i);
        }
    }

    public void serializeContentsUsing(Object[] value, JsonGenerator g,
            SerializationContext ctxt, ValueSerializer<Object> ser)
        throws JacksonException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;
        final boolean filtered = _needToCheckFiltering(ctxt);

        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                    continue;
                }
                // Check if this element should be suppressed (only in filtered mode)
                if (filtered && !_shouldSerializeElement(ctxt, elem, ser)) {
                    continue;
                }
                if (typeSer == null) {
                    ser.serialize(elem, g, ctxt);
                } else {
                    ser.serializeWithType(elem, g, ctxt, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, elem, i);
        }
    }

    public void serializeTypedContents(Object[] value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;
        final boolean filtered = _needToCheckFiltering(ctxt);
        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    if (filtered && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                    continue;
                }
                Class<?> cc = elem.getClass();
                ValueSerializer<Object> serializer = _dynamicValueSerializers.serializerFor(cc);
                if (serializer == null) {
                    serializer = _findAndAddDynamic(ctxt, cc);
                }
                // Check if this element should be suppressed (only in filtered mode)
                if (filtered && !_shouldSerializeElement(ctxt, elem, serializer)) {
                    continue;
                }
                serializer.serializeWithType(elem, g, ctxt, typeSer);
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, elem, i);
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
            Object elem, ValueSerializer<Object> serializer)
    {
        if (_suppressableValue == null) {
            return true;
        }
        if (_suppressableValue == MARKER_FOR_EMPTY) {
            if (serializer != null) {
                return !serializer.isEmpty(ctxt, elem);
            }
            // For strings, check emptiness directly
            if (elem instanceof String str) {
                return !str.isEmpty();
            }
            return true;
        }
        return !_suppressableValue.equals(elem);
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        JsonArrayFormatVisitor arrayVisitor = visitor.expectArrayFormat(typeHint);
        if (arrayVisitor != null) {
            JavaType contentType = _elementType;
            ValueSerializer<?> valueSer = _elementSerializer;
            if (valueSer == null) {
                valueSer = visitor.getContext().findContentValueSerializer(contentType, _property);
            }
            arrayVisitor.itemsFormat(valueSer, contentType);
        }
    }
}
