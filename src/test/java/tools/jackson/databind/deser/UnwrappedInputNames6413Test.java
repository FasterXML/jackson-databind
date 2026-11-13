package tools.jackson.databind.deser;

import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

import tools.jackson.core.JsonParser;
import tools.jackson.databind.*;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.TypeDeserializer;
import tools.jackson.databind.jsontype.TypeIdResolver;

import static org.junit.jupiter.api.Assertions.*;

class UnwrappedInputNames6413Test
{
    private final ObjectMapper mapper = JsonMapper.builder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES).build();

    static class NamedDeserializer extends ValueDeserializer<Object> {
        private final Set<String> names;
        NamedDeserializer(String... names) {
            this.names = Set.of(names);
        }
        @Override
        public Object deserialize(JsonParser p, DeserializationContext ctxt) {
            throw new AssertionError("Only name collection is expected");
        }
        @Override
        public void collectAllPropertyNamesTo(Set<String> result) {
            result.addAll(names);
        }
    }

    static class TypeIdDeserializer extends TypeDeserializer {
        private final JsonTypeInfo.As inclusion;
        TypeIdDeserializer(JsonTypeInfo.As inclusion) {
            this.inclusion = inclusion;
        }
        @Override
        public TypeDeserializer forProperty(BeanProperty prop) { return this; }
        @Override
        public JsonTypeInfo.As getTypeInclusion() { return inclusion; }
        @Override
        public String getPropertyName() { return "kind"; }
        @Override
        public TypeIdResolver getTypeIdResolver() { return null; }
        @Override
        public Class<?> getDefaultImpl() { return null; }
        @Override
        public Object deserializeTypedFromObject(JsonParser p, DeserializationContext ctxt) {
            throw new AssertionError();
        }
        @Override
        public Object deserializeTypedFromArray(JsonParser p, DeserializationContext ctxt) {
            throw new AssertionError();
        }
        @Override
        public Object deserializeTypedFromScalar(JsonParser p, DeserializationContext ctxt) {
            throw new AssertionError();
        }
        @Override
        public Object deserializeTypedFromAny(JsonParser p, DeserializationContext ctxt) {
            throw new AssertionError();
        }
    }

    private SettableBeanProperty property(ValueDeserializer<?> deser,
            TypeDeserializer typeDeser) {
        SettableBeanProperty prop = CreatorProperty.construct(PropertyName.construct("value"),
                mapper.constructType(Object.class), null, typeDeser,
                null, null, 0, null, PropertyMetadata.STD_OPTIONAL);
        return deser == null ? prop : prop.withValueDeserializer(deser);
    }

    @Test
    void missingDeserializerDoesNotContributeNames() {
        Set<String> names = new HashSet<>(Set.of("existing"));
        assertFalse(property(null, new TypeIdDeserializer(JsonTypeInfo.As.PROPERTY))
                .collectValuePropertyNamesTo(names));
        assertEquals(Set.of("existing"), names);
    }

    @Test
    void opacityIsIndependentOfExistingNamesAndCollectionOrder() {
        SettableBeanProperty named = property(new NamedDeserializer("value", "alias"), null);
        SettableBeanProperty opaque = property(new NamedDeserializer(), null);
        for (boolean namedFirst : new boolean[] { true, false }) {
            Set<String> names = new HashSet<>(Set.of("value", "alias"));
            if (namedFirst) {
                assertFalse(named.collectValuePropertyNamesTo(names));
                assertTrue(opaque.collectValuePropertyNamesTo(names));
            } else {
                assertTrue(opaque.collectValuePropertyNamesTo(names));
                assertFalse(named.collectValuePropertyNamesTo(names));
            }
            assertEquals(Set.of("value", "alias"), names);
        }
    }

    @Test
    void onlyPropertyBasedTypeIdsContributeNamesWithoutChangingOpacity() {
        for (JsonTypeInfo.As inclusion : JsonTypeInfo.As.values()) {
            for (boolean opaque : new boolean[] { true, false }) {
                Set<String> names = new HashSet<>(Set.of("existing"));
                ValueDeserializer<?> deser = opaque ? new NamedDeserializer()
                        : new NamedDeserializer("value");
                assertEquals(opaque, property(deser, new TypeIdDeserializer(inclusion))
                        .collectValuePropertyNamesTo(names));
                Set<String> expected = new HashSet<>(Set.of("existing"));
                if (!opaque) {
                    expected.add("value");
                }
                if (inclusion == JsonTypeInfo.As.PROPERTY
                        || inclusion == JsonTypeInfo.As.EXISTING_PROPERTY) {
                    expected.add("kind");
                }
                assertEquals(expected, names, inclusion.name());
            }
        }
    }

    @Test
    void anySetterDoesNotCollectNamesOrConsultTypeDeserializer() {
        ValueDeserializer<?> deser = new NamedDeserializer("value") {
            @Override
            public boolean hasAnySetter() { return true; }
            @Override
            public void collectAllPropertyNamesTo(Set<String> names) {
                throw new AssertionError("Any-setter must short circuit");
            }
        };
        SettableBeanProperty prop = new SettableBeanProperty.Delegating(property(deser, null)) {
            @Override
            protected SettableBeanProperty withDelegate(SettableBeanProperty d) {
                throw new AssertionError();
            }
            @Override
            public TypeDeserializer getValueTypeDeserializer() {
                throw new AssertionError("Any-setter must short circuit");
            }
        };
        Set<String> names = new HashSet<>(Set.of("existing"));
        assertTrue(prop.collectValuePropertyNamesTo(names));
        assertEquals(Set.of("existing"), names);
    }

    @Test
    void decoratedPropertyUsesDeserializerAccessors() {
        SettableBeanProperty prop = new SettableBeanProperty.Delegating(
                property(new NamedDeserializer("backing"), null)) {
            @Override
            protected SettableBeanProperty withDelegate(SettableBeanProperty d) {
                throw new AssertionError();
            }
            @Override
            public ValueDeserializer<Object> getValueDeserializer() {
                return new NamedDeserializer("delegated");
            }
            @Override
            public TypeDeserializer getValueTypeDeserializer() {
                return new TypeIdDeserializer(JsonTypeInfo.As.PROPERTY);
            }
        };
        Set<String> names = new HashSet<>();
        assertFalse(prop.collectValuePropertyNamesTo(names));
        assertEquals(Set.of("delegated", "kind"), names);
    }

    static class Nested {
        public int number;
    }

    static class Leaf {
        @JsonAlias("alias")
        public String value;
        @JsonUnwrapped(prefix = "deep_")
        public Nested nested;
        public Nested regular;
    }

    static class Pair {
        @JsonUnwrapped(prefix = "left_")
        public Leaf left;
        @JsonUnwrapped(prefix = "right_")
        public Leaf right;
    }

    @Test
    void nestedUnwrappedNamesAreRoutedButOrdinaryValuesStayNested() throws Exception {
        Pair pair = mapper.readValue("""
                {"left_value":"L","left_deep_number":1,"left_regular":{"number":2},
                 "right_value":"R","right_deep_number":3,"right_regular":{"number":4}}
                """, Pair.class);
        assertEquals("L", pair.left.value);
        assertEquals(1, pair.left.nested.number);
        assertEquals(2, pair.left.regular.number);
        assertEquals("R", pair.right.value);
        assertEquals(3, pair.right.nested.number);
        assertEquals(4, pair.right.regular.number);
        assertThrows(UnrecognizedPropertyException.class,
                () -> mapper.readValue("""
                        {"left_regular_number":2}
                        """, Pair.class));
    }
}
