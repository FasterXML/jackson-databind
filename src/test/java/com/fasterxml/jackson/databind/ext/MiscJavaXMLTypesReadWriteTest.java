package com.fasterxml.jackson.databind.ext;

import javax.xml.datatype.*;
import javax.xml.namespace.QName;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;
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
}
