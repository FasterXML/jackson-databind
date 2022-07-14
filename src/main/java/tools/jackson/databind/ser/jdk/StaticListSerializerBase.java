package tools.jackson.databind.ser.jdk;

import java.util.*;
import java.util.Objects;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.*;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.jsonFormatVisitors.JsonArrayFormatVisitor;
import tools.jackson.databind.jsonFormatVisitors.JsonFormatVisitorWrapper;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.StdSerializer;

/**
 * Intermediate base class for Lists, Collections and Arrays
 * that contain static (non-dynamic) value types.
 */
public abstract class StaticListSerializerBase<T extends Collection<?>>
    extends StdSerializer<T>
{
    /**
     * Setting for specific local override for "unwrap single element arrays":
     * true for enable unwrapping, false for preventing it, `null` for using
     * global configuration.
     */
    protected final Boolean _unwrapSingle;

    protected StaticListSerializerBase(Class<?> cls) {
        super(cls);
        _unwrapSingle = null;
    }

    protected StaticListSerializerBase(StaticListSerializerBase<?> src,
            Boolean unwrapSingle) {
        super(src);
        _unwrapSingle = unwrapSingle;
    }

    public abstract ValueSerializer<?> _withResolved(BeanProperty prop,
            Boolean unwrapSingle);

    /*
    /**********************************************************
    /* Post-processing
    /**********************************************************
     */

    @SuppressWarnings("unchecked")
    @Override
    public ValueSerializer<?> createContextual(SerializerProvider serializers,
            BeanProperty property)
    {
        ValueSerializer<?> ser = null;
        
        if (property != null) {
            final AnnotationIntrospector intr = serializers.getAnnotationIntrospector();
            AnnotatedMember m = property.getMember();
            if (m != null) {
                ser = serializers.serializerInstance(m,
                        intr.findContentSerializer(serializers.getConfig(), m));
            }
        }
        Boolean unwrapSingle = null;
        JsonFormat.Value format = findFormatOverrides(serializers, property, handledType());
        if (format != null) {
            unwrapSingle = format.getFeature(JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED);
        }
        // [databind#124]: May have a content converter
        ser = findContextualConvertingSerializer(serializers, property, ser);
        if (ser == null) {
            ser = serializers.findContentValueSerializer(String.class, property);
        }
        // Optimization: default serializer just writes String, so we can avoid a call:
        if (isDefaultSerializer(ser)) {
            if (Objects.equals(unwrapSingle, _unwrapSingle)) {
                return this;
            }
            return _withResolved(property, unwrapSingle);
        }
        // otherwise...
        // note: will never have TypeSerializer, because Strings are "natural" type
        return new CollectionSerializer(serializers.constructType(String.class),
                true, /*TypeSerializer*/ null, (ValueSerializer<Object>) ser);
    }

    @Override
    public boolean isEmpty(SerializerProvider provider, T value) {
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
            SerializerProvider provider, TypeSerializer typeSer) throws JacksonException;
}
