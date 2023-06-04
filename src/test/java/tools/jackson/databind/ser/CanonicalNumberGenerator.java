package tools.jackson.databind.ser;

import java.math.BigDecimal;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.core.util.JsonGeneratorDelegate;
import tools.jackson.databind.SerializerProvider;

public class CanonicalNumberGenerator extends JsonGeneratorDelegate {

    /**
     * TODO The constant should be public or the verify method. Copied from
     * `jackson-databing` class `NumberSerializer`
     */
    protected final static int MAX_BIG_DECIMAL_SCALE = 9999;

    private final ValueToString<BigDecimal> _bigDecimalToString;

    public CanonicalNumberGenerator(JsonGenerator gen, ValueToString<BigDecimal> bigDecimalToString) {
        super(gen);
        this._bigDecimalToString = bigDecimalToString;
    }

    @Override
    public JsonGenerator writeNumber(double v) throws JacksonException {
        BigDecimal wrapper = BigDecimal.valueOf(v);
        return writeNumber(wrapper);
    }

    @Override
    public JsonGenerator writeNumber(BigDecimal v) throws JacksonException {
        if (!verifyBigDecimalRange(v)) {
            // TODO Is there a better way? I can't call delegate._reportError(). 
            String msg = bigDecimalOutOfRangeError(v);
            throw new StreamWriteException(this, msg);
        }

        String converted = _bigDecimalToString.convert(v);
        return delegate.writeNumber(converted);
    }

    public static boolean verifyBigDecimalRange(BigDecimal value, SerializerProvider provider) {
        boolean result = verifyBigDecimalRange(value);

        if (!result) {
            provider.reportMappingProblem(bigDecimalOutOfRangeError(value));
        }

        return result;
    }

    public static boolean verifyBigDecimalRange(BigDecimal value) {
        int scale = value.scale();
        return ((scale >= -MAX_BIG_DECIMAL_SCALE) && (scale <= MAX_BIG_DECIMAL_SCALE));
    }

    // TODO Everyone should use the same method
    public static String bigDecimalOutOfRangeError(BigDecimal value) {
        return String.format(
                "Attempt to write plain `java.math.BigDecimal` (see StreamWriteFeature.WRITE_BIGDECIMAL_AS_PLAIN) with illegal scale (%d): needs to be between [-%d, %d]",
                value.scale(), MAX_BIG_DECIMAL_SCALE, MAX_BIG_DECIMAL_SCALE);
    }
}
