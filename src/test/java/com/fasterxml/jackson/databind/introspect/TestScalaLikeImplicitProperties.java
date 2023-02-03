package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.MapperConfig;

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
                if (name.endsWith("‿")) {
                    return name.substring(0, name.length()-1);
                }
            } else if (member instanceof AnnotatedMethod) {
                name = member.getName();
                if (name.endsWith("_⁀")) {
                    return name.substring(0, name.length()-2);
                }
                if (!name.startsWith("get") && !name.startsWith("set")) {
                    return name;
                }
            } else if (member instanceof AnnotatedParameter) {
                // A placeholder for legitimate property name detection
                // such as what the JDK8 module provides
                return "prop";
            }
            return null;
        }

        /* Deprecated since 2.9
        @Override
        public boolean hasCreatorAnnotation(Annotated a) {
            return (a instanceof AnnotatedConstructor);
        }
        */

        @Override
        public JsonCreator.Mode findCreatorAnnotation(MapperConfig<?> config, Annotated a) {
            // A placeholder for legitimate creator detection.
            // In Scala, all primary constructors should be creators,
            // but I can't obtain a reference to the AnnotatedClass from the
            // AnnotatedConstructor, so it's simulated here.
            return (a instanceof AnnotatedConstructor)
                    ? JsonCreator.Mode.DEFAULT : null;
        }
    }

    static class ValProperty
    {
        private final String prop‿;
        public String prop() { return prop‿; }

        public ValProperty(String prop) {
            prop‿ = prop;
        }
    }

    static class ValWithBeanProperty
    {
        private final String prop‿;
        public String prop() { return prop‿; }
        public String getProp() { return prop‿; }

        public ValWithBeanProperty(String prop) {
            prop‿ = prop;
        }
    }

    static class VarProperty
    {
        private String prop‿;
        public String prop() { return prop‿; }
        public void prop_⁀(String p) { prop‿ = p; }

        public VarProperty(String prop) {
            prop‿ = prop;
        }
    }

    static class VarWithBeanProperty
    {
        private String prop‿;
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

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testValProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        assertEquals("{\"prop\":\"val\"}", m.writeValueAsString(new ValProperty("val")));
    }

    public void testValWithBeanProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        assertEquals("{\"prop\":\"val\"}", m.writeValueAsString(new ValWithBeanProperty("val")));
    }


    public void testVarProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        assertEquals("{\"prop\":\"var\"}", m.writeValueAsString(new VarProperty("var")));
        VarProperty result = m.readValue("{\"prop\":\"read\"}", VarProperty.class);
        assertEquals("read", result.prop());
    }


    public void testVarWithBeanProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        assertEquals("{\"prop\":\"var\"}", m.writeValueAsString(new VarWithBeanProperty("var")));
        VarWithBeanProperty result = m.readValue("{\"prop\":\"read\"}", VarWithBeanProperty.class);
        assertEquals("read", result.prop());
    }


    public void testGetterSetterProperty() throws Exception
    {
        ObjectMapper m = manglingMapper();

        assertEquals("{\"prop\":\"get/set\"}", m.writeValueAsString(new GetterSetterProperty()));
        GetterSetterProperty result = m.readValue("{\"prop\":\"read\"}", GetterSetterProperty.class);
        assertEquals("read", result.prop());
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    private ObjectMapper manglingMapper()
    {
        ObjectMapper m = new ObjectMapper();
        m.setAnnotationIntrospector(new NameMangler());
        return m;
    }
}
