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

    @Deprecated // since 3.1
    public EnumSetSerializer(EnumSetSerializer src,
            TypeSerializer vts, ValueSerializer<?> valueSerializer,
            Boolean unwrapSingle, BeanProperty property) {
        this(src, vts, valueSerializer, unwrapSingle, property, null, false);
    }

    /**
     * @since 3.1
     */
    public EnumSetSerializer(EnumSetSerializer src,
             TypeSerializer vts, ValueSerializer<?> valueSerializer,
             Boolean unwrapSingle, BeanProperty property,
             Object suppressableValue, boolean suppressNulls) {
        super(src, vts, valueSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    @Override
    protected EnumSetSerializer _withValueTypeSerializer(TypeSerializer vts) {
        // no typing for enum elements (always strongly typed), so don't change
        return this;
    }

    @Deprecated // @since 3.1
    @Override
    protected EnumSetSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle) {
        return new EnumSetSerializer(this, vts, elementSerializer, unwrapSingle, property, null, false);
    }

    @Override
    public EnumSetSerializer withResolved(BeanProperty property,
            TypeSerializer vts, ValueSerializer<?> elementSerializer,
            Boolean unwrapSingle, Object suppressableValue, boolean suppressNulls) {
        return new EnumSetSerializer(this, vts, elementSerializer, unwrapSingle, property, suppressableValue, suppressNulls);
    }

    @Override
    public boolean isEmpty(SerializationContext prov, EnumSet<? extends Enum<?>> value) {
        return value.isEmpty();
    }

    @Override
    public boolean hasSingleElement(EnumSet<? extends Enum<?>> value) {
        return value.size() == 1;
    }

    @Override
    public final void serialize(EnumSet<? extends Enum<?>> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final int len = value.size();
        if (len == 1) {
            if (((_unwrapSingle == null)
                    && ctxt.isEnabled(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED))
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
    public void serializeContents(EnumSet<? extends Enum<?>> value, JsonGenerator g,
            SerializationContext ctxt)
        throws JacksonException
    {
        final boolean needsFiltering = _needToCheckFiltering(ctxt);
        g.assignCurrentValue(value);
        ValueSerializer<Object> enumSer = _elementSerializer;
        // Need to dynamically find instance serializer; unfortunately that seems
        // to be the only way to figure out type (no accessors to the enum class that set knows)
        for (Enum<?> en : value) {
            if (enumSer == null) {
                // 12-Jan-2010, tatu: Since enums cannot be polymorphic, let's
                //   not bother with typed serializer variant here
                enumSer = _findAndAddDynamic(ctxt, en.getDeclaringClass());
            }
            // [databind#5369] Support @JsonInclude in Collection
            if (needsFiltering && !_shouldSerializeElement(ctxt, en, enumSer)) {
                continue;
            }
            enumSer.serialize(en, g, ctxt);
        }
    }
}
