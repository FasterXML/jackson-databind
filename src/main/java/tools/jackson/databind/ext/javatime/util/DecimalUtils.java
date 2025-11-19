/*
 * Copyright 2013 FasterXML.com
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may
 * not use this file except in compliance with the License. You may obtain
 * a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the license for the specific language governing permissions and
 * limitations under the license.
 */

package tools.jackson.databind.ext.javatime.util;

import tools.jackson.core.io.NumberInput;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.function.BiFunction;

/**
 * Utilities to aid in the translation of decimal types to/from multiple parts.
 *
 * @author Nick Williams
 */
public final class DecimalUtils
{
    private DecimalUtils() { }

    public static String toDecimal(long seconds, int nanoseconds)
    {
        StringBuilder sb = new StringBuilder(20)
            .append(seconds)
            .append('.');
        // 14-Mar-2016, tatu: Although we do not yet (with 2.7) trim trailing zeroes,
        //   for general case,
        if (nanoseconds == 0L) {
            // !!! TODO: 14-Mar-2016, tatu: as per [datatype-jsr310], should trim
            //     trailing zeroes
            if (seconds == 0L) {
                return "0.0";
            }

//            sb.append('0');
            sb.append("000000000");
        } else {
            StringBuilder nanoSB = new StringBuilder(9);
            nanoSB.append(nanoseconds);
            // May need to both prepend leading nanos (if value less than 0.1)
            final int nanosLen = nanoSB.length();
            int prepZeroes = 9 - nanosLen;
            while (prepZeroes > 0) {
                --prepZeroes;
                sb.append('0');
            }

            // !!! TODO: 14-Mar-2016, tatu: as per [datatype-jsr310], should trim
            //     trailing zeroes
            /*
            // AND possibly trim trailing ones
            int i = nanosLen;
            while ((i > 1) && nanoSB.charAt(i-1) == '0') {
                --i;
            }
            if (i < nanosLen) {
                nanoSB.setLength(i);
            }
            */
            sb.append(nanoSB);
        }
        return sb.toString();
    }

    /**
     * Factory method for constructing {@link BigDecimal} out of second, nano-second
     * components.
     */
    public static BigDecimal toBigDecimal(long seconds, int nanoseconds)
    {
        // [modules-java8#359] For negative seconds with positive nanos (times before epoch),
        // we need to compute the proper decimal value: seconds + (nanos / 1_000_000_000)
        // Example: Instant{epochSecond=-1, nano=999000000} represents -0.001 seconds
        if (seconds < 0 && nanoseconds > 0) {
            return BigDecimal.valueOf(seconds)
                .add(BigDecimal.valueOf(nanoseconds).scaleByPowerOfTen(-9));
        }

        if (nanoseconds == 0L) {
            // 14-Mar-2015, tatu: Let's retain one zero to avoid interpretation
            //    as integral number
            if (seconds == 0L) { // except for "0.0" where it cannot be done without scientific notation
                return BigDecimal.ZERO.setScale(1);
            }
            return BigDecimal.valueOf(seconds).setScale(9);
        }
        return NumberInput.parseBigDecimal(toDecimal(seconds, nanoseconds), false);
    }

    /**
     * Extracts the seconds and nanoseconds component of {@code seconds} as {@code long} and {@code int}
     * values, passing them to the given converter.   The implementation avoids latency issues present
     * on some JRE releases.
     *
     * @since 2.19
     */
    public static <T> T extractSecondsAndNanos(BigDecimal seconds,
            BiFunction<Long, Integer, T> convert, boolean negativeAdjustment) {
        // Complexity is here to workaround unbounded latency in some BigDecimal operations.
        //   https://github.com/FasterXML/jackson-databind/issues/2141
        long secondsOnly;
        int nanosOnly;

        BigDecimal nanoseconds = seconds.scaleByPowerOfTen(9);
        if (nanoseconds.precision() - nanoseconds.scale() <= 0) {
            // There are no non-zero digits to the left of the decimal point.
            // This protects against very negative exponents.
            secondsOnly = nanosOnly = 0;
        }
        else if (seconds.scale() < -63) {
            // There would be no low-order bits once we chop to a long.
            // This protects against very positive exponents.
            secondsOnly = nanosOnly = 0;
        }
        else {
            // Now we know that seconds has reasonable scale, we can safely chop it apart.
            secondsOnly = seconds.longValue();
            nanosOnly = nanoseconds.subtract(BigDecimal.valueOf(secondsOnly).scaleByPowerOfTen(9)).intValue();

            if (secondsOnly < 0 && secondsOnly > Instant.MIN.getEpochSecond()) {
                // [modules-java8#337] since 2.19, not always we need to adjust nanos
                if (negativeAdjustment) {
                    // Issue #69 and Issue #120: avoid sending a negative adjustment to the Instant constructor, we want this as the actual nanos
                    nanosOnly = Math.abs(nanosOnly);
                }
            }
        }

        return convert.apply(secondsOnly, nanosOnly);
    }
}
