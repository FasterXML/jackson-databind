package com.fasterxml.jackson.databind.deser.creators;

import java.util.Arrays;
import java.util.List;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;

public class SingleArgCreatorTest extends BaseMapTest
{
    // [databind#430]: single arg BUT named; should not delegate

    static class SingleNamedStringBean {
        final String _ss;

        @JsonCreator
        public SingleNamedStringBean(@JsonProperty("") String ss){
            this._ss = ss;
        }

        public String getSs() { return _ss; }
    }

    // For [databind#614]
    static class SingleNamedButStillDelegating {
        protected final String value;

        @JsonCreator(mode=JsonCreator.Mode.DELEGATING)
        public SingleNamedButStillDelegating(@JsonProperty("foobar") String v) {
            value = v;
        }

        public String getFoobar() { return "x"; }
    }

    // For [databind#557]
    static class StringyBean
    {
        public final String value;

        private StringyBean(String value) { this.value = value; }

        public String getValue() {
            return value;
        }
    }

    static class StringyBeanWithProps
    {
        public final String value;

        @JsonCreator
        private StringyBeanWithProps(String v) { value = v; }

        public String getValue() {
            return value;
        }
    }

    @SuppressWarnings("serial")
    static class MyParamIntrospector extends JacksonAnnotationIntrospector
    {
        private final String name;

        public MyParamIntrospector(String n) { name = n; }

        @Override
        public String findImplicitPropertyName(AnnotatedMember param) {
            if (param instanceof AnnotatedParameter) {
                AnnotatedParameter ap = (AnnotatedParameter) param;
                switch (ap.getIndex()) {
                case 0: return name;
                }
                return "param"+ap.getIndex();
            }
            return super.findImplicitPropertyName(param);
        }
    }

    // [databind#660]
    static class ExplicitFactoryBeanA {
        private String value;

        private ExplicitFactoryBeanA(String str) {
            throw new IllegalStateException("Should not get called!");
        }

        private ExplicitFactoryBeanA(String str, boolean b) {
            value = str;
        }

        @JsonCreator
        public static ExplicitFactoryBeanA create(String str) {
            ExplicitFactoryBeanA bean = new ExplicitFactoryBeanA(str, false);
            bean.value = str;
            return bean;
        }

        public String value() { return value; }
    }

    // [databind#660]
    static class ExplicitFactoryBeanB {
        private String value;

        @JsonCreator
        private ExplicitFactoryBeanB(String str) {
            value = str;
        }

        public static ExplicitFactoryBeanB valueOf(String str) {
            return new ExplicitFactoryBeanB(null);
        }

        public String value() { return value; }
    }

    static class XY {
        public int x, y;
    }

    // [databind#1383]
    static class SingleArgWithImplicit {
        protected XY _value;

        private SingleArgWithImplicit() {
            throw new Error("Should not get called");
        }
        private SingleArgWithImplicit(XY v, boolean bogus) {
            _value = v;
        }

        @JsonCreator
        public static SingleArgWithImplicit from(XY v) {
            return new SingleArgWithImplicit(v, true);
        }

        public XY getFoobar() { return _value; }
    }

    // [databind#3062]
    static class DecVector3062 {
        List<Double> elems;

        public DecVector3062() { super(); }
        public DecVector3062(List<Double> e) { elems = e; }
        public DecVector3062(double elem) { elems = Arrays.asList(elem); }
        public DecVector3062(Double elem) { elems = Arrays.asList(elem); }

        public List<Double> getElems() { return elems; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    public void testNamedSingleArg() throws Exception
    {
        SingleNamedStringBean bean = MAPPER.readValue(q("foobar"),
                SingleNamedStringBean.class);
        assertEquals("foobar", bean._ss);
    }

    public void testSingleStringArgWithImplicitName() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector("value"));
        StringyBean bean = mapper.readValue(q("foobar"), StringyBean.class);
        assertEquals("foobar", bean.getValue());
    }

    // [databind#714]
    public void testSingleImplicitlyNamedNotDelegating() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector("value"));
        StringyBeanWithProps bean = mapper.readValue("{\"value\":\"x\"}", StringyBeanWithProps.class);
        assertEquals("x", bean.getValue());
    }

    // [databind#714]
    public void testSingleExplicitlyNamedButDelegating() throws Exception
    {
        SingleNamedButStillDelegating bean = MAPPER.readValue(q("xyz"),
                SingleNamedButStillDelegating.class);
        assertEquals("xyz", bean.value);
    }

    public void testExplicitFactory660a() throws Exception
    {
        // First, explicit override for factory
        ExplicitFactoryBeanA bean = MAPPER.readValue(q("abc"), ExplicitFactoryBeanA.class);
        assertNotNull(bean);
        assertEquals("abc", bean.value());
    }

    public void testExplicitFactory660b() throws Exception
    {
        // and then one for private constructor
        ExplicitFactoryBeanB bean2 = MAPPER.readValue(q("def"), ExplicitFactoryBeanB.class);
        assertNotNull(bean2);
        assertEquals("def", bean2.value());
    }

    // [databind#1383]
    public void testSingleImplicitDelegating() throws Exception
    {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.setAnnotationIntrospector(new MyParamIntrospector("value"));
        SingleArgWithImplicit bean = mapper.readValue(a2q("{'x':1,'y':2}"),
                SingleArgWithImplicit.class);
        XY v = bean.getFoobar();
        assertNotNull(v);
        assertEquals(1, v.x);
        assertEquals(2, v.y);
    }

    // [databind#3062]
    public void testMultipleDoubleCreators3062() throws Exception
    {
        DecVector3062 vector = new DecVector3062(Arrays.asList(1.0, 2.0, 3.0));
        String result = MAPPER.writeValueAsString(vector);
        DecVector3062 deser = MAPPER.readValue(result, DecVector3062.class);
        assertEquals(vector.elems, deser.elems);
    }
}
