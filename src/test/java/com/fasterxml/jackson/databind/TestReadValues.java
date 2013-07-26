package com.fasterxml.jackson.databind;

import java.util.*;

import com.fasterxml.jackson.core.*;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

@SuppressWarnings("resource")
public class TestReadValues extends BaseMapTest
{
    /*
    /**********************************************************
    /* Helper types
    /**********************************************************
     */

    static class Bean {
        public int a;
    }
    
    /*
    /**********************************************************
    /* Unit tests; root-level value sequences via Mapper
    /**********************************************************
     */

    private final ObjectMapper MAPPER = new ObjectMapper();

    public void testRootBeans() throws Exception
    {
        final String JSON = "{\"a\":3}{\"a\":27}  ";
        Iterator<Bean> it = MAPPER.reader(Bean.class).readValues(JSON);

        assertNotNull(((MappingIterator<?>) it).getCurrentLocation());
        assertTrue(it.hasNext());
        Bean b = it.next();
        assertEquals(3, b.a);
        assertTrue(it.hasNext());
        b = it.next();
        assertEquals(27, b.a);
        assertFalse(it.hasNext());
    }

    public void testRootMaps() throws Exception
    {
        final String JSON = "{\"a\":3}{\"a\":27}  ";
        Iterator<Map<?,?>> it = MAPPER.reader(Map.class).readValues(JSON);

        assertNotNull(((MappingIterator<?>) it).getCurrentLocation());
        assertTrue(it.hasNext());
        Map<?,?> map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertTrue(it.hasNext());
        assertNotNull(((MappingIterator<?>) it).getCurrentLocation());
        map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(27), map.get("a"));
        assertFalse(it.hasNext());
    }

    /*
    /**********************************************************
    /* Unit tests; root-level value sequences via JsonParser
    /**********************************************************
     */

    public void testRootBeansWithParser() throws Exception
    {
        final String JSON = "{\"a\":3}{\"a\":27}  ";
        JsonParser jp = MAPPER.getFactory().createParser(JSON);
        
        Iterator<Bean> it = jp.readValuesAs(Bean.class);

        assertTrue(it.hasNext());
        Bean b = it.next();
        assertEquals(3, b.a);
        assertTrue(it.hasNext());
        b = it.next();
        assertEquals(27, b.a);
        assertFalse(it.hasNext());
    }

    public void testRootArraysWithParser() throws Exception
    {
        final String JSON = "[1][3]";
        JsonParser jp = MAPPER.getFactory().createParser(JSON);

        // NOTE: We must point JsonParser to the first element; if we tried to
        // use "managed" accessor, it would try to advance past START_ARRAY.
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        
        Iterator<int[]> it = MAPPER.reader(int[].class).readValues(jp);
        assertTrue(it.hasNext());
        int[] array = it.next();
        assertEquals(1, array.length);
        assertEquals(1, array[0]);
        assertTrue(it.hasNext());
        array = it.next();
        assertEquals(1, array.length);
        assertEquals(3, array[0]);
        assertFalse(it.hasNext());
    }
    
    public void testHasNextWithEndArray() throws Exception {
        final String JSON = "[1,3]";
        JsonParser jp = MAPPER.getFactory().createParser(JSON);

        // NOTE: We must point JsonParser to the first element; if we tried to
        // use "managed" accessor, it would try to advance past START_ARRAY.
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        jp.nextToken();
        
        Iterator<Integer> it = MAPPER.reader(Integer.class).readValues(jp);
        assertTrue(it.hasNext());
        int value = it.next();
        assertEquals(1, value);
        assertTrue(it.hasNext());
        value = it.next();
        assertEquals(3, value);
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }
    
    public void testHasNextWithEndArrayManagedParser() throws Exception {
        final String JSON = "[1,3]";

        Iterator<Integer> it = MAPPER.reader(Integer.class).readValues(JSON);
        assertTrue(it.hasNext());
        int value = it.next();
        assertEquals(1, value);
        assertTrue(it.hasNext());
        value = it.next();
        assertEquals(3, value);
        assertFalse(it.hasNext());
        assertFalse(it.hasNext());
    }
    
    /*
    /**********************************************************
    /* Unit tests; non-root arrays
    /**********************************************************
     */

    public void testNonRootBeans() throws Exception
    {
        final String JSON = "{\"leaf\":[{\"a\":3},{\"a\":27}]}";
        JsonParser jp = MAPPER.getFactory().createParser(JSON);
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        assertToken(JsonToken.FIELD_NAME, jp.nextToken());
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        // can either advance to first START_OBJECT, or clear current token;
        // explicitly passed JsonParser MUST point to the first token of
        // the first element
        assertToken(JsonToken.START_OBJECT, jp.nextToken());
        
        Iterator<Bean> it = MAPPER.reader(Bean.class).readValues(jp);

        assertTrue(it.hasNext());
        Bean b = it.next();
        assertEquals(3, b.a);
        assertTrue(it.hasNext());
        b = it.next();
        assertEquals(27, b.a);
        assertFalse(it.hasNext());
        jp.close();
    }

    public void testNonRootMapsWithParser() throws Exception
    {
        final String JSON = "[{\"a\":3},{\"a\":27}]";
        JsonParser jp = MAPPER.getFactory().createParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());

        // can either advance to first START_OBJECT, or clear current token;
        // explicitly passed JsonParser MUST point to the first token of
        // the first element
        jp.clearCurrentToken();
        
        Iterator<Map<?,?>> it = MAPPER.reader(Map.class).readValues(jp);

        assertTrue(it.hasNext());
        Map<?,?> map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(3), map.get("a"));
        assertTrue(it.hasNext());
        map = it.next();
        assertEquals(1, map.size());
        assertEquals(Integer.valueOf(27), map.get("a"));
        assertFalse(it.hasNext());
        jp.close();
    }

    public void testNonRootMapsWithObjectReader() throws Exception
    {
        String JSON = "[{ \"hi\": \"ho\", \"neighbor\": \"Joe\" },\n"
            +"{\"boy\": \"howdy\", \"huh\": \"what\"}]";
        final MappingIterator<Map<String, Object>> iterator = MAPPER
                .reader()
                .withType(new TypeReference<Map<String, Object>>(){})
                .readValues(JSON);

        Map<String,Object> map;
        assertTrue(iterator.hasNext());
        map = iterator.nextValue();
        assertEquals(2, map.size());
        assertTrue(iterator.hasNext());
        map = iterator.nextValue();
        assertEquals(2, map.size());
        assertFalse(iterator.hasNext());
    }
    
    public void testNonRootArraysUsingParser() throws Exception
    {
        final String JSON = "[[1],[3]]";
        JsonParser jp = MAPPER.getFactory().createParser(JSON);
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        
        // Important: as of 2.1, START_ARRAY can only be skipped if the
        // target type is NOT a Collection or array Java type.
        // So we have to explicitly skip it in this particular case.
        assertToken(JsonToken.START_ARRAY, jp.nextToken());
        
        Iterator<int[]> it = MAPPER.readValues(jp, int[].class);

        assertTrue(it.hasNext());
        int[] array = it.next();
        assertEquals(1, array.length);
        assertEquals(1, array[0]);
        assertTrue(it.hasNext());
        array = it.next();
        assertEquals(1, array.length);
        assertEquals(3, array[0]);
        assertFalse(it.hasNext());
        jp.close();
    }
}
