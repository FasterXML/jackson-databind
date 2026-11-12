package tools.jackson.databind.ser;

import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.core.JsonGenerator;
import tools.jackson.core.TokenStreamFactory;
import tools.jackson.databind.*;
import tools.jackson.databind.cfg.GeneratorSettings;
import tools.jackson.databind.cfg.SerializationContexts;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

class JsonValueScopedCacheTest extends DatabindTestUtil
{
    @JsonPropertyOrder({"keep", "drop"})
    static class Payload {
        public int keep = 1;
        public int drop = 2;
    }

    static class Alternate {
        public int keep = 3;
        public int drop = 4;
    }

    static class Wrapped {
        private final Object value;

        Wrapped(Object value) {
            this.value = value;
        }

        @JsonValue
        @JsonIgnoreProperties("drop")
        public Object value() {
            return value;
        }
    }

    static class Holder {
        public Wrapped value;

        Holder(Object value) {
            this.value = new Wrapped(value);
        }
    }

    static class Pair {
        @JsonIgnoreProperties("keep")
        public Wrapped first = new Wrapped(new Payload());
        public Wrapped second = new Wrapped(new Payload());
    }

    static class GenericWrapped<T> {
        @JsonValue
        public List<T> values;

        GenericWrapped(List<T> values) {
            this.values = values;
        }
    }

    static class GenericHolder {
        public GenericWrapped<Payload> value = new GenericWrapped<>(List.of(new Payload()));
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonTypeName("wrapped")
    static class TypedWrapped extends Wrapped {
        TypedWrapped(Object value) {
            super(value);
        }
    }

    static class EmptyHolder {
        @JsonInclude(JsonInclude.Include.NON_EMPTY)
        public Wrapped value = new Wrapped(new Payload());
    }

    static class DateWrapped {
        @JsonValue
        @JsonFormat(pattern = "HH:mm MMM")
        public Object value() {
            return new Date(0L);
        }
    }

    static class DateHolder {
        public DateWrapped value = new DateWrapped();
    }

    static class AttributeSerializer extends ValueSerializer<Payload> {
        private final Object captured;

        AttributeSerializer() {
            this(null);
        }

        AttributeSerializer(Object captured) {
            this.captured = captured;
        }

        @Override
        public ValueSerializer<?> createContextual(SerializationContext ctxt,
                BeanProperty property) {
            return new AttributeSerializer(ctxt.getAttribute("label"));
        }

        @Override
        public void serialize(Payload value, JsonGenerator gen, SerializationContext ctxt) {
            gen.writeString(String.valueOf(captured));
        }
    }

    static class ProbeContexts extends SerializationContexts.DefaultImpl {
        final AtomicInteger lookups;
        final AtomicInteger calls;
        final boolean attributes;

        ProbeContexts(boolean attributes) {
            this(null, null, null, new AtomicInteger(), new AtomicInteger(), attributes);
        }

        ProbeContexts(TokenStreamFactory factory, SerializerFactory serializers,
                SerializerCache cache, AtomicInteger lookups, AtomicInteger calls,
                boolean attributes) {
            super(factory, serializers, cache);
            this.lookups = lookups;
            this.calls = calls;
            this.attributes = attributes;
        }

        @Override
        public SerializationContexts forMapper(Object mapper, TokenStreamFactory factory,
                SerializerFactory serializers, SerializerCache cache) {
            return new ProbeContexts(factory, serializers, cache, lookups, calls, attributes);
        }

        @Override
        public SerializationContextExt createContext(SerializationConfig config,
                GeneratorSettings settings) {
            SerializationContextExt ctxt = new SerializationContextExt(
                    _streamFactory, config, settings, _serializerFactory, _cache) {
                @Override
                public ValueSerializer<Object> findContentValueSerializer(Class<?> type,
                        BeanProperty property) {
                    lookups.incrementAndGet();
                    return super.findContentValueSerializer(type, property);
                }

                @Override
                public ValueSerializer<Object> findContentValueSerializer(JavaType type,
                        BeanProperty property) {
                    lookups.incrementAndGet();
                    return super.findContentValueSerializer(type, property);
                }
            };
            if (attributes) {
                ctxt.setAttribute("label", "call" + calls.incrementAndGet());
            }
            return ctxt;
        }
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void reusesFilteredDelegate(boolean map) {
        ProbeContexts probe = new ProbeContexts(false);
        ObjectMapper mapper = jsonMapperBuilder().serializationContexts(probe).build();
        ObjectWriter writer = mapper.writerFor(Holder.class);
        Holder holder = new Holder(map ? Map.of("keep", 1, "drop", 2) : new Payload());
        assertEquals("{\"value\":{\"keep\":1}}", writer.writeValueAsString(holder));
        int first = probe.lookups.get();
        assertTrue(first > 0);
        assertEquals("{\"value\":{\"keep\":1}}", writer.writeValueAsString(holder));
        assertEquals(first, probe.lookups.get());
    }

    @Test
    void separatesPropertiesAndRuntimeTypes() {
        ObjectMapper mapper = newJsonMapper();
        ObjectWriter writer = mapper.writerFor(Holder.class);
        assertEquals("{\"value\":{\"keep\":1}}",
                writer.writeValueAsString(new Holder(new Payload())));
        assertEquals("{\"value\":{\"keep\":3}}",
                writer.writeValueAsString(new Holder(new Alternate())));
        assertEquals("{\"value\":{\"keep\":1}}",
                writer.writeValueAsString(new Holder(new Payload())));
        ObjectWriter pair = mapper.writerFor(Pair.class);
        assertEquals("{\"first\":{},\"second\":{\"keep\":1}}",
                pair.writeValueAsString(new Pair()));
        assertEquals("{\"first\":{},\"second\":{\"keep\":1}}",
                pair.writeValueAsString(new Pair()));
    }

    @Test
    void preservesGenericValuesAndNull() {
        ObjectMapper mapper = newJsonMapper();
        ObjectWriter writer = mapper.writerFor(GenericHolder.class);
        String expected = "{\"value\":[{\"keep\":1,\"drop\":2}]}";
        assertEquals(expected, writer.writeValueAsString(new GenericHolder()));
        assertEquals(expected, writer.writeValueAsString(new GenericHolder()));
        assertEquals("{\"value\":null}", mapper.writeValueAsString(new Holder(null)));
    }

    @Test
    void reusesDelegateBetweenEmptyCheckAndWrite() {
        ProbeContexts probe = new ProbeContexts(false);
        ObjectMapper mapper = jsonMapperBuilder().serializationContexts(probe).build();
        ObjectWriter writer = mapper.writerFor(EmptyHolder.class);
        assertEquals("{\"value\":{\"keep\":1}}", writer.writeValueAsString(new EmptyHolder()));
        int first = probe.lookups.get();
        assertEquals(1, first);
        assertEquals("{\"value\":{\"keep\":1}}", writer.writeValueAsString(new EmptyHolder()));
        assertEquals(first, probe.lookups.get());
    }

    @Test
    void preservesTypedAndUntypedOutput() {
        ObjectMapper mapper = newJsonMapper();
        ObjectWriter plain = mapper.writerFor(Holder.class);
        ObjectWriter typed = mapper.writerFor(TypedWrapped.class);
        for (int i = 0; i < 2; ++i) {
            assertEquals("{\"value\":{\"keep\":1}}",
                    plain.writeValueAsString(new Holder(new Payload())));
            assertEquals("[\"wrapped\",{\"keep\":1}]",
                    typed.writeValueAsString(new TypedWrapped(new Payload())));
        }
    }

    @Test
    void invalidatesAcrossWriterConfigurations() {
        ProbeContexts probe = new ProbeContexts(false);
        ObjectMapper mapper = jsonMapperBuilder().serializationContexts(probe).build();
        ObjectWriter first = mapper.writerFor(Holder.class);
        ObjectWriter second = first.with(Locale.FRANCE);
        Holder holder = new Holder(new Payload());
        assertEquals("{\"value\":{\"keep\":1}}", first.writeValueAsString(holder));
        int count = probe.lookups.get();
        assertEquals("{\"value\":{\"keep\":1}}", second.writeValueAsString(holder));
        assertTrue(probe.lookups.get() > count);
        count = probe.lookups.get();
        assertEquals("{\"value\":{\"keep\":1}}", second.writeValueAsString(holder));
        assertEquals(count, probe.lookups.get());
        assertEquals("{\"value\":{\"keep\":1}}", first.writeValueAsString(holder));
        assertTrue(probe.lookups.get() > count);
    }

    @Test
    void preservesWriterTimeZoneAndLocale() {
        ObjectWriter base = newJsonMapper().writerFor(DateHolder.class);
        ObjectWriter utc = base.with(TimeZone.getTimeZone("UTC")).with(Locale.US);
        ObjectWriter tokyo = base.with(TimeZone.getTimeZone("Asia/Tokyo")).with(Locale.FRANCE);
        assertEquals("{\"value\":\"00:00 Jan\"}", utc.writeValueAsString(new DateHolder()));
        assertEquals("{\"value\":\"09:00 janv.\"}", tokyo.writeValueAsString(new DateHolder()));
        assertEquals("{\"value\":\"00:00 Jan\"}", utc.writeValueAsString(new DateHolder()));
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void preservesCustomSerializerAttributes(boolean perCall) {
        SimpleModule module = new SimpleModule().addSerializer(Payload.class,
                new AttributeSerializer());
        ProbeContexts probe = new ProbeContexts(perCall);
        ObjectMapper mapper = jsonMapperBuilder().addModule(module)
                .serializationContexts(probe).build();
        ObjectWriter first = mapper.writerFor(Holder.class).withAttribute("label", "first");
        ObjectWriter second = first.withAttribute("label", "second");
        probe.calls.set(0);
        assertEquals("{\"value\":\"" + (perCall ? "call1" : "first") + "\"}",
                first.writeValueAsString(new Holder(new Payload())));
        assertEquals("{\"value\":\"" + (perCall ? "call2" : "second") + "\"}",
                second.writeValueAsString(new Holder(new Payload())));
        assertEquals("{\"value\":\"" + (perCall ? "call3" : "first") + "\"}",
                first.writeValueAsString(new Holder(new Payload())));
    }

    static class TypedHolder {
        public TypedWrapped value = new TypedWrapped(new Payload());
    }

    @Test
    void reusesTypedDelegate() {
        ProbeContexts probe = new ProbeContexts(false);
        ObjectWriter writer = jsonMapperBuilder().serializationContexts(probe).build()
                .writerFor(TypedHolder.class);
        String expected = "{\"value\":[\"wrapped\",{\"keep\":1}]}";
        assertEquals(expected, writer.writeValueAsString(new TypedHolder()));
        int first = probe.lookups.get();
        assertTrue(first > 0);
        assertEquals(expected, writer.writeValueAsString(new TypedHolder()));
        assertEquals(first, probe.lookups.get());
    }

    @Test
    void bypassesCacheWithPerCallAttributes() {
        ProbeContexts probe = new ProbeContexts(true);
        ObjectWriter writer = jsonMapperBuilder().serializationContexts(probe).build()
                .writerFor(Holder.class);
        Holder holder = new Holder(new Payload());
        assertEquals("{\"value\":{\"keep\":1}}", writer.writeValueAsString(holder));
        int first = probe.lookups.get();
        assertEquals("{\"value\":{\"keep\":1}}", writer.writeValueAsString(holder));
        assertTrue(probe.lookups.get() > first);
    }

    @Test
    void preservesPerCallAttributesInsideMap() {
        ProbeContexts probe = new ProbeContexts(true);
        SimpleModule module = new SimpleModule().addSerializer(Payload.class,
                new AttributeSerializer());
        ObjectWriter writer = jsonMapperBuilder().addModule(module)
                .serializationContexts(probe).build().writerFor(Holder.class);
        probe.calls.set(0);
        Holder holder = new Holder(Map.of("keep", new Payload()));
        assertEquals("{\"value\":{\"keep\":\"call1\"}}", writer.writeValueAsString(holder));
        assertEquals("{\"value\":{\"keep\":\"call2\"}}", writer.writeValueAsString(holder));
    }

    record A(int keep, int drop) { }
    record B(int keep, int drop) { }
    record C(int keep, int drop) { }
    record D(int keep, int drop) { }
    record E(int keep, int drop) { }
    record F(int keep, int drop) { }
    record G(int keep, int drop) { }
    record H(int keep, int drop) { }
    record I(int keep, int drop) { }

    @Test
    void retainsCorrectnessBeyondCacheCapacity() {
        ObjectWriter writer = newJsonMapper().writerFor(Holder.class);
        List<Object> values = List.of(new A(1, 2), new B(1, 2), new C(1, 2), new D(1, 2),
                new E(1, 2), new F(1, 2), new G(1, 2), new H(1, 2), new I(1, 2));
        for (int pass = 0; pass < 2; ++pass) {
            for (Object value : values) {
                assertEquals("{\"value\":{\"keep\":1}}",
                        writer.writeValueAsString(new Holder(value)));
            }
        }
    }
}
