package perf;

import com.fasterxml.jackson.annotation.JsonFormat;

public class NumberTestData {
    @JsonFormat(pattern = "0.00")
    private Double v1;
    private Double v2;
    private Double v3;
    private Double v4;
    @JsonFormat(pattern = "0.00")
    private double v5;

    public Double getV1() {
        return v1;
    }

    public NumberTestData setV1(Double v1) {
        this.v1 = v1;
        return this;
    }

    public Double getV2() {
        return v2;
    }

    public NumberTestData setV2(Double v2) {
        this.v2 = v2;
        return this;
    }

    public Double getV3() {
        return v3;
    }

    public NumberTestData setV3(Double v3) {
        this.v3 = v3;
        return this;
    }

    public Double getV4() {
        return v4;
    }

    public NumberTestData setV4(Double v4) {
        this.v4 = v4;
        return this;
    }

    public double getV5() {
        return v5;
    }

    public NumberTestData setV5(double v5) {
        this.v5 = v5;
        return this;
    }
}
