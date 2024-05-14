package com.fasterxml.jackson.databind.ser.std;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;

import java.io.IOException;
import java.text.DecimalFormat;

public class NumberDecimalFormatSerializer extends ToStringSerializer {
    private final DecimalFormat decimalFormat;

    public NumberDecimalFormatSerializer(String pattern) {
        this.decimalFormat = new DecimalFormat(pattern);
    }

    @Override
    public void serialize(Object value, JsonGenerator gen, SerializerProvider provider) throws IOException {
        super.serialize(decimalFormat.format(value), gen, provider);
    }
}
