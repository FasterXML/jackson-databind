package com.fasterxml.jackson.databind.ser;

import java.io.IOException;
import java.math.BigDecimal;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.JsonGeneratorDelegate;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.SerializerProvider;

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
    public void writeNumber(double v) throws IOException {
        BigDecimal wrapper = BigDecimal.valueOf(v);
        writeNumber(wrapper);
    }

    @Override
    public void writeNumber(BigDecimal v) throws IOException {
        if (!verifyBigDecimalRange(v)) {
            String msg = bigDecimalOutOfRangeError(v);
            throw new JsonGenerationException(msg, this);
        }

        String converted = _bigDecimalToString.convert(v);
        delegate.writeNumber(converted);
    }

    public static boolean verifyBigDecimalRange(BigDecimal value, SerializerProvider provider) throws JsonMappingException {
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
