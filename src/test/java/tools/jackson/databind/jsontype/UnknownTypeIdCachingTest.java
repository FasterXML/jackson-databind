package tools.jackson.databind.jsontype;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonTypeIdResolver;
import tools.jackson.databind.jsontype.impl.TypeIdResolverBase;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

// [databind#6203]
/**
 * Verifies caching of type ids in the type id lookup cache of the (long-lived,
 * shared) {@link TypeDeserializer}: unresolvable ids handled via the "nullifying"
 * fallback (whose use depends on reader configuration) are not retained, whereas
 * ones handled via {@code defaultImpl} are, like resolvable type ids (cache being
 * bounded in size).
 */
public class UnknownTypeIdCachingTest extends DatabindTestUtil
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
        @Override
        public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

        @Override
        public String idFromValue(DatabindContext ctxt, Object value) {
            return idFromValueAndType(ctxt, value, value.getClass());
        }

        @Override
        public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) {
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

    // Unknown type ids resolved via `defaultImpl` ARE cached (bounded by cache size
    // limit), as `defaultImpl` does not depend on reader configuration
    @Test
    public void cachingOfUnknownIdsWithDefaultImpl() throws Exception
    {
        RESOLVE_COUNT.set(0);
        ObjectReader r = MAPPER.readerFor(Wrapper.class);

        for (int i = 0; i < 10; ++i) {
            Wrapper w = r.readValue(a2q(
                    "{'animal':{'type':'bogus-"+i+"','name':'Rex'}}"));
            assertInstanceOf(DefaultAnimal.class, w.animal);
        }
        // Every distinct unknown id has to be resolved once
        assertEquals(10, RESOLVE_COUNT.get());

        // ... and repeating a single unknown id resolves it just once, too
        RESOLVE_COUNT.set(0);
        for (int i = 0; i < 10; ++i) {
            Wrapper w = r.readValue(a2q("{'animal':{'type':'bogus','name':'Rex'}}"));
            assertInstanceOf(DefaultAnimal.class, w.animal);
        }
        assertEquals(1, RESOLVE_COUNT.get());

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
