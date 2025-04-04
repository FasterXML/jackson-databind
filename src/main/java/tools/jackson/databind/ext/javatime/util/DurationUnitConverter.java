package tools.jackson.databind.ext.javatime.util;

import static tools.jackson.databind.ext.javatime.util.DurationUnitConverter.DurationSerialization.deserializer;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalUnit;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Handles the conversion of the duration based on the API of {@link Duration} for a restricted set of {@link ChronoUnit}.
 * Only the units considered as accurate are supported in this converter since are the only ones capable of handling
 * deserialization in a precise manner (see {@link ChronoUnit#isDurationEstimated}).
 *
 * @since 2.12
 */
public class DurationUnitConverter {

    protected static class DurationSerialization {
        final Function<Duration, Long> serializer;
        final Function<Long, Duration> deserializer;

        DurationSerialization(
                Function<Duration, Long> serializer,
                Function<Long, Duration> deserializer) {
            this.serializer = serializer;
            this.deserializer = deserializer;
        }

        static Function<Long, Duration> deserializer(TemporalUnit unit) {
            return v -> Duration.of(v, unit);
        }
    }

    private final static Map<String, DurationSerialization> UNITS;

    static {
        Map<String, DurationSerialization> units = new LinkedHashMap<>();
        units.put(ChronoUnit.NANOS.name(), new DurationSerialization(Duration::toNanos, deserializer(ChronoUnit.NANOS)));
        units.put(ChronoUnit.MICROS.name(), new DurationSerialization(d -> d.toNanos() / 1000, deserializer(ChronoUnit.MICROS)));
        units.put(ChronoUnit.MILLIS.name(), new DurationSerialization(Duration::toMillis, deserializer(ChronoUnit.MILLIS)));
        units.put(ChronoUnit.SECONDS.name(), new DurationSerialization(Duration::getSeconds, deserializer(ChronoUnit.SECONDS)));
        units.put(ChronoUnit.MINUTES.name(), new DurationSerialization(Duration::toMinutes, deserializer(ChronoUnit.MINUTES)));
        units.put(ChronoUnit.HOURS.name(), new DurationSerialization(Duration::toHours, deserializer(ChronoUnit.HOURS)));
        units.put(ChronoUnit.HALF_DAYS.name(), new DurationSerialization(d -> d.toHours() / 12, deserializer(ChronoUnit.HALF_DAYS)));
        units.put(ChronoUnit.DAYS.name(), new DurationSerialization(Duration::toDays, deserializer(ChronoUnit.DAYS)));
        UNITS = units;
    }


    final DurationSerialization serialization;

    DurationUnitConverter(DurationSerialization serialization) {
        this.serialization = serialization;
    }

    public Duration convert(long value) {
        return serialization.deserializer.apply(value);
    }

    public long convert(Duration duration) {
        return serialization.serializer.apply(duration);
    }

    /**
     * @return Description of all allowed valued as a sequence of
     * double-quoted values separated by comma
     */
    public static String descForAllowed() {
        return "\"" + UNITS.keySet().stream()
                .collect(Collectors.joining("\", \""))
                + "\"";
    }

    public static DurationUnitConverter from(String unit) {
        DurationSerialization def = UNITS.get(unit);
        return (def == null) ? null : new DurationUnitConverter(def);
    }
}
