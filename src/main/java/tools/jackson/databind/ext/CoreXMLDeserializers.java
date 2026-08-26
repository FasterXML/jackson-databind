package tools.jackson.databind.ext;

import java.util.*;

import javax.xml.XMLConstants;
import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.FromStringDeserializer;

/**
 * Container deserializers that handle "core" XML types: ones included in standard
 * JDK 1.5. Types are directly needed by JAXB, but may be unavailable on some
 * limited platforms; hence separate out from basic deserializer factory.
 */
public class CoreXMLDeserializers
{
    protected final static QName EMPTY_QNAME = QName.valueOf("");

    /**
     * Data type factories are thread-safe after instantiation (and
     * configuration, if any); and since instantiation (esp. implementation
     * introspection) can be expensive we better reuse the instance.
     */
    final static DatatypeFactory _dataTypeFactory;
    static {
        try {
            _dataTypeFactory = DatatypeFactory.newInstance();
        } catch (DatatypeConfigurationException e) {
            throw new RuntimeException(e);
        }
    }

    public static ValueDeserializer<?> findBeanDeserializer(DeserializationConfig config,
            JavaType type)
    {
        Class<?> raw = type.getRawClass();
        if (raw == QName.class) {
            return new Std(raw, TYPE_QNAME);
        }
        if (raw == XMLGregorianCalendar.class) {
            return new Std(raw, TYPE_G_CALENDAR);
        }
        if (raw == Duration.class) {
            return new Std(raw, TYPE_DURATION);
        }
        return null;
    }

    public static boolean hasDeserializerFor(Class<?> valueType) {
        return (valueType == QName.class)
                || (valueType == XMLGregorianCalendar.class)
                || (valueType == Duration.class)
                ;
    }

    /*
    /**********************************************************************
    /* Concrete deserializers
    /**********************************************************************
     */

    protected final static int TYPE_DURATION = 1;
    protected final static int TYPE_G_CALENDAR = 2;
    protected final static int TYPE_QNAME = 3;

    /**
     * Combo-deserializer that supports deserialization of somewhat optional
     * javax.xml types {@link QName}, {@link Duration} and {@link XMLGregorianCalendar}.
     * Combined into a single class to eliminate bunch of one-off implementation
     * classes, to reduce resulting jar size (mostly).
     */
    public static class Std extends FromStringDeserializer<Object>
    {
        protected final int _kind;

        public Std(Class<?> raw, int kind) {
            super(raw);
            _kind = kind;
        }

        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            // GregorianCalendar also allows integer value (timestamp),
            // which needs separate handling
            if (_kind == TYPE_G_CALENDAR) {
                if (p.hasToken(JsonToken.VALUE_NUMBER_INT)) {
                    return _gregorianFromDate(ctxt, _parseDate(p, ctxt));
                }
            }
            // QName also allows object value, which needs separate handling.
            // PROPERTY_NAME: As.PROPERTY type deserializer has already consumed
            // START_OBJECT (and the type id), so the parser sits on the first
            // payload property: same object form as START_OBJECT. [databind#6175]
            if (_kind == TYPE_QNAME) {
                if (p.hasToken(JsonToken.START_OBJECT)
                        || p.hasToken(JsonToken.PROPERTY_NAME)) {
                    return _parseQNameObject(p, ctxt);
                }
            }
            return super.deserialize(p, ctxt);
        }

        private QName _parseQNameObject(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            JsonNode tree = ctxt.readTree(p);

            JsonNode localPart = tree.get("localPart");
            if (localPart == null) {
                ctxt.reportInputMismatch(this,
                        "Object value for `QName` is missing required property 'localPart'");
            }

            if (!localPart.isString()) {
                ctxt.reportInputMismatch(this,
                        "Object value property 'localPart' for `QName` must be of type STRING, not %s",
                        localPart.getNodeType());
            }

            // Both optional properties validated regardless of which ones are defined
            JsonNode namespaceURI = _optionalStringProperty(ctxt, tree, "namespaceURI");
            JsonNode prefix = _optionalStringProperty(ctxt, tree, "prefix");

            // Missing 'namespaceURI' same as empty one ("no namespace")
            final String ns = (namespaceURI == null)
                    ? XMLConstants.NULL_NS_URI : namespaceURI.asString();
            if (prefix != null) {
                return new QName(ns, localPart.asString(), prefix.asString());
            }
            return new QName(ns, localPart.asString());
        }

        /**
         * Helper method for accessing one of optional {@code QName} properties, verifying
         * that its value (if any) is of type STRING: explicit JSON {@code null} is taken
         * to mean "not defined", but other non-STRING values are rejected same as for
         * required 'localPart' (since {@code asString()} would coerce numbers and
         * booleans into their textual form).
         *
         * @return Node for the property if it has usable value; {@code null} if not defined
         */
        private JsonNode _optionalStringProperty(DeserializationContext ctxt,
                JsonNode tree, String propName)
            throws JacksonException
        {
            JsonNode n = tree.get(propName);
            if ((n == null) || n.isNull()) {
                return null;
            }
            if (!n.isString()) {
                ctxt.reportInputMismatch(this,
                        "Object value property '%s' for `QName` must be of type STRING, not %s",
                        propName, n.getNodeType());
            }
            return n;
        }

        @Override
        protected Object _deserialize(String value, DeserializationContext ctxt)
            throws JacksonException
        {
            switch (_kind) {
            case TYPE_DURATION:
                // [databind#6127] DatatypeFactory.newDuration() parses date/time components
                // into BigIntegers (and fractional seconds into BigDecimal), using the
                // O(n^2) BigInteger(String)/BigDecimal(String) constructors. Rather than
                // isolating individual components we check length of the whole value as a
                // conservative upper bound: it is never shorter than any single component,
                // and no valid Duration comes anywhere near the (default 1000) limit.
                _validateTimestampLength(ctxt, value);
                return _dataTypeFactory.newDuration(value);
            case TYPE_QNAME:
                return QName.valueOf(value);
            case TYPE_G_CALENDAR:
                // [databind#6127] Similar to TYPE_DURATION above: length of the whole value
                // is checked as a conservative upper bound for numeric components, before
                // handing value to DatatypeFactory.newXMLGregorianCalendar(), which parses
                // fractional seconds into a BigDecimal via O(n^2) BigDecimal(String).
                _validateTimestampLength(ctxt, value);
                Date d;
                try {
                    d = _parseDate(value, ctxt);
                } catch (DatabindException e) {
                    // try to parse from native XML Schema 1.0 lexical representation String,
                    // which includes time-only formats not handled by parseXMLGregorianCalendarFromJacksonFormat(...)
                    return _dataTypeFactory.newXMLGregorianCalendar(value);
                }
                return _gregorianFromDate(ctxt, d);
            }
            throw new IllegalStateException();
        }

        @Override
        protected Object _deserializeFromEmptyString(DeserializationContext ctxt) {
            if (_kind == TYPE_QNAME) {
                return EMPTY_QNAME;
            }
            return super._deserializeFromEmptyString(ctxt);
        }

        protected XMLGregorianCalendar _gregorianFromDate(DeserializationContext ctxt,
                Date d)
        {
            if (d == null) {
                return null;
            }
            GregorianCalendar calendar = new GregorianCalendar();
            calendar.setTime(d);
            TimeZone tz = ctxt.getTimeZone();
            if (tz != null) {
                calendar.setTimeZone(tz);
            }
            return _dataTypeFactory.newXMLGregorianCalendar(calendar);
        }
    }
}
