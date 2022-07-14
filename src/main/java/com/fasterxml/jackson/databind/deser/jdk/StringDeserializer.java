package com.fasterxml.jackson.databind.deser.jdk;

import tools.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JacksonStdImpl;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;
import com.fasterxml.jackson.databind.jsontype.TypeDeserializer;
import com.fasterxml.jackson.databind.type.LogicalType;

@JacksonStdImpl
public class StringDeserializer extends StdScalarDeserializer<String> // non-final since 2.9
{
    public final static StringDeserializer instance = new StringDeserializer();

    public StringDeserializer() { super(String.class); }

    @Override
    public LogicalType logicalType() {
        return LogicalType.Textual;
    }

    @Override
    public boolean isCachable() { return true; }

    @Override
    public Object getEmptyValue(DeserializationContext ctxt) {
        return "";
    }

    // Since default `getNullValue()` would just call `getEmptyValue()`, need to override
    @Override
    public Object getNullValue(DeserializationContext ctxt) {
        return null;
    }

    @Override
    public String deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return p.getText();
        }
        JsonToken t = p.currentToken();
        // [databind#381]
        if (t == JsonToken.START_ARRAY) {
            return _deserializeFromArray(p, ctxt);
        }
        // need to gracefully handle byte[] data, as base64
        if (t == JsonToken.VALUE_EMBEDDED_OBJECT) {
            Object ob = p.getEmbeddedObject();
            if (ob == null) {
                return null;
            }
            if (ob instanceof byte[]) {
                return ctxt.getBase64Variant().encode((byte[]) ob, false);
            }
            // otherwise, try conversion using toString()...
            return ob.toString();
        }
        // 29-Jun-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (t == JsonToken.START_OBJECT) {
            return ctxt.extractScalarFromObject(p, this, _valueClass);
        }
        // allow coercions for other scalar types
        // 17-Jan-2018, tatu: Related to [databind#1853] avoid FIELD_NAME by ensuring it's
        //   "real" scalar
        if (t.isScalarValue()) {
            String text = p.getValueAsString();
            if (text != null) {
                return text;
            }
        }
        return (String) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }

    // Since we can never have type info ("natural type"; String, Boolean, Integer, Double):
    // (is it an error to even call this version?)
    @Override
    public String deserializeWithType(JsonParser p, DeserializationContext ctxt,
            TypeDeserializer typeDeserializer) throws JacksonException
    {
        return deserialize(p, ctxt);
    }
}
