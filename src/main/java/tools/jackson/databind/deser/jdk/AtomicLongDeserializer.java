package tools.jackson.databind.deser.jdk;

import java.util.concurrent.atomic.AtomicLong;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.annotation.JacksonStdImpl;
import tools.jackson.databind.deser.std.StdScalarDeserializer;
import tools.jackson.databind.type.LogicalType;
import tools.jackson.databind.util.AccessPattern;

@JacksonStdImpl
public class AtomicLongDeserializer extends StdScalarDeserializer<AtomicLong>
{
    public AtomicLongDeserializer() { super(AtomicLong.class); }

    @Override
    public LogicalType logicalType() { return LogicalType.Integer; }

    @Override // @since 2.12
    public Object getEmptyValue(DeserializationContext ctxt) {
        return new AtomicLong();
    }

    // [databind#6437]: a new instance per call, as "empty" value is mutable
    // and must not be shared (CONSTANT would let callers cache one instance)
    @Override
    public AccessPattern getEmptyAccessPattern() {
        return AccessPattern.DYNAMIC;
    }

    @Override
    public AtomicLong deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (p.isExpectedNumberIntToken()) {
            return new AtomicLong(p.getLongValue());
        }
        // 12-Jun-2020, tatu: May look convoluted, but need to work correctly with
        //   CoercionConfig
        Long L = _parseLong(p, ctxt, AtomicLong.class);
        return (L == null) ? null : new AtomicLong(L.longValue());
    }
}
