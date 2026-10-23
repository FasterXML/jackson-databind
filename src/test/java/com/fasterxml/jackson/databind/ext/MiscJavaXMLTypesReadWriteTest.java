package com.fasterxml.jackson.databind.ext;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Core XML types (javax.xml) are considered "external" (or more precisely "optional")
 * since some Java(-like) platforms do not include them: specifically, Google AppEngine
 * and Android seem to skimp on their inclusion. As such, they are dynamically loaded
 * only as needed, and need bit special handling.
 */
public class MiscJavaXMLTypesReadWriteTest
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper POLY_MAPPER = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL)
            .build();

    /*
    /**********************************************************************
    /* Serializer tests
    /**********************************************************************
     */

    @Test
    public void testQNameSer() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        assertEquals(q(qn.toString()), MAPPER.writeValueAsString(qn));
    }

    @Test
    public void testDurationSer() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value
        Duration dur = dtf.newDurationDayTime(false, 15, 19, 58, 1);
        assertEquals(q(dur.toString()), MAPPER.writeValueAsString(dur));
    }

    @Test
    public void testXMLGregorianCalendarSerAndDeser() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);

        long timestamp = cal.toGregorianCalendar().getTimeInMillis();
        String numStr = String.valueOf(timestamp);
        assertEquals(numStr, MAPPER.writeValueAsString(cal));

        // [JACKSON-403] Needs to come back ok as well:
        XMLGregorianCalendar calOut = MAPPER.readValue(numStr, XMLGregorianCalendar.class);
        assertNotNull(calOut);
        assertEquals(timestamp, calOut.toGregorianCalendar().getTimeInMillis());

        ObjectMapper mapper = new ObjectMapper();
        // and then textual variant
        mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false);
        // this is ALMOST same as default for XMLGregorianCalendar... just need to unify Z/+0000
        String exp = cal.toXMLFormat();
        String act = mapper.writeValueAsString(cal);
        act = act.substring(1, act.length() - 1); // remove quotes
        exp = removeZ(exp);
        act = removeZ(act);
        assertEquals(exp, act);
    }

    private String removeZ(String dateStr) {
        if (dateStr.endsWith("Z")) {
            return dateStr.substring(0, dateStr.length()-1);
        }
        if (dateStr.endsWith("+0000")) {
            return dateStr.substring(0, dateStr.length()-5);
        }
        if (dateStr.endsWith("+00:00")) {
            return dateStr.substring(0, dateStr.length()-6);
        }
        return dateStr;
    }

    /*
    /**********************************************************************
    /* Deserializer tests
    /**********************************************************************
     */

    // First things first: must be able to load the deserializers...
    @Test
    public void testDeserializerLoading()
    {
        CoreXMLDeserializers sers = new CoreXMLDeserializers();
        TypeFactory f = TypeFactory.defaultInstance();
        sers.findBeanDeserializer(f.constructType(Duration.class), null, null);
        sers.findBeanDeserializer(f.constructType(XMLGregorianCalendar.class), null, null);
        sers.findBeanDeserializer(f.constructType(QName.class), null, null);
    }

    @Test
    public void testQNameDeser() throws Exception
    {
        QName qn = new QName("http://abc", "tag", "prefix");
        String qstr = qn.toString();
        assertEquals(qn, MAPPER.readValue(q(qstr), QName.class),
            "Should deserialize to equal QName (exp serialization: '"+qstr+"')");

        // [databind#4450]
        qn = MAPPER.readValue(q(""), QName.class);
        assertNotNull(qn);
        assertEquals("", qn.getLocalPart());
    }

    @Test
    public void testXMLGregorianCalendarDeser() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        XMLGregorianCalendar cal = dtf.newXMLGregorianCalendar
            (1974, 10, 10, 18, 15, 17, 123, 0);
        String exp = cal.toXMLFormat();
        assertEquals(cal, MAPPER.readValue(q(exp), XMLGregorianCalendar.class),
            "Should deserialize to equal XMLGregorianCalendar ('"+exp+"')");
    }

    @Test
    public void testDurationDeser() throws Exception
    {
        DatatypeFactory dtf = DatatypeFactory.newInstance();
        // arbitrary value, like... say, 27d5h15m59s
        Duration dur = dtf.newDurationDayTime(true, 27, 5, 15, 59);
        String exp = dur.toString();
        assertEquals(dur, MAPPER.readValue(q(exp), Duration.class),
            "Should deserialize to equal Duration ('"+exp+"')");
    }

    /*
    /**********************************************************************
    /* Polymorphic handling tests
    /**********************************************************************
     */


    @Test
    public void testPolymorphicXMLGregorianCalendar() throws Exception
    {
        XMLGregorianCalendar cal = DatatypeFactory.newInstance().newXMLGregorianCalendar
                (1974, 10, 10, 18, 15, 17, 123, 0);
        String json = POLY_MAPPER.writeValueAsString(cal);
        Object result = POLY_MAPPER.readValue(json, Object.class);
        if (!(result instanceof XMLGregorianCalendar)) {
            fail("Expected a `XMLGregorianCalendar`, got: "+result.getClass());
        }
        assertEquals(cal, result);
    }

    /*
    /**********************************************************************
    /* StreamReadConstraints validation tests
    /**********************************************************************
     */

    @Test
    public void testDurationNumberLengthConstraint() throws Exception
    {
        // Create a mapper with a small maxNumberLength to test the constraint
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(100).build())
                .build();
        ObjectMapper constrainedMapper = JsonMapper.builder(jsonFactory).build();

        // A Duration with a year component that has 120 digits should exceed the limit
        String bigDuration = "\"P" + repeatString("9", 120) + "Y\"";
        try {
            constrainedMapper.readValue(bigDuration, Duration.class);
            fail("Should not pass: expected StreamConstraintsException for oversized Duration numeric component");
        } catch (StreamConstraintsException e) {
            verifyException(e, "exceeds the maximum allowed");
        }

        // A normal-length Duration should still work
        Duration dur = constrainedMapper.readValue("\"P1Y2M3D\"", Duration.class);
        assertNotNull(dur);
        assertEquals(1, dur.getYears());
        assertEquals(2, dur.getMonths());
        assertEquals(3, dur.getDays());
    }

    @Test
    public void testXMLGregorianCalendarNumberLengthConstraint() throws Exception
    {
        // Create a mapper with a small maxNumberLength to test the constraint
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(100).build())
                .build();
        ObjectMapper constrainedMapper = JsonMapper.builder(jsonFactory).build();

        // Time-only value (valid xs:time lexical form, not handled by _parseDate):
        // without the constraint check this falls through to
        // DatatypeFactory.newXMLGregorianCalendar(), which parses the fractional
        // seconds via the O(n^2) BigDecimal(String) constructor.
        String bigCalendar = "\"00:00:00." + repeatString("9", 120) + "\"";
        try {
            constrainedMapper.readValue(bigCalendar, XMLGregorianCalendar.class);
            fail("Should not pass: expected StreamConstraintsException for oversized XMLGregorianCalendar fractional seconds");
        } catch (StreamConstraintsException e) {
            verifyException(e, "exceeds the maximum allowed");
        }

        // A normal-length XMLGregorianCalendar should still work
        XMLGregorianCalendar cal = constrainedMapper.readValue("\"2023-01-01T00:00:00\"", XMLGregorianCalendar.class);
        assertNotNull(cal);
        assertEquals(2023, cal.getYear());
        assertEquals(1, cal.getMonth());
        assertEquals(1, cal.getDay());
        assertEquals(0, cal.getHour());
        assertEquals(0, cal.getMinute());
        assertEquals(0, cal.getSecond());

        // Use a standard ISO-8601 date-time format (the kind _parseDate handles) but
        // with an excessively long year component. The validation fires before _parseDate
        // is reached, so this should be rejected by the constraint check.
        String bigDateTime = "\"" + repeatString("9", 120) + "-01-01T00:00:00\"";
        try {
            constrainedMapper.readValue(bigDateTime, XMLGregorianCalendar.class);
            fail("Should not pass: expected StreamConstraintsException for oversized date-time value");
        } catch (StreamConstraintsException e) {
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    // Same checks but relying on default StreamReadConstraints (1000), without
    // any explicit configuration
    @Test
    public void testDefaultNumberLengthConstraints() throws Exception
    {
        final int len = StreamReadConstraints.DEFAULT_MAX_NUM_LEN + 100;

        try {
            MAPPER.readValue(q("P" + repeatString("9", len) + "Y"), Duration.class);
            fail("Should not pass: expected StreamConstraintsException for oversized Duration");
        } catch (StreamConstraintsException e) {
            verifyException(e, "exceeds the maximum allowed");
        }

        try {
            MAPPER.readValue(q("00:00:00." + repeatString("9", len)),
                    XMLGregorianCalendar.class);
            fail("Should not pass: expected StreamConstraintsException for oversized XMLGregorianCalendar");
        } catch (StreamConstraintsException e) {
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    // QName has no numeric components, so the number-length check must not
    // apply to it: long local parts remain bound by max String length only
    @Test
    public void testQNameNotConstrainedByNumberLength() throws Exception
    {
        String localPart = repeatString("a", StreamReadConstraints.DEFAULT_MAX_NUM_LEN + 100);
        QName qn = MAPPER.readValue(q(localPart), QName.class);
        assertEquals(localPart, qn.getLocalPart());
    }
}
