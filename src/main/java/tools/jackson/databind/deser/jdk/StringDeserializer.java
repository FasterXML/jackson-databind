package tools.jackson.databind.deser.jdk;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.type.LogicalType;

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
            TypeDeserializer typeDeserializer) throws JacksonException
    {
        return deserialize(p, ctxt);
    }
}
