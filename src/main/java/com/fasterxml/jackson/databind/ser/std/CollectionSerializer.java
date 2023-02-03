package com.fasterxml.jackson.databind.ser.std;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.jsontype.TypeSerializer;
import com.fasterxml.jackson.databind.ser.ContainerSerializer;
import com.fasterxml.jackson.databind.ser.impl.PropertySerializerMap;

/**
 * Fallback serializer for cases where Collection is not known to be
 * of type for which more specializer serializer exists (such as
 * index-accessible List).
 * If so, we will just construct an {@link java.util.Iterator}
 * to iterate over elements.
 */
public class CollectionSerializer
    extends AsArraySerializerBase<Collection<?>>
{
    private static final long serialVersionUID = 1L;

    /*
    /**********************************************************
    /* Life-cycle
    /**********************************************************
     */

    /**
     * @since 2.6
     */
    public CollectionSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            JsonSerializer<Object> valueSerializer) {
        super(Collection.class, elemType, staticTyping, vts, valueSerializer);
    }

    /**
     * @deprecated since 2.6
     */
    @Deprecated // since 2.6
    public CollectionSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            BeanProperty property, JsonSerializer<Object> valueSerializer) {
        // note: assumption is 'property' is always passed as null
        this(elemType, staticTyping, vts, valueSerializer);
    }

    public CollectionSerializer(CollectionSerializer src,
            BeanProperty property, TypeSerializer vts, JsonSerializer<?> valueSerializer,
            Boolean unwrapSingle) {
        super(src, property, vts, valueSerializer, unwrapSingle);
    }

    @Override
    public ContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new CollectionSerializer(this, _property, vts, _elementSerializer, _unwrapSingle);
    }

    @Override
    public CollectionSerializer withResolved(BeanProperty property,
            TypeSerializer vts, JsonSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new CollectionSerializer(this, property, vts, elementSerializer, unwrapSingle);
    }

    /*
    /**********************************************************
    /* Accessors
    /**********************************************************
     */

    @Override
    public boolean isEmpty(SerializerProvider prov, Collection<?> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(Collection<?> value) {
        return value.size() == 1;
    }

    /*
    /**********************************************************
    /* Actual serialization
    /**********************************************************
     */

    @Override
    public final void serialize(Collection<?> value, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null) &&
                    provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, g, provider);
                return;
            }
        }
        g.writeStartArray(value, len);
        serializeContents(value, g, provider);
        g.writeEndArray();
    }

    @Override
    public void serializeContents(Collection<?> value, JsonGenerator g, SerializerProvider provider) throws IOException
    {
        g.setCurrentValue(value);
        if (_elementSerializer != null) {
            serializeContentsUsing(value, g, provider, _elementSerializer);
            return;
        }
        Iterator<?> it = value.iterator();
        if (!it.hasNext()) {
            return;
        }
        PropertySerializerMap serializers = _dynamicSerializers;
        final TypeSerializer typeSer = _valueTypeSerializer;

        int i = 0;
        try {
            do {
                Object elem = it.next();
                if (elem == null) {
                    provider.defaultSerializeNull(g);
                } else {
                    Class<?> cc = elem.getClass();
                    JsonSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(serializers,
                                    provider.constructSpecializedType(_elementType, cc), provider);
                        } else {
                            serializer = _findAndAddDynamic(serializers, cc, provider);
                        }
                        serializers = _dynamicSerializers;
                    }
                    if (typeSer == null) {
                        serializer.serialize(elem, g, provider);
                    } else {
                        serializer.serializeWithType(elem, g, provider, typeSer);
                    }
                }
                ++i;
            } while (it.hasNext());
        } catch (Exception e) {
            wrapAndThrow(provider, e, value, i);
        }
    }

    public void serializeContentsUsing(Collection<?> value, JsonGenerator g, SerializerProvider provider,
            JsonSerializer<Object> ser) throws IOException
    {
        Iterator<?> it = value.iterator();
        if (it.hasNext()) {
            TypeSerializer typeSer = _valueTypeSerializer;
            int i = 0;
            do {
                Object elem = it.next();
                try {
                    if (elem == null) {
                        provider.defaultSerializeNull(g);
                    } else {
                        if (typeSer == null) {
                            ser.serialize(elem, g, provider);
                        } else {
                            ser.serializeWithType(elem, g, provider, typeSer);
                        }
                    }
                    ++i;
                } catch (Exception e) {
                    wrapAndThrow(provider, e, value, i);
                }
            } while (it.hasNext());
        }
    }
}
