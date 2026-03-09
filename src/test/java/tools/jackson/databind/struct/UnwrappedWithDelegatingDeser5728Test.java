package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.ValueDeserializerModifier;
import tools.jackson.databind.deser.bean.BeanDeserializerBase;
import tools.jackson.databind.deser.std.DelegatingDeserializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5728]: @JsonUnwrapped fails when deserializer is wrapped
//   with DelegatingDeserializer
public class UnwrappedWithDelegatingDeser5728Test extends DatabindTestUtil
{
    static class Inner5728 {
        public String p1;
        public String p2;
    }

    static class Outer5728 {
        @JsonUnwrapped
        public Inner5728 inner;
    }

    // Minimal DelegatingDeserializer that just delegates everything
    static class WrappingDeserializer extends DelegatingDeserializer {
        WrappingDeserializer(ValueDeserializer<?> d) {
            super(d);
        }

        @Override
        protected ValueDeserializer<?> newDelegatingInstance(ValueDeserializer<?> newDelegatee) {
            return new WrappingDeserializer(newDelegatee);
        }
    }

    @SuppressWarnings("serial")
    static class WrappingModifier extends ValueDeserializerModifier {
        @Override
        public ValueDeserializer<?> modifyDeserializer(DeserializationConfig config,
                BeanDescription.Supplier beanDescRef, ValueDeserializer<?> deserializer) {
            if (deserializer instanceof BeanDeserializerBase) {
                return new WrappingDeserializer(deserializer);
            }
            return deserializer;
        }
    }

    private final ObjectMapper UNWRAP_MAPPER = JsonMapper.builder()
            .addModule(new SimpleModule()
                .setDeserializerModifier(new WrappingModifier()))
            .build();
    
    // Verify that @JsonUnwrapped works when the inner type's deserializer
    // is wrapped with a DelegatingDeserializer
    @Test
    public void testUnwrappedWithDelegatingDeserializer() throws Exception
    {
        String json = a2q("{'p1':'value1','p2':'value2'}");
        Outer5728 result = UNWRAP_MAPPER.readValue(json, Outer5728.class);
        assertNotNull(result.inner, "Unwrapped inner object should not be null");
        assertEquals("value1", result.inner.p1);
        assertEquals("value2", result.inner.p2);
    }

    // Same test but with FAIL_ON_UNKNOWN_PROPERTIES enabled — this is
    // where the issue manifests as an UnrecognizedPropertyException
    @Test
    public void testUnwrappedWithDelegatingDeserializerAndFailOnUnknown() throws Exception
    {
        ObjectReader r = UNWRAP_MAPPER.readerFor(Outer5728.class)
                .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        String json = a2q("{'p1':'value1','p2':'value2'}");
        Outer5728 result = r.readValue(json);
        assertNotNull(result.inner, "Unwrapped inner object should not be null");
        assertEquals("value1", result.inner.p1);
        assertEquals("value2", result.inner.p2);
    }
}
