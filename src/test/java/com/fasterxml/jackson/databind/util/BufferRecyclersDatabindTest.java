package com.fasterxml.jackson.databind.util;

import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.core.util.RecyclerPool;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;

// For [databind#4321]: basic test
public class BufferRecyclersDatabindTest
    extends BaseMapTest
{
    @JsonPropertyOrder({ "a", "b" })
    static class Pojo4321 {
        public int a;
        public String b;

        public Pojo4321(int a, String b) {
            this.a = a;
            this.b = b;
        }
        protected Pojo4321() { }
    }

    // // Parsers with RecyclerPools:

    public void testParserWithThreadLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.threadLocalPool());
    }

    public void testParserWithNopLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.nonRecyclingPool());
    }

    public void testParserWithDequeuPool() throws Exception {
        _testParser(JsonRecyclerPools.newConcurrentDequePool());
        _testParser(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    public void testParserWithLockFreePool() throws Exception {
        _testParser(JsonRecyclerPools.newLockFreePool());
        _testParser(JsonRecyclerPools.sharedLockFreePool());
    }

    public void testParserWithBoundedPool() throws Exception {
        _testParser(JsonRecyclerPools.newBoundedPool(5));
        _testParser(JsonRecyclerPools.sharedBoundedPool());
    }

    private void _testParser(RecyclerPool<BufferRecycler> pool) throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder(
                JsonFactory.builder()
                    .recyclerPool(pool)
                    .build()).build();
        final String DOC = "{\"a\":123,\"b\":\"foobar\"}";

        // Let's first test using char-backed parser
        Pojo4321 value = mapper.readerFor(Pojo4321.class)
                .readValue(DOC);
        assertEquals(123, value.a);
        assertEquals("foobar", value.b);

        // and then byte-backed parser
        value = mapper.readerFor(Pojo4321.class)
                .readValue(utf8Bytes(DOC));
        assertEquals(123, value.a);
        assertEquals("foobar", value.b);
    }

    // // Generators with RecyclerPools:

    public void testGeneratorWithThreadLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.threadLocalPool());
    }

    public void testGeneratorWithNopLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.nonRecyclingPool());
    }

    public void testGeneratorWithDequeuPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newConcurrentDequePool());
        _testGenerator(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    public void testGeneratorWithLockFreePool() throws Exception {
        _testGenerator(JsonRecyclerPools.newLockFreePool());
        _testGenerator(JsonRecyclerPools.sharedLockFreePool());
    }

    public void testGeneratorWithBoundedPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newBoundedPool(5));
        _testGenerator(JsonRecyclerPools.sharedBoundedPool());
    }

    private void _testGenerator(RecyclerPool<BufferRecycler> pool) throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder(
                JsonFactory.builder()
                    .recyclerPool(pool)
                    .build()).build();
        final String EXP = "{\"a\":-42,\"b\":\"bogus\"}";

        // First write as String
        assertEquals(EXP, mapper.writeValueAsString(new Pojo4321(-42, "bogus")));

        // and then as bytes
        assertEquals(EXP, new String(mapper.writeValueAsBytes(new Pojo4321(-42, "bogus")),
                StandardCharsets.UTF_8));
    }
}
