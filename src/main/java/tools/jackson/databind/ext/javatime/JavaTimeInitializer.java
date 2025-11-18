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

package tools.jackson.databind.ext.javatime;

import java.time.*;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.deser.ValueInstantiator;
import tools.jackson.databind.deser.ValueInstantiators;
import tools.jackson.databind.deser.std.StdValueInstantiator;
import tools.jackson.databind.ext.javatime.deser.*;
import tools.jackson.databind.ext.javatime.deser.key.*;
import tools.jackson.databind.ext.javatime.ser.*;
import tools.jackson.databind.ext.javatime.ser.key.ZonedDateTimeKeySerializer;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.AnnotatedClassResolver;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.module.SimpleDeserializers;
import tools.jackson.databind.module.SimpleKeyDeserializers;
import tools.jackson.databind.module.SimpleSerializers;
import tools.jackson.databind.ser.std.ToStringSerializer;

/**
 * Class that registers capability of deserializing and serializing (reading and writing)
 * {@code java.time} values with the Jackson core.
 *<p>
 * In Jackson 3, the module is embedded in databind and handlers are automatically
 * registered: approach is similar to one used by full {@link JacksonModule}s.
 *<p>
 * Most {@code java.time} types are serialized as numbers (integers or decimals as appropriate) if the
 * {@link DateTimeFeature#WRITE_DATES_AS_TIMESTAMPS} feature is enabled
 * (or, for {@link Duration}, {@link DateTimeFeature#WRITE_DURATIONS_AS_TIMESTAMPS}),
 * and otherwise are serialized in standard
 * <a href="http://en.wikipedia.org/wiki/ISO_8601" target="_blank">ISO-8601</a> string representation.
 * ISO-8601 specifies formats for representing offset dates and times, zoned dates and times,
 * local dates and times, periods, durations, zones, and more. All {@code java.time} types
 * have built-in translation to and from ISO-8601 formats.
 * <p>
 * Granularity of timestamps is controlled through the companion features
 * {@link DateTimeFeature#WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS} and
 * {@link DateTimeFeature#READ_DATE_TIMESTAMPS_AS_NANOSECONDS}. For serialization, timestamps are
 * written as fractional numbers (decimals), where the number is seconds and the decimal is fractional seconds, if
 * {@code WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS} is enabled (it is by default), with resolution as fine as nanoseconds depending on the
 * underlying JDK implementation. If {@code WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS} is disabled, timestamps are written as a whole number of
 * milliseconds. At deserialization time, decimal numbers are always read as fractional second timestamps with up-to-nanosecond resolution,
 * since the meaning of the decimal is unambiguous. The more ambiguous integer types are read as fractional seconds without a decimal point
 * if {@code READ_DATE_TIMESTAMPS_AS_NANOSECONDS} is enabled (it is by default), and otherwise they are read as milliseconds.
 * <p>
 * Some exceptions to this standard serialization/deserialization rule:
 * <ul>
 * <li>{@link Period}, which always results in an ISO-8601 format because Periods must be represented in years, months, and/or days.
 *  </li>
 * <li>{@link Year}, which only contains a year and cannot be represented with a timestamp.
 *  </li>
 * <li>{@link YearMonth}, which only contains a year and a month and cannot be represented with a timestamp.
 *  </li>
 * <li>{@link MonthDay}, which only contains a month and a day and cannot be represented with a timestamp.
 *  </li>
 * <li>{@link ZoneId} and {@link ZoneOffset}, which do not actually store dates and times but are supported with this module nonetheless.
 *  </li>
 * <li>{@link LocalDate}, {@link LocalTime}, {@link LocalDateTime}, and {@link OffsetTime}, which cannot portably be converted to timestamps
 * and are instead represented as arrays when WRITE_DATES_AS_TIMESTAMPS is enabled.
 *  </li>
 * </ul>
 */
public final class JavaTimeInitializer
    implements java.io.Serializable
{
    private static final long serialVersionUID = 1L;
    private static final JavaTimeInitializer INSTANCE = new JavaTimeInitializer();

    public static JavaTimeInitializer getInstance() {
        return INSTANCE;
    }

    private JavaTimeInitializer() { }

    public void setupModule(JacksonModule.SetupContext context) {
        context.addDeserializers(new SimpleDeserializers()
            // // Instant variants:
            .addDeserializer(Instant.class, InstantDeserializer.INSTANT)
            .addDeserializer(OffsetDateTime.class, InstantDeserializer.OFFSET_DATE_TIME)
            .addDeserializer(ZonedDateTime.class, InstantDeserializer.ZONED_DATE_TIME)
    
            // // Other deserializers
            .addDeserializer(Duration.class, DurationDeserializer.INSTANCE)
            .addDeserializer(LocalDateTime.class, LocalDateTimeDeserializer.INSTANCE)
            .addDeserializer(LocalDate.class, LocalDateDeserializer.INSTANCE)
            .addDeserializer(LocalTime.class, LocalTimeDeserializer.INSTANCE)
            .addDeserializer(Month.class, MonthDeserializer.INSTANCE)
            .addDeserializer(MonthDay.class, MonthDayDeserializer.INSTANCE)
            .addDeserializer(OffsetTime.class, OffsetTimeDeserializer.INSTANCE)
            .addDeserializer(Period.class, JSR310StringParsableDeserializer.PERIOD)
            .addDeserializer(Year.class, YearDeserializer.INSTANCE)
            .addDeserializer(YearMonth.class, YearMonthDeserializer.INSTANCE)
            .addDeserializer(ZoneId.class, JSR310StringParsableDeserializer.ZONE_ID)
            .addDeserializer(ZoneOffset.class, JSR310StringParsableDeserializer.ZONE_OFFSET)
        );

        // then serializers:
        context.addSerializers(new SimpleSerializers()
            .addSerializer(Duration.class, DurationSerializer.INSTANCE)
            .addSerializer(Instant.class, InstantSerializer.INSTANCE)
            .addSerializer(LocalDateTime.class, LocalDateTimeSerializer.INSTANCE)
            .addSerializer(LocalDate.class, LocalDateSerializer.INSTANCE)
            .addSerializer(LocalTime.class, LocalTimeSerializer.INSTANCE)
            .addSerializer(Month.class, MonthSerializer.INSTANCE)
            .addSerializer(MonthDay.class, MonthDaySerializer.INSTANCE)
            .addSerializer(OffsetDateTime.class, OffsetDateTimeSerializer.INSTANCE)
            .addSerializer(OffsetTime.class, OffsetTimeSerializer.INSTANCE)
            .addSerializer(Period.class, new ToStringSerializer(Period.class))
            .addSerializer(Year.class, YearSerializer.INSTANCE)
            .addSerializer(YearMonth.class, YearMonthSerializer.INSTANCE)

            .addSerializer(ZonedDateTime.class, ZonedDateTimeSerializer.INSTANCE)

            // Need to override Type Id handling
            // (actual concrete type is `ZoneRegion`, but that's not visible)
            .addSerializer(ZoneId.class, new ZoneIdSerializer())

            .addSerializer(ZoneOffset.class, new ToStringSerializer(ZoneOffset.class))
        );

        // key serializers
        context.addKeySerializers(new SimpleSerializers()
                .addSerializer(ZonedDateTime.class, ZonedDateTimeKeySerializer.INSTANCE)
        );

        // key deserializers
        context.addKeyDeserializers(new SimpleKeyDeserializers()
            .addDeserializer(Duration.class, DurationKeyDeserializer.INSTANCE)
            .addDeserializer(Instant.class, InstantKeyDeserializer.INSTANCE)
            .addDeserializer(LocalDateTime.class, LocalDateTimeKeyDeserializer.INSTANCE)
            .addDeserializer(LocalDate.class, LocalDateKeyDeserializer.INSTANCE)
            .addDeserializer(LocalTime.class, LocalTimeKeyDeserializer.INSTANCE)
            .addDeserializer(MonthDay.class, MonthDayKeyDeserializer.INSTANCE)
            .addDeserializer(OffsetDateTime.class, OffsetDateTimeKeyDeserializer.INSTANCE)
            .addDeserializer(OffsetTime.class, OffsetTimeKeyDeserializer.INSTANCE)
            .addDeserializer(Period.class, PeriodKeyDeserializer.INSTANCE)
            .addDeserializer(Year.class, YearKeyDeserializer.INSTANCE)
            .addDeserializer(YearMonth.class, YearMonthKeyDeserializer.INSTANCE)
            .addDeserializer(ZonedDateTime.class, ZonedDateTimeKeyDeserializer.INSTANCE)
            .addDeserializer(ZoneId.class, ZoneIdKeyDeserializer.INSTANCE)
            .addDeserializer(ZoneOffset.class, ZoneOffsetKeyDeserializer.INSTANCE)
        );

        context.addValueInstantiators(new ValueInstantiators.Base() {
            @Override
            public ValueInstantiator modifyValueInstantiator(DeserializationConfig config,
                    BeanDescription.Supplier beanDescRef, ValueInstantiator defaultInstantiator)
            {
                JavaType type = beanDescRef.getType();
                Class<?> raw = type.getRawClass();

                // 15-May-2015, tatu: In theory not safe, but in practice we do need to do "fuzzy" matching
                // because we will (for now) be getting a subtype, but in future may want to downgrade
                // to the common base type. Even more, serializer may purposefully force use of base type.
                // So... in practice it really should always work, in the end. :)
                if (ZoneId.class.isAssignableFrom(raw)) {
                    // let's assume we should be getting "empty" StdValueInstantiator here:
                    if (defaultInstantiator instanceof StdValueInstantiator inst) {
                        // one further complication: we need ZoneId info, not sub-class
                        AnnotatedClass ac;
                        if (raw == ZoneId.class) {
                            ac = beanDescRef.getClassInfo();
                        } else {
                            // we don't need Annotations, so constructing directly is fine here
                            // even if it's not generally recommended
                            ac = AnnotatedClassResolver.resolve(config,
                                    config.constructType(ZoneId.class), config);
                        }
                        if (!inst.canCreateFromString()) {
                            AnnotatedMethod factory = _findFactory(ac, "of", String.class);
                            if (factory != null) {
                                inst.configureFromStringCreator(factory);
                            }
                            // otherwise... should we indicate an error?
                        }
                        // return ZoneIdInstantiator.construct(config, beanDesc, defaultInstantiator);
                    }
                }
                return defaultInstantiator;
            }
        });
    }

    private AnnotatedMethod _findFactory(AnnotatedClass cls, String name, Class<?>... argTypes)
    {
        final int argCount = argTypes.length;
        for (AnnotatedMethod method : cls.getFactoryMethods()) {
            if (!name.equals(method.getName())
                    || (method.getParameterCount() != argCount)) {
                continue;
            }
            for (int i = 0; i < argCount; ++i) {
                Class<?> argType = method.getParameter(i).getRawType();
                if (!argType.isAssignableFrom(argTypes[i])) {
                    continue;
                }
            }
            return method;
        }
        return null;
    }
}
