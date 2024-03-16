package com.fasterxml.jackson.databind.deser;

import java.math.BigDecimal;

final class BigDecimalWrapper {
    BigDecimal number;

    public BigDecimalWrapper() {}

    public BigDecimalWrapper(BigDecimal number) {
        this.number = number;
    }

    public void setNumber(BigDecimal number) {
        this.number = number;
    }
}
