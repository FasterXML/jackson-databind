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

import java.time.DateTimeException;
import java.time.Period;
import java.time.ZoneId;
import java.time.ZoneOffset;

import com.fasterxml.jackson.annotation.JsonFormat;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonParser;

import tools.jackson.core.JsonToken;

import tools.jackson.core.util.VersionUtil;

import tools.jackson.databind.BeanProperty;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.CoercionAction;
import tools.jackson.databind.cfg.CoercionInputShape;
import tools.jackson.databind.jsontype.TypeDeserializer;

/**
 * Deserializer for all Java 8 temporal {@link java.time} types that cannot be represented
 * with numbers and that have parse functions that can take {@link String}s,
 * and where format is not configurable.
 *
 * @author Nick Williams
 * @author Tatu Saloranta
 */
public class JSR310StringParsableDeserializer
    extends JSR310DeserializerBase<Object>
{
    protected final static int TYPE_PERIOD = 1;
    protected final static int TYPE_ZONE_ID = 2;
    protected final static int TYPE_ZONE_OFFSET = 3;

    public static final ValueDeserializer<Period> PERIOD =
        createDeserializer(Period.class, TYPE_PERIOD);

    public static final ValueDeserializer<ZoneId> ZONE_ID =
        createDeserializer(ZoneId.class, TYPE_ZONE_ID);

    public static final ValueDeserializer<ZoneOffset> ZONE_OFFSET =
        createDeserializer(ZoneOffset.class, TYPE_ZONE_OFFSET);

    protected final int _typeSelector;

    @SuppressWarnings("unchecked")
    protected JSR310StringParsableDeserializer(Class<?> supportedType, int typeSelector)
    {
        super((Class<Object>)supportedType);
        _typeSelector = typeSelector;
    }

    protected JSR310StringParsableDeserializer(JSR310StringParsableDeserializer base, Boolean leniency) {
        super(base, leniency);
        _typeSelector = base._typeSelector;
    }

    @SuppressWarnings("unchecked")
    protected static <T> ValueDeserializer<T> createDeserializer(Class<T> type, int typeId) {
        return (ValueDeserializer<T>) new JSR310StringParsableDeserializer(type, typeId);
    }

    @Override
    protected JSR310StringParsableDeserializer withLeniency(Boolean leniency) {
        if (_isLenient == !Boolean.FALSE.equals(leniency)) {
            return this;
        }
        // TODO: or should this be casting as above in createDeserializer? But then in createContext, we need to
        // call the withLeniency method in this class. (See if we can follow InstantDeser convention here?)
        return new JSR310StringParsableDeserializer(this, leniency);
    }

    @Override
    public ValueDeserializer<?> createContextual(DeserializationContext ctxt,
            BeanProperty property)
    {
        JsonFormat.Value format = findFormatOverrides(ctxt, property, handledType());
        JSR310StringParsableDeserializer deser = this;
        if (format != null) {
            if (format.hasLenient()) {
                Boolean leniency = format.getLenient();
                if (leniency != null) {
                    deser = this.withLeniency(leniency);
                }
            }
        }
        return deser;
    }

    @Override
    public Object deserialize(JsonParser p, DeserializationContext ctxt)
        throws JacksonException
    {
        if (p.hasToken(JsonToken.VALUE_STRING)) {
            return _fromString(p, ctxt, p.getString());
        }
        // 30-Sep-2020, tatu: New! "Scalar from Object" (mostly for XML)
        if (p.isExpectedStartObjectToken()) {
            final String str = ctxt.extractScalarFromObject(p, this, handledType());
            // 17-May-2025, tatu: [databind#4656] need to check for `null`
            if (str != null) {
                return _fromString(p, ctxt, str);
            }
            // fall through
        } else if (p.hasToken(JsonToken.VALUE_EMBEDDED_OBJECT)) {
            // 20-Apr-2016, tatu: Related to [databind#1208], can try supporting embedded
            //    values quite easily
            return p.getEmbeddedObject();
        } else if (p.isExpectedStartArrayToken()) {
            return _deserializeFromArray(p, ctxt);
        }
        
        throw ctxt.wrongTokenException(p, handledType(), JsonToken.VALUE_STRING, null);
    }

    @Override
    public Object deserializeWithType(JsonParser p, DeserializationContext context,
            TypeDeserializer deserializer)
        throws JacksonException
    {
        // This is a nasty kludge right here, working around issues like
        // [datatype-jsr310#24]. But should work better than not having the work-around.
        JsonToken t = p.currentToken();
        if ((t != null) && t.isScalarValue()) {
            return deserialize(p, context);
        }
        return deserializer.deserializeTypedFromAny(p, context);
    }

    protected Object _fromString(JsonParser p, DeserializationContext ctxt,
            String string)
        throws JacksonException
    {
        string = string.trim();
        if (string.length() == 0) {
            CoercionAction act = ctxt.findCoercionAction(logicalType(), _valueClass,
                    CoercionInputShape.EmptyString);
            if (act == CoercionAction.Fail) {
                ctxt.reportInputMismatch(this,
"Cannot coerce empty String (\"\") to %s (but could if enabling coercion using `CoercionConfig`)",
_coercedTypeDesc());
            }
            // 21-Jun-2020, tatu: As of 2.12, leniency considered legacy setting,
            //    but still supported.
            if (!isLenient()) {
                return _failForNotLenient(p, ctxt, JsonToken.VALUE_STRING);
            }
            if (act == CoercionAction.AsEmpty) {
                return getEmptyValue(ctxt);
            }
            // None of the types has specific null value
            return null;
        }
        try {
            switch (_typeSelector) {
            case TYPE_PERIOD:
                return Period.parse(string);
            case TYPE_ZONE_ID:
                return ZoneId.of(string);
            case TYPE_ZONE_OFFSET:
                return ZoneOffset.of(string);
            }
        } catch (DateTimeException e) {
            return _handleDateTimeFormatException(ctxt, e, null, string);
        }
        VersionUtil.throwInternal();
        return null;
    }
}
