package com.fasterxml.jackson.databind.convert;

import java.io.IOException;
import java.util.*;

import com.fasterxml.jackson.core.JsonGenerator;

import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

public class TestConvertingSerializer
    extends com.fasterxml.jackson.databind.BaseMapTest
{
    @JsonSerialize(converter=ConvertingBeanConverter.class)
    static class ConvertingBean
    {
        public int x, y;

        public ConvertingBean(int v1, int v2) {
            x = v1;
            y = v2;
        }
    }

    static class Point
    {
        public int x, y;

        public Point(int v1, int v2) {
            x = v1;
            y = v2;
        }
    }

    static class ConvertingBeanContainer
    {
        public List<ConvertingBean> values;

        public ConvertingBeanContainer(ConvertingBean... beans) {
            values = Arrays.asList(beans);
        }
    }

    static class ConvertingBeanConverter extends StdConverter<ConvertingBean, int[]>
    {
        @Override
        public int[] convert(ConvertingBean value) {
            return new int[] { value.x, value.y };
        }
    }

    static class PointConverter extends StdConverter<Point, int[]>
    {
        @Override public int[] convert(Point value) {
            return new int[] { value.x, value.y };
        }
    }

    static class PointWrapper {
        @JsonSerialize(converter=PointConverter.class)
        public Point value;

        public PointWrapper(int x, int y) {
            value = new Point(x, y);
        }
    }

    static class PointListWrapperArray {
        @JsonSerialize(contentConverter=PointConverter.class)
        public Point[] values;

        public PointListWrapperArray(int x, int y) {
            values = new Point[] { new Point(x, y), new Point(y, x) };
        }
    }

    static class PointListWrapperList {
        @JsonSerialize(contentConverter=PointConverter.class)
        public List<Point> values;

        public PointListWrapperList(int x, int y) {
            values = Arrays.asList(new Point[] { new Point(x, y), new Point(y, x) });
        }
    }

    static class PointListWrapperMap {
        @JsonSerialize(contentConverter=PointConverter.class)
        public Map<String,Point> values;

        public PointListWrapperMap(String key, int x, int y) {
            values = new HashMap<String,Point>();
            values.put(key, new Point(x, y));
        }
    }

    // [databind#357]
    static class Value { }

    static class ListWrapper {
        @JsonSerialize(contentConverter = ValueToStringListConverter.class)
        public List<Value> list = Arrays.asList(new Value());
    }

    static class ValueToStringListConverter extends StdConverter<Value, List<String>> {
        @Override
        public List<String> convert(Value value) {
            return Arrays.asList("Hello world!");
        }
    }

    // [databind#359]
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

    static class TargetSerializer extends JsonSerializer<Target>
    {
        @Override
        public void serialize(Target a, JsonGenerator jsonGenerator, SerializerProvider provider) throws IOException {
            jsonGenerator.writeString("Target");
        }
    }

    // [databind#731]
    @JsonPropertyOrder({ "a", "b" })
    public static class DummyBean {
        public final int a, b;
        public DummyBean(int v1, int v2) {
            a = v1 * 2;
            b = v2 * 2;
        }
    }

    @JsonSerialize(converter = UntypedConvertingBeanConverter.class)
    static class ConvertingBeanWithUntypedConverter {
        public int x, y;
        public ConvertingBeanWithUntypedConverter(int v1, int v2) {
            x = v1;
            y = v2;
        }
    }

    static class UntypedConvertingBeanConverter extends StdConverter<ConvertingBeanWithUntypedConverter, Object>
    {
        @Override
        public Object convert(ConvertingBeanWithUntypedConverter cb) {
            return new DummyBean(cb.x, cb.y);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    public void testClassAnnotationSimple() throws Exception
    {
        String json = objectWriter().writeValueAsString(new ConvertingBean(1, 2));
        assertEquals("[1,2]", json);
    }

    public void testClassAnnotationForLists() throws Exception
    {
        String json = objectWriter().writeValueAsString(new ConvertingBeanContainer(
                new ConvertingBean(1, 2), new ConvertingBean(3, 4)));
        assertEquals("{\"values\":[[1,2],[3,4]]}", json);
    }

    public void testPropertyAnnotationSimple() throws Exception
    {
        String json = objectWriter().writeValueAsString(new PointWrapper(3, 4));
        assertEquals("{\"value\":[3,4]}", json);
    }

    public void testPropertyAnnotationForArrays() throws Exception {
        String json = objectWriter().writeValueAsString(new PointListWrapperArray(4, 5));
        assertEquals("{\"values\":[[4,5],[5,4]]}", json);
    }

    public void testPropertyAnnotationForLists() throws Exception {
        String json = objectWriter().writeValueAsString(new PointListWrapperList(7, 8));
        assertEquals("{\"values\":[[7,8],[8,7]]}", json);
    }

    public void testPropertyAnnotationForMaps() throws Exception {
        String json = objectWriter().writeValueAsString(new PointListWrapperMap("a", 1, 2));
        assertEquals("{\"values\":{\"a\":[1,2]}}", json);
    }

    // [databind#357]
    public void testConverterForList357() throws Exception {
        String json = objectWriter().writeValueAsString(new ListWrapper());
        assertEquals("{\"list\":[[\"Hello world!\"]]}", json);
    }

    // [databind#359]
    public void testIssue359() throws Exception {
        String json = objectWriter().writeValueAsString(new Bean359());
        assertEquals("{\"stuff\":[\"Target\"]}", json);
    }

    // [databind#731]: Problems converting from java.lang.Object ("unknown")
    public void testIssue731() throws Exception
    {
        String json = objectWriter().writeValueAsString(new ConvertingBeanWithUntypedConverter(1, 2));
        // must be  {"a":2,"b":4}
        assertEquals("{\"a\":2,\"b\":4}", json);
    }
}
