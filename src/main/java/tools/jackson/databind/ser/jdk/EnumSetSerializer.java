package tools.jackson.databind.ser.jdk;

import java.util.*;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.jsontype.TypeSerializer;
import tools.jackson.databind.ser.std.AsArraySerializerBase;

public class EnumSetSerializer
    extends AsArraySerializerBase<EnumSet<? extends Enum<?>>>
{
    public EnumSetSerializer(JavaType elemType) {
        super(EnumSet.class, elemType, true, null, null);
    }

    public EnumSetSerializer(EnumSetSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        super(src, vts, valueSerializer, unwrapSingle, property);
    }

    @Override
    protected EnumSetSerializer _withValueTypeSerializer(TypeSerializer vts) {
        // no typing for enums (always "hard" type)
        return this;
    }

    @Override
    public EnumSetSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new EnumSetSerializer(this, vts, elementSerializer, unwrapSingle, property);
    }

    @Override
    public boolean isEmpty(SerializerProvider prov, EnumSet<? extends Enum<?>> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(EnumSet<? extends Enum<?>> value) {
        return value.size() == 1;
    }

    @Override
    public final void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator gen,
            SerializerProvider provider) throws JacksonException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null)
                    && provider.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
                    || (_unwrapSingle == Boolean.TRUE)) {
                serializeContents(value, gen, provider);
                return;
            }
        }
        gen.writeStartArray(value, len);
        serializeContents(value, gen, provider);
        gen.writeEndArray();
    }

    @Override
    public void serializeContents(EnumSet<? extends Enum<?>> value, JsonGenerator gen,
            SerializerProvider ctxt)
        throws JacksonException
    {
        ValueSerializer<Object> enumSer = _elementSerializer;
        // Need to dynamically find instance serializer; unfortunately that seems
        // to be the only way to figure out type (no accessors to the enum class that set knows)
        for (Enum<?> en : value) {
            if (enumSer == null) {
                // 12-Jan-2010, tatu: Since enums cannot be polymorphic, let's
                //   not bother with typed serializer variant here
                enumSer = _findAndAddDynamic(ctxt, en.getDeclaringClass());
            }
            enumSer.serialize(en, gen, ctxt);
        }
    }
}
