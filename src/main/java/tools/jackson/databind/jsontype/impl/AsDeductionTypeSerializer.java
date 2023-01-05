package tools.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.type.WritableTypeId;
import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.jsontype.TypeSerializer;

public class AsDeductionTypeSerializer extends TypeSerializerBase
{
    private final static AsDeductionTypeSerializer INSTANCE = new AsDeductionTypeSerializer();

    protected AsDeductionTypeSerializer() {
        super(null, null);
    }

    public static AsDeductionTypeSerializer instance() {
        return INSTANCE;
    }

    @Override
    public TypeSerializer forProperty(SerializerProvider ctxt, BeanProperty prop) {
        return this;
    }

    // This isn't really right but there's no "none" option
    @Override
    public As getTypeInclusion() { return As.EXISTING_PROPERTY; }

    @Override
    public WritableTypeId writeTypePrefix(JsonGenerator g, SerializerProvider ctxt,
            WritableTypeId idMetadata) throws JacksonException
    {
        // NOTE: We can NOT simply skip writing since we may have to
        // write surrounding Object or Array start/end markers. But
        // we are not to generate type id to write (compared to base class)

        if (idMetadata.valueShape.isStructStart()
                // also: do not try to write native type id
                && !g.canWriteTypeId()) {
            return g.writeTypePrefix(idMetadata);
        }
        return null;
    }

    @Override
    public WritableTypeId writeTypeSuffix(JsonGenerator g, SerializerProvider ctxt,
            WritableTypeId idMetadata) throws JacksonException
    {
        return (idMetadata == null) ? null
            : g.writeTypeSuffix(idMetadata);
    }
}
