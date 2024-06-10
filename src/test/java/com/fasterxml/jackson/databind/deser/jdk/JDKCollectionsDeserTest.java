package com.fasterxml.jackson.databind.deser.jdk;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.core.type.TypeReference;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.*;

/**
 * Tests for special collection/map types via `java.util.Collections`
 */
public class JDKCollectionsDeserTest
{
    static class XBean {
        public int x;

        public XBean() { }
        public XBean(int x) { this.x = x; }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final static ObjectMapper MAPPER = newJsonMapper();

    // And then a round-trip test for singleton collections
    @Test
    public void testSingletonCollections() throws Exception
    {
        final TypeReference<List<XBean>> xbeanListType = new TypeReference<List<XBean>>() { };

        String json = MAPPER.writeValueAsString(Collections.singleton(new XBean(3)));
        Collection<XBean> result = MAPPER.readValue(json, xbeanListType);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(3, result.iterator().next().x);

        json = MAPPER.writeValueAsString(Collections.singletonList(new XBean(28)));
        result = MAPPER.readValue(json, xbeanListType);
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(28, result.iterator().next().x);
    }

    // [databind#1868]: Verify class name serialized as is
    @Test
    public void testUnmodifiableSet() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY)
                .build();

        Set<String> theSet = Collections.unmodifiableSet(Collections.singleton("a"));
        String json = mapper.writeValueAsString(theSet);

        assertEquals("[\"java.util.Collections$UnmodifiableSet\",[\"a\"]]", json);

        Set<?> result = mapper.readValue(json, Set.class);
        assertNotNull(result);
        assertEquals(1, result.size());
    }

    // [databind#4262]: Handle problem of `null`s for `TreeSet`
    @Test
    public void testNullsWithTreeSet() throws Exception
    {
        try {
            MAPPER.readValue("[ \"acb\", null, 123 ]", TreeSet.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "`java.util.Collection` of type ");
            verifyException(e, " does not accept `null` values");
        }
    }
}
