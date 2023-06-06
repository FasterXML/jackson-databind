package tools.jackson.databind.ser;

import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ser.std.StdSerializer;

public class PrettyBigDecimalSerializer extends StdSerializer<BigDecimal>
        implements ValueToString<BigDecimal> {

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
            throws JacksonException {
        CanonicalNumberGenerator.verifyBigDecimalRange(value, provider);

        String output = convert(value);
        gen.writeNumber(output);
    }

    @Override
    public String convert(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}