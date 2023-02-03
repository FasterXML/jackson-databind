package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// Unit tests for [databind#1868], [databind#1880], [databind#2265]
public class UtilCollectionsTypesTest extends BaseMapTest
{
    private final ObjectMapper DEFAULT_MAPPER = JsonMapper.builder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                    ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
            .build();

    /*
    /**********************************************************
    /* Unit tests, "empty" types
    /**********************************************************
     */

    public void testEmptyList() throws Exception {
        _verifyCollection(Collections.emptyList());
    }

    public void testEmptySet() throws Exception {
        _verifyCollection(Collections.emptySet());
    }

    public void testEmptyMap() throws Exception {
        _verifyMap(Collections.emptyMap());
    }

    /*
    /**********************************************************
    /* Unit tests, "singleton" types
    /**********************************************************
     */

    public void testSingletonList() throws Exception {
        _verifyCollection(Collections.singletonList("TheOne"));
    }

    public void testSingletonSet() throws Exception {
        _verifyCollection(Collections.singleton("TheOne"));
    }

    public void testSingletonMap() throws Exception {
        _verifyMap(Collections.singletonMap("foo", "bar"));
    }

    /*
    /**********************************************************
    /* Unit tests, "unmodifiable" types
    /**********************************************************
     */

    public void testUnmodifiableList() throws Exception {
        _verifyCollection(Collections.unmodifiableList(Arrays.asList("first", "second")));
    }

    // [databind#2265]
    public void testUnmodifiableListFromLinkedList() throws Exception {
        final List<String> input = new LinkedList<>();
        input.add("first");
        input.add("second");

        // Can't use simple "_verifyCollection" as type may change; instead use
        // bit more flexible check:
        Collection<?> act = _writeReadCollection(Collections.unmodifiableList(input));
        assertEquals(input, act);

        // and this check may be bit fragile (may need to revisit), but is good enough for now:
        assertEquals(Collections.unmodifiableList(new ArrayList<>(input)).getClass(), act.getClass());
    }

    public void testUnmodifiableSet() throws Exception
    {
        Set<String> input = new LinkedHashSet<>(Arrays.asList("first", "second"));
        _verifyCollection(Collections.unmodifiableSet(input));
    }

    public void testUnmodifiableMap() throws Exception
    {
        Map<String,String> input = new LinkedHashMap<>();
        input.put("a", "b");
        input.put("c", "d");
        _verifyMap(Collections.unmodifiableMap(input));
    }

    /*
    /**********************************************************
    /* Unit tests, "synchronized" types, [databind#3009]
    /**********************************************************
     */

    // [databind#3009]
    public void testSynchronizedCollection() throws Exception
    {
        // 07-Jan-2021, tatu: Some oddities, need to inline checking:
        final Collection<?> input = Collections.synchronizedCollection(
                Arrays.asList("first", "second"));
        final Collection<?> output = _writeReadCollection(input);
        final Class<?> actType = output.getClass();
        if (!Collection.class.isAssignableFrom(actType)) {
            fail("Should be subtype of java.util.Collection, is: "+actType.getName());
        }

        // And for some reason iteration order varies or something: direct equality
        // check fails, so simply check contents:
        assertEquals(input.size(), output.size());
        assertEquals(new ArrayList<>(input),
                new ArrayList<>(output));
    }

    // [databind#3009]
    public void testSynchronizedSet() throws Exception {
        Set<String> input = new LinkedHashSet<>(Arrays.asList("first", "second"));
        _verifyCollection(Collections.synchronizedSet(input));
    }

    // [databind#3009]
    public void testSynchronizedListRandomAccess() throws Exception {
        _verifyCollection(Collections.synchronizedList(
                Arrays.asList("first", "second")));
    }

    // [databind#3009]
    public void testSynchronizedListLinked() throws Exception {
        final List<String> linked = new LinkedList<>();
        linked.add("first");
        linked.add("second");
        _verifyApproxCollection(Collections.synchronizedList(linked),
                List.class);
    }

    // [databind#3009]
    public void testSynchronizedMap() throws Exception {
        Map<String,String> input = new LinkedHashMap<>();
        input.put("a", "b");
        input.put("c", "d");
        _verifyMap(Collections.synchronizedMap(input));
    }

    /*
    /**********************************************************
    /* Unit tests, other
    /**********************************************************
     */

    public void testArraysAsList() throws Exception
    {
        // Here there are no semantics to preserve, so simply check that
        // contents remain the same
        List<String> input = Arrays.asList("a", "bc", "def");
        String json = DEFAULT_MAPPER.writeValueAsString(input);
        @SuppressWarnings("unchecked")
        List<String> result = (List<String>) DEFAULT_MAPPER.readValue(json, List.class);
        assertEquals(input, result);
        // 21-Aug-2022, tatu: [databind#3565] should try to NOT make it
        //   unmodifiable
        result.set(1, "b");
    }

    /*
    /**********************************************************
    /* Helper methods
    /**********************************************************
     */

    protected void _verifyCollection(Collection<?> exp) throws Exception {
        Collection<?> act = _writeReadCollection(exp);
        assertEquals(exp, act);
        assertEquals(exp.getClass(), act.getClass());
    }

    protected void _verifyApproxCollection(Collection<?> exp,
            Class<?> expType) throws Exception
    {
        Collection<?> act = _writeReadCollection(exp);
        assertEquals(exp, act);
        final Class<?> actType = act.getClass();
        if (!expType.isAssignableFrom(actType)) {
            fail("Should be subtype of "+expType.getName()+", is: "+actType.getName());
        }
    }

    protected Collection<?> _writeReadCollection(Collection<?> input) throws Exception {
        final String json = DEFAULT_MAPPER.writeValueAsString(input);
        return DEFAULT_MAPPER.readValue(json, Collection.class);
    }

    protected void _verifyMap(Map<?,?> exp) throws Exception
    {
        String json = DEFAULT_MAPPER.writeValueAsString(exp);
        Map<?,?> act = DEFAULT_MAPPER.readValue(json, Map.class);

        assertEquals(exp, act);
        assertEquals(exp.getClass(), act.getClass());
    }
}
