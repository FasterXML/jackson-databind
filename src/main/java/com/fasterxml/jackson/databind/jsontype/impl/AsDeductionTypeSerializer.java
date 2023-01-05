package com.fasterxml.jackson.databind.jsontype.impl;

import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.type.WritableTypeId;
import com.fasterxml.jackson.databind.BeanProperty;

/**
 * @since 2.14.2
 */
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
    public AsDeductionTypeSerializer forProperty(BeanProperty prop) {
        return this;
    }

    // This isn't really right but there's no "none" option
    @Override
    public As getTypeInclusion() { return As.EXISTING_PROPERTY; }

    @Override
    public WritableTypeId writeTypePrefix(JsonGenerator g,
            WritableTypeId idMetadata) throws IOException
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
    public WritableTypeId writeTypeSuffix(JsonGenerator g,
            WritableTypeId idMetadata) throws IOException
    {
        return (idMetadata == null) ? null
            : g.writeTypeSuffix(idMetadata);
    }
}
