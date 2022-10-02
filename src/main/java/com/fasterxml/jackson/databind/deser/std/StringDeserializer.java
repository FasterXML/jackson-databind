package com.fasterxml.jackson.databind.deser.std;

import java.io.IOException;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

@JacksonStdImpl
public class StringDeserializer extends StdScalarDeserializer<String> // non-final since 2.9
{
    private static final long serialVersionUID = 1L;

    /**
     * @since 2.2
     */
    public final static StringDeserializer instance = new StringDeserializer();

    public StringDeserializer() { super(String.class); }

    @Override // since 2.12
    public LogicalType logicalType() {
        return LogicalType.Textual;
    }

    // since 2.6, slightly faster lookups for this very common type
    @Override
    public boolean isCachable() { return true; }

    @Override // since 2.9
    public Object getEmptyValue(DeserializationContext ctxt) throws JsonMappingException {
        return "";
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // The critical path: ensure we handle the common case first.
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return p.getText();
        }
        // [databind#381]
        if (p.hasToken(JsonToken.START_ARRAY)) {
            return _deserializeFromArray(p, ctxt);
        }
        return _parseString(p, ctxt, this);
    }

    // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
    // (is it an error to even call this version?)
    @Override
    public String deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws IOException {
        return deserialize(p, ctxt);
    }
}
