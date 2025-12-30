package tools.jackson.databind.ser.jdk;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.AsArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;

@JacksonStdImpl
public class IterableSerializer
    extends AsArraySerializerBase<Iterable<?>>
{
    public IterableSerializer(JavaType elemType, boolean staticTyping,
            TypeSerializer vts) {
        super(Iterable.class, elemType, staticTyping, vts, null);
    }

    @Deprecated // since 3.1
    public IterableSerializer(IterableSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        this(src, vts, valueSerializer, unwrapSingle, property, null, false);
    }

    /**
     * @since 3.1
     */
    public IterableSerializer(IterableSerializer src,
             TypeSerializer vts, ValueSerializer<?> valueSerializer,
             Boolean unwrapSingle, BeanProperty property, Object suppressableValue, boolean suppressNulls) {
        super(src, vts, valueSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new IterableSerializer(this, vts, _elementSerializer, _unwrapSingle, _property,
                _suppressableValue, _suppressNulls);
    }

    @Override
    public IterableSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        return new IterableSerializer(this, vts, elementSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext ctxt, Iterable<?> value) {
        // Not really good way to implement this, but has to do for now:
        return !value.iterator().hasNext();
    }

    @Override
    public boolean hasSingleElement(Iterable<?> value) {
        // we can do it actually (fixed in 2.3.1)
        if (value != null) {
            Iterator<?> it = value.iterator();
            if (it.hasNext()) {
                it.next();
                if (!it.hasNext()) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public final void serialize(Iterable<?> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        if (((_unwrapSingle == null) &&
                ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                || (_unwrapSingle == Boolean.TRUE)) {
            if (hasSingleElement(value)) {
                serializeContents(value, g, ctxt);
                return;
            }
        }
        g.writeStartArray(value);
        serializeContents(value, g, ctxt);
        g.writeEndArray();
    }

    @Override
    public void serializeContents(Iterable<?> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final boolean needsFiltering = _needToCheckFiltering(ctxt);
        Iterator<?> it = value.iterator();
        if (it.hasNext()) {
            final TypeSerializer typeSer = _valueTypeSerializer;
            do {
                Object elem = it.next();
                if (elem == null) {
                    // [databind#5369] Support `@JsonInclude` in `Collection`
                    //                Need to handle this one also
                    if (needsFiltering && _suppressNulls) {
                        continue;
                    }
                    ctxt.defaultSerializeNullValue(g);
                    continue;
                }
                ValueSerializer<Object> serializer = _elementSerializer;
                if (serializer == null) {
                    Class<?> cc = elem.getClass();
                    serializer = _dynamicValueSerializers.serializerFor(cc);
                    if (serializer == null) {
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(ctxt, ctxt.constructSpecializedType(_elementType, cc));
                        } else {
                            serializer = _findAndAddDynamic(ctxt, cc);
                        }
                    }
                }
                // [databind#5369] Support `@JsonInclude` in `Collection`
                //                Let's do filtering before serialization
                if (needsFiltering && !_shouldSerializeElement(ctxt, elem, _elementSerializer)) {
                    continue;
                }
                if (typeSer == null) {
                    serializer.serialize(elem, g, ctxt);
                } else {
                    serializer.serializeWithType(elem, g, ctxt, typeSer);
                }
            } while (it.hasNext());
        }
    }
}
