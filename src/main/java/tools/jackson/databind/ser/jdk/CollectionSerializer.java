package tools.jackson.databind.ser.jdk;

import java.util.Collection;
import java.util.EnumSet;
import java.util.Iterator;

import tools.jackson.core.*;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.impl.PropertySerializerMap;
import tools.jackson.databind.ser.std.AsArraySerializerBase;
import tools.jackson.databind.ser.std.StdContainerSerializer;

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
    /**
     * Flag that indicates that we may need to check for EnumSet dynamically
     * during serialization: problem being that we can't always do it statically.
     * But we can figure out when there is a possibility wrt type signature we get.
     */
    private final boolean _maybeEnumSet;

    /*
    /**********************************************************************
    /* Life-cycle
    /**********************************************************************
     */

    public CollectionSerializer(JavaType elemType, boolean staticTyping, TypeSerializer vts,
            ValueSerializer<Object> valueSerializer) {
        super(Collection.class, elemType, staticTyping, vts, valueSerializer);
        // Unfortunately we can't check for EnumSet statically (if type indicated it,
        // we'd have constructed `EnumSetSerializer` instead). But we can check that
        // element type could possibly be an Enum.
        _maybeEnumSet = elemType.isEnumType() || elemType.isJavaLangObject();
    }

    protected CollectionSerializer(CollectionSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        super(src, vts, valueSerializer, unwrapSingle, property);
        _maybeEnumSet = src._maybeEnumSet;
    }

    @Override
    protected StdContainerSerializer<?> _withValueTypeSerializer(TypeSerializer vts) {
        return new CollectionSerializer(this, vts, _elementSerializer, _unwrapSingle, _property);
    }

    @Override
    protected CollectionSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new CollectionSerializer(this, vts, elementSerializer, unwrapSingle, property);
    }

    /*
    /**********************************************************************
    /* Accessors
    /**********************************************************************
     */

    @Override
    public boolean isEmpty(SerializationContext prov, Collection<?> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(Collection<?> value) {
        return value.size() == 1;
    }

    /*
    /**********************************************************************
    /* Actual serialization
    /**********************************************************************
     */

    @Override
    public final void serialize(Collection<?> value, JsonGenerator g, SerializationContext provider)
        throws JacksonException
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
    public void serializeContents(Collection<?> value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        if (_elementSerializer != null) {
            serializeContentsUsing(value, g, ctxt, _elementSerializer);
            return;
        }
        Iterator<?> it = value.iterator();
        if (!it.hasNext()) {
            return;
        }
        PropertySerializerMap serializers = _dynamicValueSerializers;
        // [databind#4849]/[databind#4214]: need to check for EnumSet
        final TypeSerializer typeSer = (_maybeEnumSet && value instanceof EnumSet<?>)
                ? null : _valueTypeSerializer;

        int i = 0;
        try {
            do {
                Object elem = it.next();
                if (elem == null) {
                    ctxt.defaultSerializeNullValue(g);
                } else {
                    Class<?> cc = elem.getClass();
                    ValueSerializer<Object> serializer = serializers.serializerFor(cc);
                    if (serializer == null) {
                        if (_elementType.hasGenericTypes()) {
                            serializer = _findAndAddDynamic(ctxt, ctxt.constructSpecializedType(_elementType, cc));
                        } else {
                            serializer = _findAndAddDynamic(ctxt, cc);
                        }
                        serializers = _dynamicValueSerializers;
                    }
                    if (typeSer == null) {
                        serializer.serialize(elem, g, ctxt);
                    } else {
                        serializer.serializeWithType(elem, g, ctxt, typeSer);
                    }
                }
                ++i;
            } while (it.hasNext());
        } catch (Exception e) {
            wrapAndThrow(ctxt, e, value, i);
        }
    }

    public void serializeContentsUsing(Collection<?> value, JsonGenerator g, SerializationContext provider,
            ValueSerializer<Object> ser)
        throws JacksonException
    {
        Iterator<?> it = value.iterator();
        if (it.hasNext()) {
            // [databind#4849]/[databind#4214]: need to check for EnumSet
            final TypeSerializer typeSer = (_maybeEnumSet && value instanceof EnumSet<?>)
                    ? null : _valueTypeSerializer;
            int i = 0;
            do {
                Object elem = it.next();
                try {
                    if (elem == null) {
                        provider.defaultSerializeNullValue(g);
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
