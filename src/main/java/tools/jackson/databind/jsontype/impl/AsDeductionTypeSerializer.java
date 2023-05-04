package tools.jackson.databind.jsontype.impl;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.JsonToken;
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

        if (idMetadata.valueShape.isStructStart()) {
            // 03-May-2023, tatu: [databind#3914]: should not write Native Type Id;
            //   but may need to write the value start marker
            if (g.canWriteTypeId()) {
                idMetadata.wrapperWritten = false;
                if (idMetadata.valueShape == JsonToken.START_OBJECT) {
                    g.writeStartObject(idMetadata.forValue);
                } else if (idMetadata.valueShape == JsonToken.START_ARRAY) {
                    g.writeStartArray(idMetadata.forValue);
                }
                return idMetadata;
            }
            // But for non-wrapper types can just use the default handling
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
