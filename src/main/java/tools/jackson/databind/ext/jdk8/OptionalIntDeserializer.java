package tools.jackson.databind.ext.jdk8;

import java.util.OptionalInt;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonToken;
import tools.jackson.core.JsonTokenId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.type.LogicalType;

public class OptionalIntDeserializer extends BaseScalarOptionalDeserializer<OptionalInt>
{
    static final OptionalIntDeserializer INSTANCE = new OptionalIntDeserializer();

    public OptionalIntDeserializer() {
        super(OptionalInt.class, OptionalInt.empty());
    }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override
    public OptionalInt deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        // minor optimization, first, for common case
        if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
            return OptionalInt.of(p.getIntValue());
        }
        CoercionAction act;
        switch (p.currentTokenId()) {
        case JsonTokenId.ID_STRING:
            String text = p.getText();
            act = _checkFromStringCoercion(ctxt, text);
            if (act == CoercionAction.AsNull) {
                return (OptionalInt) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (OptionalInt) getEmptyValue(ctxt);
            }
            text = text.trim();
            if (_checkTextualNull(ctxt, text)) {
                return (OptionalInt) getNullValue(ctxt);
            }
            return OptionalInt.of(_parseIntPrimitive(ctxt, text));
        case JsonTokenId.ID_NUMBER_FLOAT:
            act = _checkFloatToIntCoercion(p, ctxt, _valueClass);
            if (act == CoercionAction.AsNull) {
                return (OptionalInt) getNullValue(ctxt);
            }
            if (act == CoercionAction.AsEmpty) {
                return (OptionalInt) getEmptyValue(ctxt);
            }
            return OptionalInt.of(p.getValueAsInt());
        case JsonTokenId.ID_NULL:
            return _empty;
        case JsonTokenId.ID_START_ARRAY:
            if (ctxt.isEnabled(DeserializationFeature.UNWRAP_SINGLE_VALUE_ARRAYS)) {
                p.nextToken();
                final OptionalInt parsed = deserialize(p, ctxt);
                _verifyEndArrayForSingle(p, ctxt);
                return parsed;            
            }
            break;
        default:
        }
        return (OptionalInt) ctxt.handleUnexpectedToken(getValueType(ctxt), p);
    }
}
