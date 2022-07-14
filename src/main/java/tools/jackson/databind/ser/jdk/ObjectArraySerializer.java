package tools.jackson.databind.ser.jdk;

import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.ArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;

/**
 * Generic serializer for Object arrays (<code>Object[]</code>).
 */
@JacksonStdImpl
public class ObjectArraySerializer
    extends ArraySerializerBase<Object[]>
{
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

    @SuppressWarnings("unchecked")
    public ObjectArraySerializer(ObjectArraySerializer src,
            BeanProperty property, TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle)
    {
        super(src,  property, unwrapSingle);
        _elementType = src._elementType;
        _valueTypeSerializer = vts;
        _staticTyping = src._staticTyping;
        _elementSerializer = (ValueSerializer<Object>) elementSerializer;
    }

    @Override
    public ValueSerializer<?> _withResolved(BeanProperty prop, Boolean unwrapSingle) {
        return new ObjectArraySerializer(this, prop,
                _valueTypeSerializer, _elementSerializer, unwrapSingle);
    }
    
    @Override
    public StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new ObjectArraySerializer(_elementType, _staticTyping, vts, _elementSerializer);
    }

    public ObjectArraySerializer withResolved(BeanProperty prop,
            TypeSerializer vts, ValueSerializer<?> ser, Boolean unwrapSingle) {
        if ((_property == prop) && (ser == _elementSerializer)
                && (_valueTypeSerializer == vts) && (Objects.equals(_unwrapSingle, unwrapSingle))) {
            return this;
        }
        return new ObjectArraySerializer(this, prop, vts, ser, unwrapSingle);
    }

    /*
    /**********************************************************************
    /* Post-processing
    /**********************************************************************
     */

    @Override
    public ValueSerializer<?> createContextual(SerializerProvider ctxt,
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
        return withResolved(property, vts, ser, unwrapSingle);
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
    public boolean isEmpty(SerializerProvider prov, Object[] value) {
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
    public final void serialize(Object[] value, JsonGenerator g, SerializerProvider ctxt)
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
    public void serializeContents(Object[] value, JsonGenerator g, SerializerProvider ctxt)
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
        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
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
                serializer.serialize(elem, g, ctxt);
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, elem, i);
        }
    }

    public void serializeContentsUsing(Object[] value, JsonGenerator g, SerializerProvider provider,
            ValueSerializer<Object> ser) throws JacksonException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;

        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    provider.defaultSerializeNullValue(g);
                    continue;
                }
                if (typeSer == null) {
                    ser.serialize(elem, g, provider);
                } else {
                    ser.serializeWithType(elem, g, provider, typeSer);
                }
            }
        } catch (Exception e) {
            wrapAndThrow(provider, e, elem, i);
        }
    }

    public void serializeTypedContents(Object[] value, JsonGenerator g, SerializerProvider ctxt)
        throws JacksonException
    {
        final int len = value.length;
        final TypeSerializer typeSer = _valueTypeSerializer;
        int i = 0;
        Object elem = null;
        try {
            for (; i < len; ++i) {
                elem = value[i];
                if (elem == null) {
                    ctxt.defaultSerializeNullValue(g);
                    continue;
                }
                Class<?> cc = elem.getClass();
                ValueSerializer<Object> serializer = _dynamicValueSerializers.serializerFor(cc);
                if (serializer == null) {
                    serializer = _findAndAddDynamic(ctxt, cc);
                }
                serializer.serializeWithType(elem, g, ctxt, typeSer);
            }
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, elem, i);
        }
    }

    @Override
    public void acceptJsonFormatVisitor(JsonFormatVisitorWrapper visitor, JavaType typeHint)
    {
        JsonArrayFormatVisitor arrayVisitor = visitor.expectArrayFormat(typeHint);
        if (arrayVisitor != null) {
            JavaType contentType = _elementType;
            ValueSerializer<?> valueSer = _elementSerializer;
            if (valueSer == null) {
                valueSer = visitor.getProvider().findContentValueSerializer(contentType, _property);
            }
            arrayVisitor.itemsFormat(valueSer, contentType);
        }
    }
}
