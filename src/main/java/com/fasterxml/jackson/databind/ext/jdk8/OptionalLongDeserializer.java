package com.fasterxml.jackson.databind.ext.jdk8;

import java.io.IOException;
import java.util.OptionalLong;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;

public class OptionalLongDeserializer extends BaseScalarOptionalDeserializer<OptionalLong>
{
    private static final long serialVersionUID = 1L;

    static final OptionalLongDeserializer INSTANCE = new OptionalLongDeserializer();

    public OptionalLongDeserializer() {
        super(OptionalLong.class, OptionalLong.empty());
    }

    @Override
    public OptionalLong deserialize(JsonParser p, DeserializationContext ctxt) throws IOException
    {
        // minor optimization, first, for common case
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return OptionalLong.of(p.getLongValue());
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
            return OptionalLong.of(_parseLongPrimitive(ctxt, text));
        case JsonTokenId.ID_NUMBER_FLOAT:
            if (!ctxt.isEnabled(DeserializationFeature.ACCEPT_FLOAT_AS_INT)) {
                _failDoubleToIntCoercion(p, ctxt, "long");
            }
            return OptionalLong.of(p.getValueAsLong());
        case JsonTokenId.ID_NULL:
            return _empty;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final OptionalLong parsed = deserialize(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;
            }
            break;
        }
        return (OptionalLong) ctxt.handleUnexpectedToken(_valueClass, p);
    }
}
