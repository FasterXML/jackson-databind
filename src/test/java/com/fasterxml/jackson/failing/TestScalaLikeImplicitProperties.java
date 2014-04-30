package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests Scala-style JVM naming patterns for properties.
 *
 * Scala uses identifiers that are legal JVM names, but not legal Java names:
 *
 * <ul>
 *     <li><code>prop␣</code> (trailing space) for fields</li>
 *     <li><code>prop</code> for getters</li>
 *     <li><code>prop_=</code> for setters</li>
 * </ul>
 *
 * Scala sources turn property accesses into method calls in most cases; the
 * backing field and the particulars of the method names are implementation details.
 *
 * Since I can't reproduce them in Java, I've substituted legal but uncommonly
 * used characters as placeholders.
 */
public class TestScalaLikeImplicitProperties extends BaseMapTest
{
    static class ValProperty
    {
        public final String prop‿;
        public String prop() { return prop‿; }

        public ValProperty(String prop) {
            prop‿ = prop;
        }
    }

    static class VarProperty
    {
        public String prop‿;
        public String prop() { return prop‿; }
        public void prop_⁀(String p) { prop‿ = p; }

        public VarProperty(String prop) {
            prop‿ = prop;
        }
    }

    static class GetterSetterProperty
    {
        // Different name to represent an arbitrary implementation, not necessarily local to this class.
        private String _prop_impl = "get/set";
        public String prop() { return _prop_impl; }
        public void prop_⁀(String p) { _prop_impl = p; }

        // Getter/Setters are typically not in the constructor because they are implemented
        // by the end user, not the compiler. They should be detected similar to 'bean-style'
        // getProp/setProp pairs.
    }


    public void testValProperty() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"val\"}", m.writeValueAsString(new ValProperty("val")));
    }


    public void testVarProperty() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"var\"}", m.writeValueAsString(new VarProperty("var")));
        VarProperty result = m.readValue("{\"prop\":\"read\"}", VarProperty.class);
        assertEquals("read", result.prop());
    }


    public void testGetterSetterProperty() throws Exception
    {
        ObjectMapper m = new ObjectMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"get/set\"}", m.writeValueAsString(new GetterSetterProperty()));
        GetterSetterProperty result = m.readValue("{\"prop\":\"read\"}", GetterSetterProperty.class);
        assertEquals("read", result.prop());
    }
}
