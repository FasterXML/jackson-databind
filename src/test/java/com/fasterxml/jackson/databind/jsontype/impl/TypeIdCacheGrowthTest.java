package com.fasterxml.jackson.databind.jsontype.impl;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.deser.DefaultDeserializationContext;
import com.fasterxml.jackson.databind.deser.std.NullifyingDeserializer;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

// [databind#6203]
/**
 * Tests to verify that unrecognized type ids are NOT retained in the per-{@link
 * com.fasterxml.jackson.databind.jsontype.TypeDeserializer} type id to deserializer
 * lookup cache: only ids that actually resolve to a subtype are cached.
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
        private static final long serialVersionUID = 1L;

        @Override
        public JsonTypeInfo.Id getMechanism() { return JsonTypeInfo.Id.NAME; }

        @Override
        public String idFromValue(Object value) {
            return idFromValueAndType(value, value.getClass());
        }

        @Override
        public String idFromValueAndType(Object value, Class<?> type) {
            return (type == Impl.class) ? "impl" : "?";
        }

        @Override
        public JavaType typeFromId(DatabindContext ctxt, String id) {
            return "impl".equals(id) ? ctxt.constructType(Impl.class) : null;
        }
    }

    private final static int UNKNOWN_ID_COUNT = 100;

    private final ObjectMapper MAPPER = newJsonMapper();

    // Unknown type ids resolved via `defaultImpl` must not be cached
    @Test
    public void unknownTypeIdsWithDefaultImplNotCached() throws Exception
    {
        DeserializationContext ctxt = _context(MAPPER.getDeserializationConfig());
        AsPropertyTypeDeserializer typeDeser = _typeDeserializer(MAPPER.constructType(DefaultImpl.class));

        JsonDeserializer<Object> defaultDeser = null;
        for (int i = 0; i < UNKNOWN_ID_COUNT; ++i) {
            JsonDeserializer<Object> deser = typeDeser._findDeserializer(ctxt, "unknown-"+i);
            if (defaultDeser == null) {
                defaultDeser = deser;
            } else {
                // ... and all unknown ids share the very same `defaultImpl` deserializer
                assertSame(defaultDeser, deser);
            }
        }
        assertEquals(0, typeDeser._deserializers.size());

        // But actually resolvable type ids are still cached
        typeDeser._findDeserializer(ctxt, "impl");
        typeDeser._findDeserializer(ctxt, "impl");
        assertEquals(1, typeDeser._deserializers.size());
    }

    // Unknown type ids handled by `NullifyingDeserializer` must not be cached, either
    @Test
    public void unknownTypeIdsWithoutFailOnInvalidSubtypeNotCached() throws Exception
    {
        DeserializationContext ctxt = _context(MAPPER.getDeserializationConfig()
                .without(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE));
        AsPropertyTypeDeserializer typeDeser = _typeDeserializer(null);

        for (int i = 0; i < UNKNOWN_ID_COUNT; ++i) {
            assertSame(NullifyingDeserializer.instance,
                    typeDeser._findDeserializer(ctxt, "unknown-"+i));
        }
        assertEquals(0, typeDeser._deserializers.size());

        typeDeser._findDeserializer(ctxt, "impl");
        assertEquals(1, typeDeser._deserializers.size());
    }

    private DeserializationContext _context(DeserializationConfig config) {
        return ((DefaultDeserializationContext) MAPPER.getDeserializationContext())
                .createInstance(config, null, null);
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
