package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonTypeInfo.As;

import tools.jackson.core.*;
import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Covers the default {@code deserializeTypedWithKnownTypeId} implementation on
 * {@link TypeDeserializer} — the wrapper-array fallback that is preserved for any
 * subclass that does not extend {@code TypeDeserializerBase}. [databind#2747]
 */
class TypeDeserializerDefaultFallback2747Test extends DatabindTestUtil
{
    // Minimal TypeDeserializer that extends the abstract base directly (NOT
    // TypeDeserializerBase), so invoking `deserializeTypedWithKnownTypeId`
    // exercises the default wrapper-array fallback on TypeDeserializer.
    static class WrapperArrayTypeDeser extends TypeDeserializer
    {
        @Override public TypeDeserializer forProperty(BeanProperty prop) { return this; }
        @Override public As getTypeInclusion() { return As.WRAPPER_ARRAY; }
        @Override public String getPropertyName() { return null; }
        @Override public TypeIdResolver getTypeIdResolver() { return null; }
        @Override public Class<?> getDefaultImpl() { return null; }

        // Decodes the synthetic `[typeId, value]` produced by the default
        // deserializeTypedWithKnownTypeId(...) implementation. Returns
        // "typeId:<number>" so the test can assert the round trip.
        @Override
        public Object deserializeTypedFromArray(JsonParser p, DeserializationContext ctxt)
            throws JacksonException
        {
            // p at START_ARRAY
            p.nextToken();
            String typeId = p.getString();
            p.nextToken();
            int value = p.getIntValue();
            p.nextToken(); // END_ARRAY
            return typeId + ":" + value;
        }

        @Override public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) throws JacksonException { return deserializeTypedFromArray(p, ctxt); }
        @Override public Object deserializeTypedFromScalar(JsonParser p, DeserializationContext ctxt) throws JacksonException { return deserializeTypedFromArray(p, ctxt); }
        @Override public Object deserializeTypedFromAny(JsonParser p, DeserializationContext ctxt) throws JacksonException { return deserializeTypedFromArray(p, ctxt); }
    }

    // Placeholder value used as the registered deserialization target; its custom
    // deserializer is the shim that calls the TypeDeserializer default.
    static class Box {
        final String payload;
        Box(String payload) { this.payload = payload; }
    }

    static class BoxDeser extends ValueDeserializer<Box>
    {
        private static final WrapperArrayTypeDeser TD = new WrapperArrayTypeDeser();

        @Override
        public Box deserialize(JsonParser p, DeserializationContext ctxt) throws JacksonException {
            // Invokes the default TypeDeserializer.deserializeTypedWithKnownTypeId
            // (NOT the TypeDeserializerBase override, since our TD extends
            // TypeDeserializer directly).
            Object decoded = TD.deserializeTypedWithKnownTypeId(p, ctxt, "tag");
            return new Box((String) decoded);
        }
    }

    @Test
    void defaultFallbackRoutesThroughDeserializeTypedFromArray() throws Exception {
        JsonMapper mapper = JsonMapper.builder()
                .addModule(new SimpleModule().addDeserializer(Box.class, new BoxDeser()))
                .build();

        Box result = mapper.readValue("42", Box.class);
        // The default impl wraps as ["tag", 42] and hands it to deserializeTypedFromArray,
        // which in our test TypeDeserializer returns "tag:42".
        assertEquals("tag:42", result.payload);
    }
}
