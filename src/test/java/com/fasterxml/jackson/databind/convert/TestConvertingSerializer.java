package com.fasterxml.jackson.databind.convert;

import java.util.*;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;

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
}
