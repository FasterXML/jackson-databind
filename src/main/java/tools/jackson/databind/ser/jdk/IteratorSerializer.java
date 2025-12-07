package tools.jackson.databind.ser.jdk;

import java.util.Iterator;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.AsArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;

@JacksonStdImpl
public class IteratorSerializer
    extends AsArraySerializerBase<Iterator<?>>
{
    public IteratorSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts) {
        super(Iterator.class, elemType, staticTyping, vts, null);
    }

    @Deprecated // since 3.1.0
    public IteratorSerializer(IteratorSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        this(src, vts, valueSerializer, unwrapSingle, property, null, false);
    }

    /**
     * @since 3.1.0
     */
    public IteratorSerializer(IteratorSerializer src,
              TypeSerializer vts, ValueSerializer<?> valueSerializer,
              Boolean unwrapSingle, BeanProperty property,
              Object suppressableValue, boolean suppressNulls) {
        super(src, vts, valueSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IteratorSerializer(this, vts, _elementSerializer, _unwrapSingle, _property);
    }

    @Override
    public IteratorSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new IteratorSerializer(this, vts, elementSerializer, unwrapSingle, property,
                null, false);
    }

    @Override
    public IteratorSerializer withResolved(BeanProperty property,
           TypeSerializer vts, ValueSerializer<?> elementSerializer,
           Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        return new IteratorSerializer(this, vts, elementSerializer, unwrapSingle, property,
                suppressableValue, suppressNulls);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext prov, Iterator<?> value) {
        return !value.hasNext();
    }

    @Override
    public boolean hasSingleElement(Iterator<?> value) {
        // no really good way to determine (without consuming iterator), so:
        return false;
    }

    @Override
    public final void serialize(Iterator<?> value, JsonGenerator gen,
            SerializationContext provider)
        throws JacksonException
    {
        // 02-Dec-2016, tatu: As per comments above, can't determine single element so...
        /*
        if (((_unwrapSingle == null) &&
                provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                || (_unwrapSingle == Boolean.TRUE)) {
            if (hasSingleElement(value)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        */
        gen.writeStartArray(value);
        if (provider.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS)
            && ((_suppressableValue != null) || _suppressNulls)
        ) {
            serializeFilteredContents(value, gen, provider);
        } else {
            serializeContents(value, gen, provider);
        }
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(Iterator<?> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        serializeContentsImpl(value, g, ctxt,
            false);
    }

    @Override
    protected void serializeFilteredContents(Iterator<?> value, JsonGenerator g,
            SerializationContext ctxt) throws JacksonException
    {
        serializeContentsImpl(value, g, ctxt,
            ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS));
    }

    private void serializeContentsImpl(Iterator<?> value, JsonGenerator g,
           SerializationContext ctxt, boolean filtered) throws JacksonException
    {
        if (!value.hasNext()) {
            return;
        }
        ValueSerializer<Object> serializer = _elementSerializer;
        if (serializer == null) {
            if (filtered) {
                _serializeFilteredDynamicContents(value, g, ctxt);
            } else {
                _serializeDynamicContents(value, g, ctxt);
            }
            return;
        }
        final TypeSerializer typeSer = _valueTypeSerializer;
        do {
            Object elem = value.next();
            if (elem == null) {
                if (filtered && _suppressNulls) {
                    continue;
                }
                ctxt.defaultSerializeNullValue(g);
            } else {
                // Check if this element should be suppressed (only in filtered mode)
                if (filtered && !_shouldSerializeElement(elem, serializer, ctxt)) {
                    continue;
                }
                if (typeSer == null) {
                    serializer.serialize(elem, g, ctxt);
                } else {
                    serializer.serializeWithType(elem, g, ctxt, typeSer);
                }
            }
        } while (value.hasNext());
    }

    protected void _serializeDynamicContents(Iterator<?> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        _serializeDynamicContentsImpl(value, g, ctxt,
            false);
    }

    protected void _serializeFilteredDynamicContents(Iterator<?> value, JsonGenerator g,
         SerializationContext ctxt) throws JacksonException
    {
        _serializeDynamicContentsImpl(value, g, ctxt,
            ctxt.isEnabled(SerializationFeature.APPLY_JSON_INCLUDE_FOR_COLLECTIONS));
    }

    private void _serializeDynamicContentsImpl(Iterator<?> value, JsonGenerator g,
               SerializationContext ctxt, boolean filtered) throws JacksonException
    {
        final TypeSerializer typeSer = _valueTypeSerializer;
        do {
            Object elem = value.next();
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
            if (filtered && !_shouldSerializeElement(elem, serializer, ctxt)) {
                continue;
            }
            if (typeSer == null) {
                serializer.serialize(elem, g, ctxt);
            } else {
                serializer.serializeWithType(elem, g, ctxt, typeSer);
            }
        } while (value.hasNext());
    }
}
