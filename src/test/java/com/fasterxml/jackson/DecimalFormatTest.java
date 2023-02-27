package com.fasterxml.jackson;

import java.math.RoundingMode;
import java.text.DecimalFormat;

public class DecimalFormatTest {
    public static void main(String[] args) {
        DecimalFormat df = new DecimalFormat("0.00");
        df.setRoundingMode(RoundingMode.HALF_EVEN);
        System.out.println(df.format(1.050));
        System.out.println(df.format(1.051));
        System.out.println(df.format(1.052));
        System.out.println(df.format(1.055));
        System.out.println(df.format(1.056));
        System.out.println(df.format(1.059));
    }
}
