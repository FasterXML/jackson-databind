package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;

public class CanonicalBigDecimalToString implements ValueToString<BigDecimal> {

    public static final CanonicalBigDecimalToString INSTANCE = new CanonicalBigDecimalToString();

    @Override
    public String convert(BigDecimal value) {
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