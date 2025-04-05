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

package tools.jackson.databind.ext.javatime.ser;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Serializer for Java 8 temporal {@link Instant}s, {@link OffsetDateTime}, and {@link ZonedDateTime}s.
 *
 * @author Nick Williams
 */
public class InstantSerializer extends InstantSerializerBase<Instant>
{
    public static final InstantSerializer INSTANCE = new InstantSerializer();

    protected InstantSerializer() {
        super(Instant.class, Instant::toEpochMilli, Instant::getEpochSecond, Instant::getNano,
                // null -> use 'value.toString()', default format
                null);
    }

    /*
    protected InstantSerializer(InstantSerializer base,
            Boolean useTimestamp, DateTimeFormatter formatter) {
        this(base, formatter, useTimestamp, base._useNanoseconds, base);
    }
    */

    protected InstantSerializer(InstantSerializer base, DateTimeFormatter formatter,
            Boolean useTimestamp, Boolean useNanoseconds,
            JsonFormat.Shape shape) {
        super(base, formatter, useTimestamp, useNanoseconds, shape);
    }

    @Override
    protected JSR310FormattedSerializerBase<Instant> withFormat(DateTimeFormatter formatter,
            Boolean useTimestamp,
            JsonFormat.Shape shape) {
        return new InstantSerializer(this, formatter, useTimestamp, this._useNanoseconds , shape);
    }

    @Override
    protected JSR310FormattedSerializerBase<?> withFeatures(Boolean writeZoneId, Boolean writeNanoseconds) {
        return new InstantSerializer(this, _formatter, _useTimestamp, writeNanoseconds,
                this._shape);
    }
}
