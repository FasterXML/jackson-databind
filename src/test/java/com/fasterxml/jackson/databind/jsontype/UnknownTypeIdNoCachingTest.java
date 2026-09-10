package com.fasterxml.jackson.databind.jsontype;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonTypeIdResolver;
import com.fasterxml.jackson.databind.jsontype.impl.TypeIdResolverBase;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// [databind#6203]
/**
 * Verifies that unresolvable type ids do not accumulate in the type id lookup cache
 * of the (long-lived, shared) {@link TypeDeserializer}: they are handled via the
 * {@code defaultImpl} / nullifying fallback, neither of which depends on the id.
 * Resolvable type ids, on the other hand, must still be cached.
 */
public class UnknownTypeIdNoCachingTest extends DatabindTestUtil
{
    static final AtomicInteger RESOLVE_COUNT = new AtomicInteger();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type", defaultImpl = DefaultAnimal.class)
    @JsonTypeIdResolver(CountingIdResolver.class)
    static abstract class Animal {
        public String name;
    }

    static class Dog extends Animal { }

    static class DefaultAnimal extends Animal { }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY,
            property = "type")
    @JsonTypeIdResolver(CountingIdResolver.class)
    static abstract class Animal2 {
        public String name;
    }

    static class Dog2 extends Animal2 { }

    static class Wrapper {
        public Animal animal;
    }

    static class Wrapper2 {
        public Animal2 animal;
    }

    public static class CountingIdResolver extends TypeIdResolverBase
    {
        private static final long serialVersionUID = 1L;

        @Override
        public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

        @Override
        public String idFromValue(Object value) {
            return idFromValueAndType(value, value.getClass());
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return type.getSimpleName();
        }

        @Override
        public JavaType typeFromId(DatabindContext ctxt, String id) {
            RESOLVE_COUNT.incrementAndGet();
            if ("Dog".equals(id)) {
                return ctxt.constructType(Dog.class);
            }
            if ("Dog2".equals(id)) {
                return ctxt.constructType(Dog2.class);
            }
            return null;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Unknown type ids must not be cached: `defaultImpl` variant
    @Test
    public void noCachingOfUnknownIdsWithDefaultImpl() throws Exception
    {
        RESOLVE_COUNT.set(0);
        ObjectReader r = MAPPER.readerFor(Wrapper.class);

        for (int i = 0; i < 10; ++i) {
            Wrapper w = r.readValue(a2q(
                    "{'animal':{'type':'bogus-"+i+"','name':'Rex'}}"));
            assertInstanceOf(DefaultAnimal.class, w.animal);
        }
        // Every distinct unknown id has to be resolved anew: nothing retained
        assertEquals(10, RESOLVE_COUNT.get());

        // ... but repeating a single unknown id must not retain it, either
        RESOLVE_COUNT.set(0);
        for (int i = 0; i < 10; ++i) {
            r.readValue(a2q("{'animal':{'type':'bogus','name':'Rex'}}"));
        }
        assertEquals(10, RESOLVE_COUNT.get());

        // Whereas known type ids ARE cached: resolved just once
        RESOLVE_COUNT.set(0);
        for (int i = 0; i < 10; ++i) {
            Wrapper w = r.readValue(a2q("{'animal':{'type':'Dog','name':'Rex'}}"));
            assertInstanceOf(Dog.class, w.animal);
            assertEquals("Rex", w.animal.name);
        }
        assertEquals(1, RESOLVE_COUNT.get());
    }

    // Unknown type ids must not be cached: `FAIL_ON_INVALID_SUBTYPE` disabled variant
    @Test
    public void noCachingOfUnknownIdsWhenNullifying() throws Exception
    {
        RESOLVE_COUNT.set(0);
        ObjectReader r = MAPPER.readerFor(Wrapper2.class)
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);

        for (int i = 0; i < 10; ++i) {
            Wrapper2 w = r.readValue(a2q(
                    "{'animal':{'type':'bogus-"+i+"','name':'Rex'}}"));
            assertNull(w.animal);
        }
        assertEquals(10, RESOLVE_COUNT.get());

        RESOLVE_COUNT.set(0);
        for (int i = 0; i < 10; ++i) {
            r.readValue(a2q("{'animal':{'type':'bogus','name':'Rex'}}"));
        }
        assertEquals(10, RESOLVE_COUNT.get());

        RESOLVE_COUNT.set(0);
        for (int i = 0; i < 10; ++i) {
            Wrapper2 w = r.readValue(a2q("{'animal':{'type':'Dog2','name':'Rex'}}"));
            assertInstanceOf(Dog2.class, w.animal);
        }
        assertEquals(1, RESOLVE_COUNT.get());
    }
}
