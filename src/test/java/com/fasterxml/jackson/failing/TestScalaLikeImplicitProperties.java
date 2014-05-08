package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.*;

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
    static class NameMangler extends JacksonAnnotationIntrospector
    {
        private static final long serialVersionUID = 1L;

        @Override
        public String findImplicitPropertyName(AnnotatedMember member) {
            String name = null;
            if (member instanceof AnnotatedField) {
                name = member.getName();
            }
            if (name != null) {
                if (name.endsWith("‿")) {                    
                    return name.substring(0, name.length()-1);
                }
            }
            return null;
        }
    }
    
    static class ValProperty
    {
        public final String prop‿;
        public String prop() { return prop‿; }

        public ValProperty(String prop) {
            prop‿ = prop;
        }
    }


    static class ValWithBeanProperty
    {
        public final String prop‿;
        public String prop() { return prop‿; }
        public String getProp() { return prop‿; }

        public ValWithBeanProperty(String prop) {
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


    static class VarWithBeanProperty
    {
        public String prop‿;
        public String prop() { return prop‿; }
        public void prop_⁀(String p) { prop‿ = p; }
        public String getProp() { return prop‿; }
        public void setProp(String p) { prop‿ = p; }

        public VarWithBeanProperty(String prop) {
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
        ObjectMapper m = manglingMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"val\"}", m.writeValueAsString(new ValProperty("val")));
    }


    public void testValWithBeanProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"val\"}", m.writeValueAsString(new ValWithBeanProperty("val")));
    }


    public void testVarProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"var\"}", m.writeValueAsString(new VarProperty("var")));
        VarProperty result = m.readValue("{\"prop\":\"read\"}", VarProperty.class);
        assertEquals("read", result.prop());
    }


    public void testVarWithBeanProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"var\"}", m.writeValueAsString(new VarWithBeanProperty("var")));
        VarWithBeanProperty result = m.readValue("{\"prop\":\"read\"}", VarWithBeanProperty.class);
        assertEquals("read", result.prop());
    }


    public void testGetterSetterProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        // TODO: Activate whatever handler implements the property detection style

        assertEquals("{\"prop\":\"get/set\"}", m.writeValueAsString(new GetterSetterProperty()));
        GetterSetterProperty result = m.readValue("{\"prop\":\"read\"}", GetterSetterProperty.class);
        assertEquals("read", result.prop());
    }

    private ObjectMapper manglingMapper()
    {
        ObjectMapper m = new ObjectMapper();
        m.setAnnotationIntrospector(new NameMangler());
        return m;
    }
}
