package com.fasterxml.jackson.databind.deser.creators;

import java.lang.annotation.*;

import com.fasterxml.jackson.annotation.JsonCreator;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.cfg.*;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.introspect.AnnotatedMember;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.databind.json.JsonMapper;

// Tests for [databind#1498] (Jackson 2.12)
public class ConstructorDetector1498Test extends BaseMapTest
{
    // Helper annotation to work around lack of implicit name access with Jackson 2.x
    @Target(ElementType.PARAMETER)
    @Retention(RetentionPolicy.RUNTIME)
    @interface ImplicitName {
        String value();
    }

    // And annotation introspector to make use of it
    @SuppressWarnings("serial")
    static class CtorNameIntrospector extends JacksonAnnotationIntrospector
    {
        @Override
        public String findImplicitPropertyName(//MapperConfig<?> config,
                AnnotatedMember member) {
            final ImplicitName ann = member.getAnnotation(ImplicitName.class);
            return (ann == null) ? null : ann.value();
        }
    }

    static class SingleArgNotAnnotated {
        protected int v;

        SingleArgNotAnnotated() { v = -1; }
        
        public SingleArgNotAnnotated(@ImplicitName("value") int value) {
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

    private final ObjectMapper MAPPER_PROPS = mapperFor(ConstructorDetector.USE_PROPERTIES_BASED);
    private final ObjectMapper MAPPER_DELEGATING = mapperFor(ConstructorDetector.USE_DELEGATING);
    private final ObjectMapper MAPPER_EXPLICIT = mapperFor(ConstructorDetector.EXPLICIT_ONLY);

    /*
    /**********************************************************************
    /* Test methods, selecting from 1-arg constructors, properties-based
    /**********************************************************************
     */

    public void test1ArgDefaultsToPropertiesNonAnnotated() throws Exception
    {
        SingleArgNotAnnotated value = MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
                SingleArgNotAnnotated.class);
        assertEquals(137, value.v);
    }

    public void test1ArgDefaultsToPropertiesNoMode() throws Exception
    {
        // and similarly for mode-less
        SingleArgNoMode value = MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
                SingleArgNoMode.class);
        assertEquals(137, value.v);
    }

    // And specific test for original [databind#1498]
    public void test1ArgDefaultsToPropertiesIssue1498() throws Exception
    {
        // and similarly for mode-less
        SingleArg1498 value = MAPPER_PROPS.readValue(a2q("{'bar' : 404 }"),
                SingleArg1498.class);
        assertEquals(404, value._bar);
    }

    // 18-Sep-2020, tatu: For now there is a problematic case of multiple eligible
    //   choices; not cleanly solvable for 2.12
    public void test1ArgDefaultsToPropsMultipleCtors() throws Exception
    {
        try {
            MAPPER_PROPS.readValue(a2q("{'value' : 137 }"),
                SingleArg2CtorsNotAnnotated.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Conflicting property-based creators");
        }
    }

    /*
    /**********************************************************************
    /* Test methods, selecting from 1-arg constructors, delegating
    /**********************************************************************
     */

    public void test1ArgDefaultsToDelegatingNoAnnotation() throws Exception
    {
        // No annotation, should be fine?
        SingleArgNotAnnotated value = MAPPER_DELEGATING.readValue("1972", SingleArgNotAnnotated.class);
        assertEquals(1972, value.v);
    }

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
    /* Test methods, selecting from 1-arg constructors, explicit fail
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

    /*
    /**********************************************************************
    /* Helper methods
    /**********************************************************************
     */

    private JsonMapper.Builder mapperBuilder() {
        return JsonMapper.builder()
                .annotationIntrospector(new CtorNameIntrospector());
    }

    private ObjectMapper mapperFor(ConstructorDetector cd) {
        return mapperBuilder()
                .constructorDetector(cd)
                .build();
    }
}
