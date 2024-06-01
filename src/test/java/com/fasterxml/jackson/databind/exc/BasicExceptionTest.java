package com.fasterxml.jackson.databind.exc;

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.type.TypeFactory;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class BasicExceptionTest
{
    static class User {
        public String user;
    }

    static class Users {
        public ArrayList<User> userList;
    }

    private final ObjectMapper MAPPER = newJsonMapper();
    private final JsonFactory JSON_F = MAPPER.getFactory();

    @Test
    public void testBadDefinition() throws Exception
    {
        JavaType t = TypeFactory.defaultInstance().constructType(String.class);
        JsonParser p = JSON_F.createParser("[]");
        InvalidDefinitionException e = new InvalidDefinitionException(p,
               "Testing", t);
        assertEquals("Testing", e.getOriginalMessage());
        assertEquals(String.class, e.getType().getRawClass());
        assertNull(e.getBeanDescription());
        assertNull(e.getProperty());
        assertSame(p, e.getProcessor());
        p.close();

        // and via factory method:
        BeanDescription beanDef = MAPPER.getSerializationConfig().introspectClassAnnotations(getClass());
        e = InvalidDefinitionException.from(p, "Testing",
                beanDef, (BeanPropertyDefinition) null);
        assertEquals(beanDef.getType(), e.getType());
        assertNotNull(e);

        // and the other constructor too
        JsonGenerator g = JSON_F.createGenerator(new StringWriter());
        e = new InvalidDefinitionException(p,
                "Testing", t);
        assertEquals("Testing", e.getOriginalMessage());
        assertEquals(String.class, e.getType().getRawClass());

        // and factory
        e = InvalidDefinitionException.from(g, "Testing",
                beanDef, (BeanPropertyDefinition) null);
        assertEquals(beanDef.getType(), e.getType());
        assertNotNull(e);

        g.close();
    }

    @SuppressWarnings("deprecation")
    @Test
    public void testInvalidFormat() throws Exception
    {
        // deprecated methods should still work:
        InvalidFormatException e = new InvalidFormatException("Testing", Boolean.TRUE,
                String.class);
        assertSame(Boolean.TRUE, e.getValue());
        assertNull(e.getProcessor());
        assertNotNull(e);

        e = new InvalidFormatException("Testing", JsonLocation.NA,
                Boolean.TRUE, String.class);
        assertSame(Boolean.TRUE, e.getValue());
        assertNull(e.getProcessor());
        assertNotNull(e);
    }

    @Test
    public void testIgnoredProperty() throws Exception
    {
        // first just construct valid instance with some variations
        JsonParser p = JSON_F.createParser("{ }");
        IgnoredPropertyException e = IgnoredPropertyException.from(p,
                this, // to get class from
                "testProp", Collections.<Object>singletonList("x"));
        assertNotNull(e);

        e = IgnoredPropertyException.from(p,
                getClass(),
                "testProp", null);
        assertNotNull(e);
        assertNull(e.getKnownPropertyIds());
        p.close();

        // also, verify failure if null passed for "value"
        try {
            IgnoredPropertyException.from(p, null,
                    "testProp", Collections.<Object>singletonList("x"));
            fail("Should not pass");
        } catch (NullPointerException e2) {
        }
    }

    @Test
    public void testUnrecognizedProperty() throws Exception
    {
        JsonParser p = JSON_F.createParser("{ }");
        UnrecognizedPropertyException e = UnrecognizedPropertyException.from(p, this,
                "testProp", Collections.<Object>singletonList("y"));
        assertNotNull(e);
        assertEquals(getClass(), e.getReferringClass());
        Collection<Object> ids = e.getKnownPropertyIds();
        assertNotNull(ids);
        assertEquals(1, ids.size());
        assertTrue(ids.contains("y"));

        e = UnrecognizedPropertyException.from(p, getClass(),
                "testProp", Collections.<Object>singletonList("y"));

        assertEquals(getClass(), e.getReferringClass());
        p.close();
    }

    // [databind#2128]: ensure Location added once and only once
    // [databind#2482]: ensure Location is the original one
    // [core#1173]: ... and needs to be correct column, too
    @Test
    public void testLocationAddition() throws Exception
    {
        String problemJson = "{\n\t\"userList\" : [\n\t{\n\t user : \"1\"\n\t},\n\t{\n\t \"user\" : \"2\"\n\t}\n\t]\n}";
        try {
            MAPPER.readValue(problemJson, Users.class);
            fail("Should not pass");
        } catch (DatabindException e) { // becomes "generic" due to wrapping for passing path info
            String msg = e.getMessage();
            String[] str = msg.split(" at \\[");
            if (str.length != 2) {
                fail("Should only get one 'at [' marker, got "+(str.length-1)+", source: "+msg);
            }
            JsonLocation loc = e.getLocation();
//          String expectedLocation = "line: 4, column: 3";
            assertEquals(4, loc.getLineNr());
            // 12-Feb-2024, tatu: varies depending on whether [core#1173] is fixed or not...
            assertEquals(3, loc.getColumnNr());
        }
    }
}
