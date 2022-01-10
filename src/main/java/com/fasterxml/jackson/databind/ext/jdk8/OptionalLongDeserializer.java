package com.fasterxml.jackson.databind.ext.jdk8;

import java.util.OptionalLong;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.type.LogicalType;

public class OptionalLongDeserializer extends BaseScalarOptionalDeserializer<OptionalLong>
{
    static final OptionalLongDeserializer INSTANCE = new OptionalLongDeserializer();

    public OptionalLongDeserializer() {
        super(OptionalLong.class, OptionalLong.empty());
    }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override
    public OptionalLong deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // minor optimization, first, for common case
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return OptionalLong.of(p.getLongValue());
        }
        CoercionAction act;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            String text = p.getText();
            act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (OptionalLong) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (OptionalLong) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (OptionalLong) getNullValue(ctxt);
            }
            return OptionalLong.of(_parseLongPrimitive(ctxt, text));
        case JsonTokenId.ID_NUMBER_FLOAT:
            act = _checkFloatToIntCoercion(p, ctxt, _valueClass);
            if (act == CoercionAction.AsNull) {
                return (OptionalLong) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (OptionalLong) getEmptyValue(ctxt);
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
        return (OptionalLong) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }
}
