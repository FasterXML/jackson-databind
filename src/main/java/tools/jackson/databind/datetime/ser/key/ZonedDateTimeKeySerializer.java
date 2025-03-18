package tools.jackson.databind.datetime.ser.key;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;

import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.datetime.util.DecimalUtils;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;

public class ZonedDateTimeKeySerializer extends ValueSerializer<ZonedDateTime> {

    public static final ZonedDateTimeKeySerializer INSTANCE = new ZonedDateTimeKeySerializer();

    private ZonedDateTimeKeySerializer() {
        // singleton
    }

    @Override
    public void serialize(ZonedDateTime value, JsonGenerator g, SerializationContext ctxt)
        throws JacksonException
    {
        /* [modules-java8#127]: Serialization of timezone data is disabled by default, but can be
         * turned on by enabling `SerializationFeature.WRITE_DATES_WITH_ZONE_ID`
         */
        if (ctxt.isEnabled(SerializationFeature.WRITE_DATES_WITH_ZONE_ID)) {
            g.writeName(DateTimeFormatter.ISO_ZONED_DATE_TIME.format(value));
        } else if (useTimestamps(ctxt)) {
            if (useNanos(ctxt)) {
                g.writeName(DecimalUtils.toBigDecimal(value.toEpochSecond(), value.getNano()).toString());
            } else {
                g.writeName(String.valueOf(value.toInstant().toEpochMilli()));
            }
        } else {
            g.writeName(DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(value));
        }
    }

    private static boolean useNanos(SerializationContext ctxt) {
        return ctxt.isEnabled(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    private static boolean useTimestamps(SerializationContext ctxt) {
        return ctxt.isEnabled(SerializationFeature.WRITE_DATE_KEYS_AS_TIMESTAMPS);
    }
}
