package tools.jackson.databind.ser;

import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializerProvider;
import tools.jackson.databind.ser.std.StdSerializer;

public class CanonicalBigDecimalSerializer extends StdSerializer<BigDecimal>
        implements ValueToString<BigDecimal> {

    protected CanonicalBigDecimalSerializer() {
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
        // TODO Convert to exponential form if necessary
        BigDecimal stripped = value.stripTrailingZeros();
        int scale = stripped.scale();
        String text = stripped.toPlainString();
        if (scale == 0) {
            return text;
        }

        int pos = text.indexOf('.');
        int exp;
        if (pos >= 0) {
            exp = pos - 1;

            if (exp == 0) {
                return text;
            }

            text = text.substring(0, pos) + text.substring(pos + 1);
        } else {
            exp = -scale;
            int end = text.length();
            while (end > 0 && text.charAt(end - 1) == '0') {
                end --;
            }
            text = text.substring(0, end);
        }

        if (text.length() == 1) {
            return text + 'E' + exp;
        }

        return text.substring(0, 1) + '.' + text.substring(1) + 'E' + exp;
    }
}