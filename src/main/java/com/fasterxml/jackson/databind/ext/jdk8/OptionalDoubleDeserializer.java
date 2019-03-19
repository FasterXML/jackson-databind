package com.fasterxml.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.OptionalDouble;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class OptionalDoubleDeserializer extends BaseScalarOptionalDeserializer<OptionalDouble>
{
    private static final long serialVersionUID = 1L;

    static final OptionalDoubleDeserializer INSTANCE = new OptionalDoubleDeserializer();

    public OptionalDoubleDeserializer() {
        super(OptionalDouble.class, OptionalDouble.empty());
    }

    @Override
    public OptionalDouble deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
        // minor optimization, first, for common case
        if (p.hasToken(JsonToken.VALUE_NUMBER_FLOAT)) {
            return OptionalDouble.of(p.getDoubleValue());
        }
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            String text = p.getText().trim();
            if ((text.length() == 0)) {
                _coerceEmptyString(ctxt, false);
                return _empty;
            }
            if (_hasTextualNull(text)) {
                _coerceTextualNull(ctxt, false);
                return _empty;
            }
            return OptionalDouble.of(_parseDoublePrimitive(ctxt, text));
        case JsonTokenId.ID_NUMBER_INT: // coercion here should be fine
            return OptionalDouble.of(p.getDoubleValue());
        case JsonTokenId.ID_NULL:
            return _empty;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final OptionalDouble parsed = deserialize(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            break;
        }
        return (OptionalDouble) ctxt.handleUnexpectedToken(_valueClass, p);
    }
}
