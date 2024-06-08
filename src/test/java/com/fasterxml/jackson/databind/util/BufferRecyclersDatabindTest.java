package com.fasterxml.jackson.databind.util;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.nio.charset.StandardCharsets;
import java.util.function.Predicate;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import com.fasterxml.jackson.core.*;

import com.fasterxml.jackson.core.util.BufferRecycler;
import com.fasterxml.jackson.core.util.JsonRecyclerPools;
import com.fasterxml.jackson.core.util.RecyclerPool;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// For [databind#4321]: basic test
public class BufferRecyclersDatabindTest extends DatabindTestUtil
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

    @Test
    public void testParserWithThreadLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.threadLocalPool());
    }

    @Test
    public void testParserWithNopLocalPool() throws Exception {
        _testParser(JsonRecyclerPools.nonRecyclingPool());
    }

    @Test
    public void testParserWithDequeuPool() throws Exception {
        _testParser(JsonRecyclerPools.newConcurrentDequePool());
        _testParser(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    @Test
    @Deprecated // tests deprecated impl
    public void testParserWithLockFreePool() throws Exception {
        _testParser(JsonRecyclerPools.newLockFreePool());
        _testParser(JsonRecyclerPools.sharedLockFreePool());
    }

    @Test
    public void testParserWithBoundedPool() throws Exception {
        _testParser(JsonRecyclerPools.newBoundedPool(5));
        _testParser(JsonRecyclerPools.sharedBoundedPool());
    }

    @Test
    public void testParserWithHybridPool() throws Exception {
        _testParser(new HybridTestPool());
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

    @Test
    public void testGeneratorWithThreadLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.threadLocalPool());
    }

    @Test
    public void testGeneratorWithNopLocalPool() throws Exception {
        _testGenerator(JsonRecyclerPools.nonRecyclingPool());
    }

    @Test
    public void testGeneratorWithDequeuPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newConcurrentDequePool());
        _testGenerator(JsonRecyclerPools.sharedConcurrentDequePool());
    }

    @Test
    @Deprecated // tests deprecated impl
    public void testGeneratorWithLockFreePool() throws Exception {
        _testGenerator(JsonRecyclerPools.newLockFreePool());
        _testGenerator(JsonRecyclerPools.sharedLockFreePool());
    }

    @Test
    public void testGeneratorWithBoundedPool() throws Exception {
        _testGenerator(JsonRecyclerPools.newBoundedPool(5));
        _testGenerator(JsonRecyclerPools.sharedBoundedPool());
    }

    @Test
    public void testGeneratorWithHybridPool() throws Exception {
        _testGenerator(new HybridTestPool());
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

    static class HybridTestPool implements RecyclerPool<BufferRecycler>
    {
        private static final long serialVersionUID = 1L;

        private static final Predicate<Thread> isVirtual = VirtualPredicate.findIsVirtualPredicate();

        private final RecyclerPool<BufferRecycler> nativePool = JsonRecyclerPools.threadLocalPool();
        private final RecyclerPool<BufferRecycler> virtualPool = JsonRecyclerPools.newConcurrentDequePool();

        @Override
        public BufferRecycler acquirePooled() {
            return isVirtual.test(Thread.currentThread()) ?
                    virtualPool.acquirePooled() :
                    nativePool.acquirePooled();
        }

        @Override
        public void releasePooled(BufferRecycler pooled) {
            if (isVirtual.test(Thread.currentThread())) {
                virtualPool.releasePooled(pooled);
            } else {
                nativePool.releasePooled(pooled);
            }
        }

        static class VirtualPredicate {
            static final MethodHandle virtualMh = findVirtualMH();

            static MethodHandle findVirtualMH() {
                try {
                    return MethodHandles.publicLookup().findVirtual(Thread.class, "isVirtual",
                            MethodType.methodType(boolean.class));
                } catch (Exception e) {
                    return null;
                }
            }

            static Predicate<Thread> findIsVirtualPredicate() {
                if (virtualMh != null) {
                    return new Predicate<Thread>() {
                        @Override
                        public boolean test(Thread thread) {
                            try {
                                return (boolean) virtualMh.invokeExact(thread);
                            } catch (Throwable e) {
                                throw new RuntimeException(e);
                            }
                        }
                    };
                }

                return new Predicate<Thread>() {
                    @Override
                    public boolean test(Thread thread) {
                        return false;
                    }
                };
            }
        }
    }
}
