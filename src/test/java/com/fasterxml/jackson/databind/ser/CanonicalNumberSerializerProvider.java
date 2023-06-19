package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public interface CanonicalNumberSerializerProvider {
    StdSerializer<BigDecimal> getNumberSerializer();
    ValueToString<BigDecimal> getValueToString();
}
