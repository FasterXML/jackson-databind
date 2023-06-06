package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class PrettyBigDecimalSerializer extends StdSerializer<BigDecimal>
        implements ValueToString<BigDecimal> {

    private static final long serialVersionUID = 1L;

    public static final PrettyBigDecimalSerializer INSTANCE = new PrettyBigDecimalSerializer();

    public static final CanonicalNumberSerializerProvider PROVIDER = new CanonicalNumberSerializerProvider() {
        @Override
        public StdSerializer<BigDecimal> getNumberSerializer() {
            return INSTANCE;
        }

        @Override
        public ValueToString<BigDecimal> getValueToString() {
            return INSTANCE;
        }
    };

    protected PrettyBigDecimalSerializer() {
        super(BigDecimal.class);
    }

    @Override
    public void serialize(BigDecimal value, JsonGenerator gen, SerializerProvider provider)
            throws IOException {
        CanonicalNumberGenerator.verifyBigDecimalRange(value, provider);

        String output = convert(value);
        gen.writeNumber(output);
    }

    @Override
    public String convert(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}