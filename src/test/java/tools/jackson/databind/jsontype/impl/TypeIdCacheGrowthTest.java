package tools.jackson.databind.jsontype.impl;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationProblemHandler;
import tools.jackson.databind.deser.std.NullifyingDeserializer;
import tools.jackson.databind.exc.InvalidTypeIdException;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

// [databind#6203]
/**
 * Tests to verify that the per-{@link
 * tools.jackson.databind.jsontype.TypeDeserializer} type id to deserializer
 * lookup cache does not grow without bound: of unrecognized type ids, only ones
 * handled via {@code defaultImpl} are cached, and cache size is capped.
 *<p>
 * Note: in package {@code ...jsontype.impl} to have access to protected
 * {@code _deserializers} / {@code _findDeserializer()} of {@link TypeDeserializerBase}.
 */
public class TypeIdCacheGrowthTest extends DatabindTestUtil
{
    static class Base { }

    static class Impl extends Base { public int x; }

    static class DefaultImpl extends Base { }

    // Resolves exactly one id ("impl"), everything else is unknown
    static class SingleIdResolver extends TypeIdResolverBase
    {
        @Override
        public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

        @Override
        public String idFromValue(DatabindContext ctxt, Object value) {
            return idFromValueAndType(ctxt, value, value.getClass());
        }

        @Override
        public String idFromValueAndType(DatabindContext ctxt, Object value, Class<?> type) {
            return (type == Impl.class) ? "impl" : "?";
        }

        @Override
        public JavaType typeFromId(DatabindContext ctxt, String id) {
            return "impl".equals(id) ? ctxt.constructType(Impl.class) : null;
        }
    }

    private final static int UNKNOWN_ID_COUNT = 100;

    private final ObjectMapper MAPPER = newJsonMapper();

    // Unknown type ids resolved via `defaultImpl` ARE cached (to avoid re-resolving
    // them every time), since `defaultImpl` does not depend on reader configuration;
    // growth is bounded by `MAX_CACHED_TYPE_IDS`
    @Test
    public void unknownTypeIdsWithDefaultImplCached() throws Exception
    {
        DeserializationContext ctxt = _context(MAPPER);
        AsPropertyTypeDeserializer typeDeser = _typeDeserializer(MAPPER.constructType(DefaultImpl.class));

        ValueDeserializer<Object> defaultDeser = null;
        for (int i = 0; i < UNKNOWN_ID_COUNT; ++i) {
            ValueDeserializer<Object> deser = typeDeser._findDeserializer(ctxt, "unknown-"+i);
            if (defaultDeser == null) {
                defaultDeser = deser;
            } else {
                // ... and all unknown ids share the very same `defaultImpl` deserializer
                assertSame(defaultDeser, deser);
            }
        }
        assertEquals(UNKNOWN_ID_COUNT, typeDeser._deserializers.size());

        // As are actually resolvable type ids
        typeDeser._findDeserializer(ctxt, "impl");
        typeDeser._findDeserializer(ctxt, "impl");
        assertEquals(UNKNOWN_ID_COUNT + 1, typeDeser._deserializers.size());
    }

    // Unknown type ids handled by `NullifyingDeserializer` must not be cached, either
    @Test
    public void unknownTypeIdsWithoutFailOnInvalidSubtypeNotCached() throws Exception
    {
        DeserializationContext ctxt = _context(jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE)
                .build());
        AsPropertyTypeDeserializer typeDeser = _typeDeserializer(null);

        for (int i = 0; i < UNKNOWN_ID_COUNT; ++i) {
            assertSame(NullifyingDeserializer.instance,
                    typeDeser._findDeserializer(ctxt, "unknown-"+i));
        }
        assertEquals(0, typeDeser._deserializers.size());

        typeDeser._findDeserializer(ctxt, "impl");
        assertEquals(1, typeDeser._deserializers.size());
    }

    // Unknown type ids resolved by a `DeserializationProblemHandler` must not be cached
    @Test
    public void unknownTypeIdsFromProblemHandlerNotCached() throws Exception
    {
        DeserializationContext ctxt = _context(jsonMapperBuilder()
                .addHandler(new DeserializationProblemHandler() {
                    @Override
                    public JavaType handleUnknownTypeId(DeserializationContext c,
                            JavaType baseType, String subTypeId, TypeIdResolver idResolver,
                            String failureMsg) {
                        return c.constructType(Impl.class);
                    }
                })
                .build());
        AsPropertyTypeDeserializer typeDeser = _typeDeserializer(null);

        for (int i = 0; i < UNKNOWN_ID_COUNT; ++i) {
            typeDeser._findDeserializer(ctxt, "unknown-"+i);
        }
        assertEquals(0, typeDeser._deserializers.size());

        // ... nor leak to a reader that has no such handler
        DeserializationContext plainCtxt = _context(MAPPER);
        assertThrows(InvalidTypeIdException.class,
                () -> typeDeser._findDeserializer(plainCtxt, "unknown-0"));

        typeDeser._findDeserializer(ctxt, "impl");
        assertEquals(1, typeDeser._deserializers.size());
    }

    // Even type ids that do resolve (like different spellings of the same type id)
    // must not grow the cache without bound
    @Test
    public void resolvedTypeIdCacheIsBounded() throws Exception
    {
        DeserializationContext ctxt = _context(MAPPER);
        JavaType baseType = MAPPER.constructType(Base.class);
        SingleIdResolver idRes = new SingleIdResolver() {
            @Override
            public JavaType typeFromId(DatabindContext c, String id) {
                return id.startsWith("impl") ? c.constructType(Impl.class) : null;
            }
        };
        idRes.init(baseType);
        AsPropertyTypeDeserializer typeDeser = new AsPropertyTypeDeserializer(baseType, idRes,
                "type", false, null, JsonTypeInfo.As.PROPERTY, true);

        final int max = TypeDeserializerBase.MAX_CACHED_TYPE_IDS;
        for (int i = 0; i < max; ++i) {
            typeDeser._findDeserializer(ctxt, "impl-"+i);
        }
        assertEquals(max, typeDeser._deserializers.size());

        // One more: cache is full, so gets cleared before new entry is added
        typeDeser._findDeserializer(ctxt, "impl-"+max);
        assertEquals(1, typeDeser._deserializers.size());
    }

    // Overlong type ids must not be cached, as cache is only bounded by entry count
    // (and type ids may be as long as maximum String length allowed)
    @Test
    public void overlongTypeIdsNotCached() throws Exception
    {
        DeserializationContext ctxt = _context(MAPPER);
        AsPropertyTypeDeserializer typeDeser = _typeDeserializer(MAPPER.constructType(DefaultImpl.class));

        final int maxLen = TypeDeserializerBase.MAX_CACHED_TYPE_ID_LENGTH;
        StringBuilder sb = new StringBuilder("unknown-");
        while (sb.length() <= maxLen) {
            sb.append('x');
        }
        final String overlongId = sb.toString();
        assertEquals(maxLen + 1, overlongId.length());

        ValueDeserializer<Object> deser = typeDeser._findDeserializer(ctxt, overlongId);
        assertSame(deser, typeDeser._findDeserializer(ctxt, overlongId));
        assertEquals(0, typeDeser._deserializers.size());

        // But one of exactly maximum length is still cached
        typeDeser._findDeserializer(ctxt, overlongId.substring(0, maxLen));
        assertEquals(1, typeDeser._deserializers.size());
    }

    private DeserializationContext _context(ObjectMapper mapper) {
        return mapper._deserializationContext();
    }

    private AsPropertyTypeDeserializer _typeDeserializer(JavaType defaultImpl)
    {
        JavaType baseType = MAPPER.constructType(Base.class);
        SingleIdResolver idRes = new SingleIdResolver();
        idRes.init(baseType);
        return new AsPropertyTypeDeserializer(baseType, idRes, "type", false, defaultImpl,
                JsonTypeInfo.As.PROPERTY, true);
    }
}
