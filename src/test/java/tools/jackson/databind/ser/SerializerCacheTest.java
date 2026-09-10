package tools.jackson.databind.ser;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.JavaType;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;
import tools.jackson.databind.ser.impl.ReadOnlyClassToSerializerMap;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertSame;

public class SerializerCacheTest extends DatabindTestUtil
{
    static class TestSerializer extends ValueSerializer<Object>
    {
        @Override
        public void serialize(Object value, JsonGenerator gen, SerializationContext ctxt) { }
    }

    @Test
    public void replaceTypedClassSerializerInvalidatesReadOnlyLookup()
    {
        SerializerCache cache = new SerializerCache();
        TestSerializer serializer1 = new TestSerializer();
        TestSerializer serializer2 = new TestSerializer();

        cache.addTypedSerializer(String.class, serializer1);
        ReadOnlyClassToSerializerMap lookup = cache.getReadOnlyLookupMap();
        assertSame(serializer1, lookup.typedValueSerializer(String.class));

        cache.addTypedSerializer(String.class, serializer2);

        assertSame(serializer2, cache.getReadOnlyLookupMap().typedValueSerializer(String.class));
    }

    @Test
    public void replaceTypedJavaTypeSerializerInvalidatesReadOnlyLookup()
    {
        SerializerCache cache = new SerializerCache();
        JavaType type = defaultTypeFactory().constructType(String.class);
        TestSerializer serializer1 = new TestSerializer();
        TestSerializer serializer2 = new TestSerializer();

        cache.addTypedSerializer(type, serializer1);
        ReadOnlyClassToSerializerMap lookup = cache.getReadOnlyLookupMap();
        assertSame(serializer1, lookup.typedValueSerializer(type));

        cache.addTypedSerializer(type, serializer2);

        assertSame(serializer2, cache.getReadOnlyLookupMap().typedValueSerializer(type));
    }
}
