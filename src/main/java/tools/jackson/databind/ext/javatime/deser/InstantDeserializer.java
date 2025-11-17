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

package tools.jackson.databind.ext.javatime.deser;

import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.time.temporal.TemporalAccessor;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.*;
import tools.jackson.core.io.NumberInput;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.ext.javatime.util.DecimalUtils;

/**
 * Deserializer for Java 8 temporal {@link Instant}s, {@link OffsetDateTime},
 * and {@link ZonedDateTime}s.
 *
 * @author Nick Williams
 */
public class InstantDeserializer<T extends Temporal>
    extends JSR310DateTimeDeserializerBase<T>
{
    private final static boolean DEFAULT_NORMALIZE_ZONE_ID = DateTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID.enabledByDefault();
    private final static boolean DEFAULT_ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS
        = DateTimeFeature.ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS.enabledByDefault();

    /**
     * Constants used to check if ISO 8601 time string is colon-less. See [jackson-modules-java8#131]
     */
    protected static final Pattern ISO8601_COLONLESS_OFFSET_REGEX = Pattern.compile("[+-][0-9]{4}(?=\\[|$)");

    // @since 2.18.2
    private static OffsetDateTime decimalToOffsetDateTime(FromDecimalArguments args) {
        // [jackson-modules-java8#308] Since 2.18.2 : Fix can't deserialize OffsetDateTime.MIN: Invalid value for EpochDay
        if (args.integer == OffsetDateTime.MIN.toEpochSecond() && args.fraction == OffsetDateTime.MIN.getNano()) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(OffsetDateTime.MIN.toEpochSecond(), OffsetDateTime.MIN.getNano()), OffsetDateTime.MIN.getOffset());
        }
        // [jackson-modules-java8#308] Since 2.18.2 : For OffsetDateTime.MAX case
        if (args.integer == OffsetDateTime.MAX.toEpochSecond() && args.fraction == OffsetDateTime.MAX.getNano()) {
            return OffsetDateTime.ofInstant(Instant.ofEpochSecond(OffsetDateTime.MAX.toEpochSecond(), OffsetDateTime.MAX.getNano()), OffsetDateTime.MAX.getOffset());
        }
        return OffsetDateTime.ofInstant(Instant.ofEpochSecond(args.integer, args.fraction), args.zoneId);
    }

    public static final InstantDeserializer<Instant> INSTANT = new InstantDeserializer<>(
            Instant.class, DateTimeFormatter.ISO_INSTANT,
            Instant::from,
            a -> Instant.ofEpochMilli(a.value),
            a -> Instant.ofEpochSecond(a.integer, a.fraction),
            null,
            true, // yes, replace zero offset with Z
            DEFAULT_NORMALIZE_ZONE_ID,
            DEFAULT_ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS
    );

    public static final InstantDeserializer<OffsetDateTime> OFFSET_DATE_TIME = new InstantDeserializer<>(
            OffsetDateTime.class, DateTimeFormatter.ISO_OFFSET_DATE_TIME,
            OffsetDateTime::from,
            a -> OffsetDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId),
            InstantDeserializer::decimalToOffsetDateTime,
            (d, z) -> (d.isEqual(OffsetDateTime.MIN) || d.isEqual(OffsetDateTime.MAX) ? d : d.withOffsetSameInstant(z.getRules().getOffset(d.toLocalDateTime()))),
            true, // yes, replace zero offset with Z
            DEFAULT_NORMALIZE_ZONE_ID,
            DEFAULT_ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS
    );

    public static final InstantDeserializer<ZonedDateTime> ZONED_DATE_TIME = new InstantDeserializer<>(
            ZonedDateTime.class, DateTimeFormatter.ISO_ZONED_DATE_TIME,
            ZonedDateTime::from,
            a -> ZonedDateTime.ofInstant(Instant.ofEpochMilli(a.value), a.zoneId),
            a -> ZonedDateTime.ofInstant(Instant.ofEpochSecond(a.integer, a.fraction), a.zoneId),
            ZonedDateTime::withZoneSameInstant,
            false, // keep zero offset and Z separate since zones explicitly supported
            DEFAULT_NORMALIZE_ZONE_ID,
            DEFAULT_ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS
    );

    protected final Function<FromIntegerArguments, T> fromMilliseconds;

    protected final Function<FromDecimalArguments, T> fromNanoseconds;

    protected final Function<TemporalAccessor, T> parsedToValue;

    protected final BiFunction<T, ZoneId, T> adjust;

    /**
     * In case of vanilla `Instant` we seem to need to translate "+0000 | +00:00 | +00"
     * timezone designator into plain "Z" for some reason; see
     * [jackson-modules-java8#18] for more info
     */
    protected final boolean replaceZeroOffsetAsZ;

    /**
     * Flag for <code>JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE</code>
     */
    protected final Boolean _adjustToContextTZOverride;

    /**
     * Flag for <code>JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS</code>
     */
    protected final Boolean _readTimestampsAsNanosOverride;

    /**
     * @since 2.16
     */
    protected InstantDeserializer(Class<T> supportedType,
            DateTimeFormatter formatter,
            Function<TemporalAccessor, T> parsedToValue,
            Function<FromIntegerArguments, T> fromMilliseconds,
            Function<FromDecimalArguments, T> fromNanoseconds,
            BiFunction<T, ZoneId, T> adjust,
            boolean replaceZeroOffsetAsZ,
            boolean normalizeZoneId,
            boolean readNumericStringsAsTimestamp
    )
    {
        super(supportedType, formatter);
        this.parsedToValue = parsedToValue;
        this.fromMilliseconds = fromMilliseconds;
        this.fromNanoseconds = fromNanoseconds;
        this.adjust = adjust == null ? ((d, z) -> d) : adjust;
        this.replaceZeroOffsetAsZ = replaceZeroOffsetAsZ;
        _adjustToContextTZOverride = null;
        _readTimestampsAsNanosOverride = null;
    }

    @SuppressWarnings("unchecked")
    protected InstantDeserializer(InstantDeserializer<T> base, DateTimeFormatter f)
    {
        super((Class<T>) base.handledType(), f);
        parsedToValue = base.parsedToValue;
        fromMilliseconds = base.fromMilliseconds;
        fromNanoseconds = base.fromNanoseconds;
        adjust = base.adjust;
        replaceZeroOffsetAsZ = (_formatter == DateTimeFormatter.ISO_INSTANT);
        _adjustToContextTZOverride = base._adjustToContextTZOverride;
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    @SuppressWarnings("unchecked")
    protected InstantDeserializer(InstantDeserializer<T> base, Boolean adjustToContextTimezoneOverride)
    {
        super((Class<T>) base.handledType(), base._formatter);
        parsedToValue = base.parsedToValue;
        fromMilliseconds = base.fromMilliseconds;
        fromNanoseconds = base.fromNanoseconds;
        adjust = base.adjust;
        replaceZeroOffsetAsZ = base.replaceZeroOffsetAsZ;
        _adjustToContextTZOverride = adjustToContextTimezoneOverride;
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    @SuppressWarnings("unchecked")
    protected InstantDeserializer(InstantDeserializer<T> base, DateTimeFormatter f, Boolean leniency)
    {
        super((Class<T>) base.handledType(), f, leniency);
        parsedToValue = base.parsedToValue;
        fromMilliseconds = base.fromMilliseconds;
        fromNanoseconds = base.fromNanoseconds;
        adjust = base.adjust;
        replaceZeroOffsetAsZ = (_formatter == DateTimeFormatter.ISO_INSTANT);
        _adjustToContextTZOverride = base._adjustToContextTZOverride;
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }

    protected InstantDeserializer(InstantDeserializer<T> base,
        Boolean leniency,
        DateTimeFormatter formatter,
        JsonFormat.Shape shape,
        Boolean adjustToContextTimezoneOverride,
        Boolean readTimestampsAsNanosOverride)
    {
        super(base, leniency, formatter, shape);
        parsedToValue = base.parsedToValue;
        fromMilliseconds = base.fromMilliseconds;
        fromNanoseconds = base.fromNanoseconds;
        adjust = base.adjust;
        replaceZeroOffsetAsZ = base.replaceZeroOffsetAsZ;
        _adjustToContextTZOverride = adjustToContextTimezoneOverride;
        _readTimestampsAsNanosOverride = readTimestampsAsNanosOverride;
    }

    /*
    @SuppressWarnings("unchecked")
    protected InstantDeserializer(InstantDeserializer<T> base,
            DatatypeFeatures features)
    {
        super((Class<T>) base.handledType(), base._formatter);
        parsedToValue = base.parsedToValue;
        fromMilliseconds = base.fromMilliseconds;
        fromNanoseconds = base.fromNanoseconds;
        adjust = base.adjust;
        replaceZeroOffsetAsZ = base.replaceZeroOffsetAsZ;
        _adjustToContextTZOverride = base._adjustToContextTZOverride;
        _readTimestampsAsNanosOverride = base._readTimestampsAsNanosOverride;
    }
    */

    @Override
    protected InstantDeserializer<T> withDateFormat(DateTimeFormatter dtf) {
        if (dtf == _formatter) {
            return this;
        }
        return new InstantDeserializer<>(this, dtf);
    }

    @Override
    protected InstantDeserializer<T> withLeniency(Boolean leniency) {
        return new InstantDeserializer<>(this, _formatter, leniency);
    }

    @SuppressWarnings("unchecked")
    @Override // @since 2.12.1
    protected JSR310DateTimeDeserializerBase<?> _withFormatOverrides(DeserializationContext ctxt,
            BeanProperty property, JsonFormat.Value formatOverrides)
    {
        InstantDeserializer<T> deser = (InstantDeserializer<T>) super._withFormatOverrides(ctxt,
                property, formatOverrides);
        Boolean adjustToContextTZOverride = formatOverrides.getFeature(
            JsonFormat.Feature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
        Boolean readTimestampsAsNanosOverride = formatOverrides.getFeature(
            JsonFormat.Feature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
        if (!Objects.equals(adjustToContextTZOverride, deser._adjustToContextTZOverride)
            || !Objects.equals(readTimestampsAsNanosOverride, deser._readTimestampsAsNanosOverride)) {
            return new InstantDeserializer<>(deser, deser._isLenient, deser._formatter,
                deser._shape, adjustToContextTZOverride, readTimestampsAsNanosOverride);
        }
        return deser;
    }

    @SuppressWarnings("unchecked")
    @Override
    public T deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        //NOTE: Timestamps contain no timezone info, and are always in configured TZ. Only
        //string values have to be adjusted to the configured TZ.
        switch (p.currentTokenId())
        {
            case JsonTokenId.ID_NUMBER_FLOAT:
                return _fromDecimal(ctxt, p.getDecimalValue());
            case JsonTokenId.ID_NUMBER_INT:
                return _fromLong(ctxt, p.getLongValue());
            case JsonTokenId.ID_STRING:
                return _fromString(p, ctxt, p.getString());
            case JsonTokenId.ID_EMBEDDED_OBJECT:
                // 20-Apr-2016, tatu: Related to [databind#1208], can try supporting embedded
                //    values quite easily
                return (T) p.getEmbeddedObject();

            case JsonTokenId.ID_START_ARRAY:
                return _deserializeFromArray(p, ctxt);
            // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
            case JsonTokenId.ID_START_OBJECT:
                final String str = ctxt.extractScalarFromObject(p, this, handledType());
                // 17-May-2025, tatu: [databind#4656] need to check for `null`
                if (str != null) {
                    return _fromString(p, ctxt, str);
                }
                // fall through
        }
        return _handleUnexpectedToken(ctxt, p, JsonToken.VALUE_STRING,
                JsonToken.VALUE_NUMBER_INT, JsonToken.VALUE_NUMBER_FLOAT);
    }

    protected boolean shouldAdjustToContextTimezone(DeserializationContext context) {
        return (_adjustToContextTZOverride != null) ? _adjustToContextTZOverride :
                context.isEnabled(DateTimeFeature.ADJUST_DATES_TO_CONTEXT_TIME_ZONE);
    }

    protected boolean shouldReadTimestampsAsNanoseconds(DeserializationContext context) {
        return (_readTimestampsAsNanosOverride != null) ? _readTimestampsAsNanosOverride :
            context.isEnabled(DateTimeFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS);
    }

    // Helper method to find Strings of form "all digits" and "digits-comma-digits"
    protected int _countPeriods(String str)
    {
        int commas = 0;
        int i = 0;
        int ch = str.charAt(i);
        if (ch == '-' || ch == '+') {
            ++i;
        }
        for (int end = str.length(); i < end; ++i) {
            ch = str.charAt(i);
            if (ch < '0' || ch > '9') {
                if (ch == '.') {
                    ++commas;
                } else {
                    return -1;
                }
            }
        }
        return commas;
    }

    protected T _fromString(JsonParser p, DeserializationContext ctxt,
            String string0)
        throws JacksonException
    {
        String string = string0.trim();
        if (string.length() == 0) {
            // 22-Oct-2020, tatu: not sure if we should pass original (to distinguish
            //   b/w empty and blank); for now don't which will allow blanks to be
            //   handled like "regular" empty (same as pre-2.12)
            return _fromEmptyString(p, ctxt, string);
        }
        // only check for other parsing modes if we are using default formatter or explicitly asked to
        if (ctxt.isEnabled(DateTimeFeature.ALWAYS_ALLOW_STRINGIFIED_DATE_TIMESTAMPS) ||
                _formatter == DateTimeFormatter.ISO_INSTANT ||
                _formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME ||
                _formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME
            ) {
            // 22-Jan-2016, [datatype-jsr310#16]: Allow quoted numbers too
            int dots = _countPeriods(string);
            if (dots >= 0) { // negative if not simple number
                try {
                    if (dots == 0) {
                        return _fromLong(ctxt, NumberInput.parseLong(string));
                    }
                    if (dots == 1) {
                        return _fromDecimal(ctxt, NumberInput.parseBigDecimal(string, false));
                    }
                } catch (NumberFormatException e) {
                    // fall through to default handling, to get error there
                }
            }

            string = replaceZeroOffsetAsZIfNecessary(string);
        }

        // For some reason DateTimeFormatter.ISO_INSTANT only supports UTC ISO 8601 strings, so it have to be excluded
        if (_formatter == DateTimeFormatter.ISO_OFFSET_DATE_TIME ||
            _formatter == DateTimeFormatter.ISO_ZONED_DATE_TIME) {

            // 21-March-2021, Oeystein: Work-around to support basic iso 8601 format (colon-less).
            // As per JSR-310; Only extended 8601 formats (with colon) are supported for
            // ZonedDateTime.parse() and OffsetDateTime.parse().
            // https://github.com/FasterXML/jackson-modules-java8/issues/131
            string = addInColonToOffsetIfMissing(string);
        }

        T value;
        try {
            TemporalAccessor acc = _formatter.parse(string);
            value = parsedToValue.apply(acc);
            if (shouldAdjustToContextTimezone(ctxt)) {
                return adjust.apply(value, getZone(ctxt));
            }
        } catch (DateTimeException e) {
            value = _handleDateTimeFormatException(ctxt, e, _formatter, string);
        }
        return value;
    }

    protected T _fromLong(DeserializationContext context, long timestamp)
    {
        if(shouldReadTimestampsAsNanoseconds(context)){
            return fromNanoseconds.apply(new FromDecimalArguments(
                    timestamp, 0, this.getZone(context)
            ));
        }
        return fromMilliseconds.apply(new FromIntegerArguments(
                timestamp, this.getZone(context)));
    }

    protected T _fromDecimal(DeserializationContext context, BigDecimal value)
    {
        FromDecimalArguments args =
            DecimalUtils.extractSecondsAndNanos(value, (s, ns) -> new FromDecimalArguments(s, ns, getZone(context)),
                    // [modules-java8#359] since 2.21, Instant.ofEpochSecond() correctly handles
                    // negative nanoseconds, so no adjustment needed
                    false);
        return fromNanoseconds.apply(args);
    }

    private ZoneId getZone(DeserializationContext context)
    {
        // Instants are always in UTC, so don't waste compute cycles
        // Normalizing the zone to prevent discrepancies.
        // See https://github.com/FasterXML/jackson-modules-java8/pull/267 for details
        if (_valueClass == Instant.class) {
            return null;
        }
        ZoneId zoneId = context.getTimeZone().toZoneId();
        return context.isEnabled(DateTimeFeature.NORMALIZE_DESERIALIZED_ZONE_ID)
                ? zoneId.normalized() : zoneId;
    }

    private String replaceZeroOffsetAsZIfNecessary(String text)
    {
        if (replaceZeroOffsetAsZ) {
            return replaceZeroOffsetAsZ(text);
        }

        return text;
    }

    private static String replaceZeroOffsetAsZ(String text)
    {
        int plusIndex = text.lastIndexOf('+');
        if (plusIndex < 0) {
            return text;
        }
        int maybeOffsetIndex = plusIndex + 1;
        int remaining = text.length() - maybeOffsetIndex;
        switch (remaining) {
            case 2:
                return text.regionMatches(maybeOffsetIndex, "00", 0, remaining)
                        ? text.substring(0, plusIndex) + 'Z'
                        : text;
            case 4:
                return text.regionMatches(maybeOffsetIndex, "0000", 0, remaining)
                        ? text.substring(0, plusIndex) + 'Z'
                        : text;
            case 5:
                return text.regionMatches(maybeOffsetIndex, "00:00", 0, remaining)
                        ? text.substring(0, plusIndex) + 'Z'
                        : text;
        }
        return text;
    }

    // @since 2.13
    private static String addInColonToOffsetIfMissing(String text)
    {
        int timeIndex = text.indexOf('T');
        if (timeIndex < 0 || timeIndex > text.length() - 1) {
            return text;
        }

        int offsetIndex = text.indexOf('+', timeIndex + 1);
        if (offsetIndex < 0) {
            offsetIndex = text.indexOf('-', timeIndex + 1);
        }

        if (offsetIndex < 0 || offsetIndex > text.length() - 5) {
            return text;
        }

        int colonIndex = text.indexOf(':', offsetIndex);
        if (colonIndex == offsetIndex + 3) {
            return text;
        }

        if (Character.isDigit(text.charAt(offsetIndex + 1))
                && Character.isDigit(text.charAt(offsetIndex + 2))
                && Character.isDigit(text.charAt(offsetIndex + 3))
                && Character.isDigit(text.charAt(offsetIndex + 4))) {
            String match = text.substring(offsetIndex, offsetIndex + 5);
            return text.substring(0, offsetIndex)
                    + match.substring(0, 3) + ':' + match.substring(3)
                    + text.substring(offsetIndex + match.length());
        }

        // fallback to slow regex path, should be fully handled by the above
        final Matcher matcher = ISO8601_COLONLESS_OFFSET_REGEX.matcher(text);
        if (matcher.find()) {
            String match = matcher.group(0);
            return matcher.replaceFirst(match.substring(0, 3) + ':' + match.substring(3));
        }
        return text;
    }

    public static class FromIntegerArguments // since 2.8.3
    {
        public final long value;
        public final ZoneId zoneId;

        FromIntegerArguments(long value, ZoneId zoneId)
        {
            this.value = value;
            this.zoneId = zoneId;
        }
    }

    public static class FromDecimalArguments // since 2.8.3
    {
        public final long integer;
        public final int fraction;
        public final ZoneId zoneId;

        FromDecimalArguments(long integer, int fraction, ZoneId zoneId)
        {
            this.integer = integer;
            this.fraction = fraction;
            this.zoneId = zoneId;
        }
    }
}
