package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;

public class PrettyBigDecimalToString implements ValueToString<BigDecimal> {

    public static final PrettyBigDecimalToString INSTANCE = new PrettyBigDecimalToString();

    @Override
    public String convert(BigDecimal value) {
        return value.stripTrailingZeros().toPlainString();
    }
}