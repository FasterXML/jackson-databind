package com.fasterxml.jackson.databind.ser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonAppend;
import com.fasterxml.jackson.databind.cfg.MapperConfig;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.BeanPropertyDefinition;
import com.fasterxml.jackson.databind.util.Annotations;

/**
 * Tests for verifying that one can append virtual properties after regular ones.
 */
public class TestVirtualProperties extends BaseMapTest
{
    @JsonAppend(attrs={ @JsonAppend.Attr("id"),
        @JsonAppend.Attr(value="internal", propName="extra", required=true)
    })
    static class SimpleBean
    {
        public int value = 13;
    }

    @JsonAppend(prepend=true, attrs={ @JsonAppend.Attr("id"),
            @JsonAppend.Attr(value="internal", propName="extra")
        })
    static class SimpleBeanPrepend
    {
        public int value = 13;
    }

    enum ABC {
        A, B, C;
    }

    @JsonAppend(attrs=@JsonAppend.Attr(value="desc", include=JsonInclude.Include.NON_EMPTY))
    static class OptionalsBean
    {
        public int value = 28;
    }

    @SuppressWarnings("serial")
    static class CustomVProperty
        extends VirtualBeanPropertyWriter
    {
        private CustomVProperty() { super(); }

        private CustomVProperty(BeanPropertyDefinition propDef,
                Annotations ctxtAnn, JavaType type) {
            super(propDef, ctxtAnn, type);
        }

        @Override
        protected Object value(Object bean, JsonGenerator jgen, SerializerProvider prov) {
            if (_name.toString().equals("id")) {
                return "abc123";
            }
            if (_name.toString().equals("extra")) {
                return new int[] { 42 };
            }
            return "???";
        }

        @Override
        public VirtualBeanPropertyWriter withConfig(MapperConfig<?> config,
                AnnotatedClass declaringClass, BeanPropertyDefinition propDef,
                JavaType type)
        {
            return new CustomVProperty(propDef, declaringClass.getAnnotations(), type);
        }
    }

    @JsonAppend(prepend=true, props={ @JsonAppend.Prop(value=CustomVProperty.class, name="id"),
            @JsonAppend.Prop(value=CustomVProperty.class, name="extra")
        })
    static class CustomVBean
    {
        public int value = 72;
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectWriter WRITER = objectWriter();

    public void testAttributeProperties() throws Exception
    {
        Map<String,Object> stuff = new LinkedHashMap<String,Object>();
        stuff.put("x", 3);
        stuff.put("y", ABC.B);

        String json = WRITER.withAttribute("id", "abc123")
                .withAttribute("internal", stuff)
                .writeValueAsString(new SimpleBean());
        assertEquals(a2q("{'value':13,'id':'abc123','extra':{'x':3,'y':'B'}}"), json);

        json = WRITER.withAttribute("id", "abc123")
                .withAttribute("internal", stuff)
                .writeValueAsString(new SimpleBeanPrepend());
        assertEquals(a2q("{'id':'abc123','extra':{'x':3,'y':'B'},'value':13}"), json);
    }

    public void testAttributePropInclusion() throws Exception
    {
        // first, with desc
        String json = WRITER.withAttribute("desc", "nice")
                .writeValueAsString(new OptionalsBean());
        assertEquals(a2q("{'value':28,'desc':'nice'}"), json);

        // then with null (not defined)
        json = WRITER.writeValueAsString(new OptionalsBean());
        assertEquals(a2q("{'value':28}"), json);

        // and finally "empty"
        json = WRITER.withAttribute("desc", "")
                .writeValueAsString(new OptionalsBean());
        assertEquals(a2q("{'value':28}"), json);
    }

    public void testCustomProperties() throws Exception
    {
        String json = WRITER.withAttribute("desc", "nice")
                .writeValueAsString(new CustomVBean());
        assertEquals(a2q("{'id':'abc123','extra':[42],'value':72}"), json);
    }
}
