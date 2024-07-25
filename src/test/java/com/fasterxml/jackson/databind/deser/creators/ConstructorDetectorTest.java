package com.fasterxml.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.exc.InvalidNullException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

// Tests for [databind#1498], [databind#3241] (Jackson 2.12)
public class ConstructorDetectorTest extends DatabindTestUtil
{
    static class SingleArgNotAnnotated {
        protected int v;

        SingleArgNotAnnotated() { v = -1; }

        public SingleArgNotAnnotated(@ImplicitName("value") int value) {
            v = value;
        }
    }

    static class SingleArgByte {
        protected byte v;

        SingleArgByte() { v = -1; }

        public SingleArgByte(@ImplicitName("value") byte value) {
            v = value;
        }
    }

    static class SingleArgShort {
        protected short v;

        SingleArgShort() { v = -1; }

        public SingleArgShort(@ImplicitName("value") short value) {
            v = value;
        }
    }

    static class SingleArgLong {
        protected long v;

        SingleArgLong() { v = -1; }

        public SingleArgLong(@ImplicitName("value") long value) {
            v = value;
        }
    }

    static class SingleArgFloat {
        protected float v;

        SingleArgFloat() { v = -1.0f; }

        public SingleArgFloat(@ImplicitName("value") float value) {
            v = value;
        }
    }

    static class SingleArgDouble {
        protected double v;

        SingleArgDouble() { v = -1.0; }

        public SingleArgDouble(@ImplicitName("value") double value) {
            v = value;
        }
    }

    static class SingleArgNoMode {
        protected int v;

        SingleArgNoMode() { v = -1; }

        @JsonCreator
        public SingleArgNoMode(@ImplicitName("value") int value) {
            v = value;
        }
    }

    static class SingleArg2CtorsNotAnnotated {
        protected int v;

        SingleArg2CtorsNotAnnotated() { v = -1; }

        public SingleArg2CtorsNotAnnotated(@ImplicitName("value") int value) {
            v = value;
        }

        public SingleArg2CtorsNotAnnotated(@ImplicitName("value") long value) {
            v = (int) (value * 2);
        }
    }

    static class SingleArg1498 {
        final int _bar;

        // note: annotation only to inject "implicit name" without needing parameter-names module
        SingleArg1498(@ImplicitName("bar") int b) {
            _bar = b;
        }
    }

    static class TwoArgsNotAnnotated {
        protected int _a, _b;

        public TwoArgsNotAnnotated(@ImplicitName("a") int a, @ImplicitName("b") int b) {
            _a = a;
            _b = b;
        }
    }

    // [databind#3241]
    static class Input3241 {
        private final Boolean field;

        // @JsonCreator gone!
        public Input3241(@ImplicitName("field") Boolean field) {
            if (field == null) {
                throw new NullPointerException("Field should not remain null!");
            }
            this.field = field;
        }

        public Boolean field() {
            return field;
        }
    }
    
    private final ObjectMapper MAPPER_PROPS = mapperFor(ConstructorDetector.USE_PROPERTIES_BASED);
    private final ObjectMapper MAPPER_DELEGATING = mapperFor(ConstructorDetector.USE_DELEGATING);
    private final ObjectMapper MAPPER_EXPLICIT = mapperFor(ConstructorDetector.EXPLICIT_ONLY);

    private final ObjectMapper MAPPER_MUST_ANNOTATE = mapperFor(ConstructorDetector.DEFAULT
            .withRequireAnnotation(true));

    /*
    /**********************************************************************
    /* Test methods, selecting from 1-arg constructors, properties-based
    /**********************************************************************
     */

    @Test
    public void test1ArgDefaultsToPropertiesNonAnnotated() throws Exception
    {
        SingleArgNotAnnotated value = MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
                SingleArgNotAnnotated.class);
        assertEquals(137, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesNonAnnotatedDecimal() throws Exception
    {
        SingleArgNotAnnotated value = MAPPER_PROPS.readValue(a2q("{'value' : 137.0 }"),
            SingleArgNotAnnotated.class);
        assertEquals(137, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesByte() throws Exception
    {
        SingleArgByte value = MAPPER_PROPS.readValue(a2q("{'value' : -99 }"),
            SingleArgByte.class);
        assertEquals(-99, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesShort() throws Exception
    {
        SingleArgShort value = MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
            SingleArgShort.class);
        assertEquals(137, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesLong() throws Exception
    {
        String val = Long.toString(Long.MAX_VALUE);
        SingleArgLong value = MAPPER_PROPS.readValue(a2q("{'value' : " + val + " }"),
            SingleArgLong.class);
        assertEquals(Long.MAX_VALUE, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesFloat() throws Exception
    {
        SingleArgFloat value = MAPPER_PROPS.readValue(a2q("{'value' : 136.99 }"),
            SingleArgFloat.class);
        assertEquals(136.99f, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesDouble() throws Exception
    {
        SingleArgDouble value = MAPPER_PROPS.readValue(a2q("{'value' : 999999999999999999.99 }"),
            SingleArgDouble.class);
        assertEquals(999999999999999999.99, value.v);
    }

    @Test
    public void test1ArgDefaultsToPropertiesNoMode() throws Exception
    {
        SingleArgNoMode value = MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
                SingleArgNoMode.class);
        assertEquals(137, value.v);
    }

    // And specific test for original [databind#1498]
    @Test
    public void test1ArgDefaultsToPropertiesIssue1498() throws Exception
    {
        SingleArg1498 value = MAPPER_PROPS.readValue(a2q("{'bar' : 404 }"),
                SingleArg1498.class);
        assertEquals(404, value._bar);
    }

    // This was working already but verify
    @Test
    public void testMultiArgAsProperties() throws Exception
    {
        TwoArgsNotAnnotated value = MAPPER_PROPS.readValue(a2q("{'a' : 3, 'b':4 }"),
                TwoArgsNotAnnotated.class);
        assertEquals(3, value._a);
        assertEquals(4, value._b);
    }

    // 18-Sep-2020, tatu: For now there is a problematic case of multiple eligible
    //   choices; not cleanly solvable for 2.12
    @Test
    public void test1ArgDefaultsToPropsMultipleCtors() throws Exception
    {
        // 23-May-2024, tatu: Will fail differently with [databind#4515]; default
        //   constructor available, implicit ones ignored
        try {
            MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
                SingleArg2CtorsNotAnnotated.class);
            fail("Should not pass");
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "\"value\"");
        }
            /*
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Conflicting property-based creators");
        }
        */
    }

    /*
    /**********************************************************************
    /* Test methods, selecting from 1-arg constructors, delegating
    /**********************************************************************
     */

    @Test
    public void test1ArgDefaultsToDelegatingNoAnnotation() throws Exception
    {
        // No annotation, should be fine?
        SingleArgNotAnnotated value = MAPPER_DELEGATING.readValue("1972", SingleArgNotAnnotated.class);
        assertEquals(1972, value.v);
    }

    @Test
    public void test1ArgDefaultsToDelegatingNoMode() throws Exception
    {
        // One with `@JsonCreator` no mode annotation (ok since indicated)
        SingleArgNoMode value = MAPPER_DELEGATING.readValue(" 2812 ", SingleArgNoMode.class);
        assertEquals(2812, value.v);
    }

    /*
    /**********************************************************************
    /* Test methods, selecting from 1-arg constructors, heuristic (pre-2.12)
    /**********************************************************************
     */

    @Test
    public void test1ArgDefaultsToHeuristics() throws Exception
    {
        final ObjectMapper mapper = mapperFor(ConstructorDetector.DEFAULT);
        final String DOC = " 13117 ";

        // First: unannotated is ok, defaults to delegating
        SingleArgNotAnnotated v1 = mapper.readValue(DOC, SingleArgNotAnnotated.class);
        assertEquals(13117, v1.v);

        // and ditto for mode-less
        SingleArgNoMode v2 = mapper.readValue(DOC, SingleArgNoMode.class);
        assertEquals(13117, v2.v);
    }

    /*
    /**********************************************************************
    /* Test methods, selecting from 1-arg constructors, explicit fails
    /**********************************************************************
     */

    // 15-Sep-2020, tatu: Tricky semantics... should this require turning
    //    off of auto-detection? If there is 0-arg ctor, that is to be used
/*
    public void test1ArgFailsNoAnnotation() throws Exception
    {
        // First: fail if nothing annotated (for 1-arg case)
        try {
            MAPPER_EXPLICIT.readValue(" 2812 ", SingleArgNotAnnotated.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "foobar");
        }
    }
    */

    @Test
    public void test1ArgFailsNoMode() throws Exception
    {
        // Second: also fail also if no "mode" indicated
        try {
            MAPPER_EXPLICIT.readValue(" 2812 ", SingleArgNoMode.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no 'mode' defined");
            verifyException(e, "SingleArgConstructor.REQUIRE_MODE");
        }
    }

    @Test
    public void test1ArgRequiresAnnotation() throws Exception
    {
        // First: if there is a 0-arg ctor, fine, must use that
        SingleArgNotAnnotated value = MAPPER_MUST_ANNOTATE.readValue("{ }",
                SingleArgNotAnnotated.class);
        assertEquals(new SingleArgNotAnnotated().v, value.v);

        // But if no such ctor, will fail
        try {
            MAPPER_MUST_ANNOTATE.readValue(" { } ", SingleArg1498.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no Creators, like default constructor");
        }
    }

    @Test
    public void testMultiArgRequiresAnnotation() throws Exception
    {
        try {
            MAPPER_MUST_ANNOTATE.readValue(" { } ", TwoArgsNotAnnotated.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "no Creators, like default constructor");
        }
    }

    // [databind#3241]
    @Test
    void nullHandlingCreator3241() throws Exception {
        ObjectMapper mapper = mapperBuilder()
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED) // new!
                .defaultSetterInfo(JsonSetter.Value.construct(Nulls.FAIL, Nulls.FAIL))
                .build();

        try {
            mapper.readValue("{ \"field\": null }", Input3241.class);
            fail("InvalidNullException expected");
        } catch (InvalidNullException e) {
            verifyException(e, "Invalid `null` value encountered");
        }
    }

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private JsonMapper.Builder mapperBuilder() {
        return JsonMapper.builder()
                .annotationIntrospector(new ImplicitNameIntrospector());
    }

    private ObjectMapper mapperFor(ConstructorDetector cd) {
        return mapperBuilder()
                .constructorDetector(cd)
                .build();
    }
}
