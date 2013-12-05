package com.fasterxml.jackson.failing;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

public class TestConvertingSerializer
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    // [Issue#357]
    static class A { }

    static class B {
        @JsonSerialize(contentConverter = AToStringConverter.class)
        public List<A> list = Arrays.asList(new A());
    }

    static class AToStringConverter extends StdConverter<A, List<String>> {
        @Override
        public List<String> convert(A value) {
            return Arrays.asList("Hello world!");
        }
    }

    // [Issue#359]

    static class Bean359 {
        @JsonSerialize(as = List.class, contentAs = Source.class)
        public List<Source> stuff = Arrays.asList(new Source());
    }

    @JsonSerialize(using = TargetSerializer.class)
    static class Target {
        public String unexpected = "Bye.";
    }

    @JsonSerialize(converter = SourceToTargetConverter.class)
    static class Source { }

    static class SourceToTargetConverter extends StdConverter<Source, Target> {
        @Override
        public Target convert(Source value) {
            return new Target();
        }
    }

    static class TargetSerializer extends JsonSerializer<TargetSerializer>
    {
        @Override
        public void serialize(TargetSerializer a, JsonGenerator jsonGenerator, SerializerProvider provider)
                throws IOException {
            jsonGenerator.writeString("Target");
        }
    }
    
    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [Issue#357]
    public void testConverterForList357() throws Exception {
        String json = objectWriter().writeValueAsString(new B());
        assertEquals("{\"list\":[[\"Hello world!\"]]}", json);
    }

    // [Issue#359]
    public void testIssue359() throws Exception
    {
        String json = objectWriter().writeValueAsString(new Bean359());
        assertNotNull(json);
        assertEquals("{\"stuff\":[\"Target\"]}", json);
    }

}
