package tools.jackson.databind.deser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for {@link MapEntryDeserializer} and its POJOWrapped variant.
 */
public class MapEntryDeserializationTest extends DatabindTestUtil
{
    static class EntryHolder {
        public Map.Entry<String, Integer> entry;
    }

    static class PojoFormatEntryHolder {
        @JsonFormat(shape = JsonFormat.Shape.POJO)
        public Map.Entry<String, Integer> entry;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /*
    /**********************************************************
    /* Tests for default (natural) format
    /**********************************************************
     */

    @Test
    public void testSimpleStringIntEntry() throws Exception
    {
        Map.Entry<String, Integer> result = MAPPER.readValue(
                a2q("{'foo':42}"),
                new TypeReference<Map.Entry<String, Integer>>() { });
        assertNotNull(result);
        assertEquals("foo", result.getKey());
        assertEquals(Integer.valueOf(42), result.getValue());
    }

    @Test
    public void testStringStringEntry() throws Exception
    {
        Map.Entry<String, String> result = MAPPER.readValue(
                a2q("{'key':'value'}"),
                new TypeReference<Map.Entry<String, String>>() { });
        assertNotNull(result);
        assertEquals("key", result.getKey());
        assertEquals("value", result.getValue());
    }

    @Test
    public void testIntegerKeyEntry() throws Exception
    {
        Map.Entry<Integer, String> result = MAPPER.readValue(
                a2q("{'123':'hello'}"),
                new TypeReference<Map.Entry<Integer, String>>() { });
        assertNotNull(result);
        assertEquals(Integer.valueOf(123), result.getKey());
        assertEquals("hello", result.getValue());
    }

    @Test
    public void testNullValueEntry() throws Exception
    {
        Map.Entry<String, String> result = MAPPER.readValue(
                a2q("{'key':null}"),
                new TypeReference<Map.Entry<String, String>>() { });
        assertNotNull(result);
        assertEquals("key", result.getKey());
        assertNull(result.getValue());
    }

    @Test
    public void testEntryInHolder() throws Exception
    {
        EntryHolder result = MAPPER.readValue(
                a2q("{'entry':{'abc':99}}"), EntryHolder.class);
        assertNotNull(result);
        assertNotNull(result.entry);
        assertEquals("abc", result.entry.getKey());
        assertEquals(Integer.valueOf(99), result.entry.getValue());
    }

    /*
    /**********************************************************
    /* Tests for error cases
    /**********************************************************
     */

    @Test
    public void testEmptyObjectFails() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{}"),
                    new TypeReference<Map.Entry<String, Integer>>() { });
            fail("Should not pass with empty object");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot deserialize");
        }
    }

    @Test
    public void testMultipleEntriesFails() throws Exception
    {
        try {
            MAPPER.readValue(a2q("{'a':1,'b':2}"),
                    new TypeReference<Map.Entry<String, Integer>>() { });
            fail("Should not pass with multiple entries");
        } catch (MismatchedInputException e) {
            verifyException(e, "more than one entry");
        }
    }

    /*
    /**********************************************************
    /* Tests for List of entries
    /**********************************************************
     */

    @Test
    public void testListOfEntries() throws Exception
    {
        List<Map.Entry<String, Integer>> result = MAPPER.readValue(
                a2q("[{'a':1},{'b':2},{'c':3}]"),
                new TypeReference<List<Map.Entry<String, Integer>>>() { });
        assertNotNull(result);
        assertEquals(3, result.size());
        assertEquals("a", result.get(0).getKey());
        assertEquals(Integer.valueOf(1), result.get(0).getValue());
        assertEquals("c", result.get(2).getKey());
        assertEquals(Integer.valueOf(3), result.get(2).getValue());
    }

    /*
    /**********************************************************
    /* Tests for complex value types
    /**********************************************************
     */

    @Test
    public void testEntryWithBeanValue() throws Exception
    {
        Map.Entry<String, StringWrapper> result = MAPPER.readValue(
                a2q("{'key':{'str':'wrapped'}}"),
                new TypeReference<Map.Entry<String, StringWrapper>>() { });
        assertNotNull(result);
        assertEquals("key", result.getKey());
        assertNotNull(result.getValue());
        assertEquals("wrapped", result.getValue().str);
    }

    @Test
    public void testEntryWithListValue() throws Exception
    {
        Map.Entry<String, List<Integer>> result = MAPPER.readValue(
                a2q("{'nums':[1,2,3]}"),
                new TypeReference<Map.Entry<String, List<Integer>>>() { });
        assertNotNull(result);
        assertEquals("nums", result.getKey());
        assertEquals(Arrays.asList(1, 2, 3), result.getValue());
    }

    /*
    /**********************************************************
    /* Tests for POJO-wrapped format
    /**********************************************************
     */

    @Test
    public void testPojoWrappedFormat() throws Exception
    {
        PojoFormatEntryHolder result = MAPPER.readValue(
                a2q("{'entry':{'key':'myKey','value':55}}"),
                PojoFormatEntryHolder.class);
        assertNotNull(result);
        assertNotNull(result.entry);
        assertEquals("myKey", result.entry.getKey());
        assertEquals(Integer.valueOf(55), result.entry.getValue());
    }

    @Test
    public void testPojoWrappedFormatNullValue() throws Exception
    {
        PojoFormatEntryHolder result = MAPPER.readValue(
                a2q("{'entry':{'key':'k','value':null}}"),
                PojoFormatEntryHolder.class);
        assertNotNull(result);
        assertNotNull(result.entry);
        assertEquals("k", result.entry.getKey());
        assertNull(result.entry.getValue());
    }

    @Test
    public void testPojoWrappedFormatNullKey() throws Exception
    {
        PojoFormatEntryHolder result = MAPPER.readValue(
                a2q("{'entry':{'key':null,'value':10}}"),
                PojoFormatEntryHolder.class);
        assertNotNull(result);
        assertNotNull(result.entry);
        assertNull(result.entry.getKey());
        assertEquals(Integer.valueOf(10), result.entry.getValue());
    }

    /*
    /**********************************************************
    /* Tests for round-trip
    /**********************************************************
     */

    @Test
    public void testRoundTrip() throws Exception
    {
        Map.Entry<String, Integer> orig = new AbstractMap.SimpleEntry<>("test", 123);
        String json = MAPPER.writeValueAsString(orig);
        Map.Entry<String, Integer> result = MAPPER.readValue(json,
                new TypeReference<Map.Entry<String, Integer>>() { });
        assertNotNull(result);
        assertEquals(orig.getKey(), result.getKey());
        assertEquals(orig.getValue(), result.getValue());
    }
}
