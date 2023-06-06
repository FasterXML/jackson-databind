package com.fasterxml.jackson.databind.ser;

import java.math.BigDecimal;

import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

public class CanonicalJsonModule extends SimpleModule {
    private static final long serialVersionUID = 1L;

    public CanonicalJsonModule() {
        this(CanonicalBigDecimalSerializer.INSTANCE);
    }
    
    public CanonicalJsonModule(StdSerializer<BigDecimal> numberSerializer) {
        addSerializer(BigDecimal.class, numberSerializer);
    }
}
