package tools.jackson.databind.ser;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.Annotated;
import tools.jackson.databind.introspect.AnnotatedField;
import tools.jackson.databind.introspect.AnnotatedMethod;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JsonValueEnclosingPropertyFilterTest extends DatabindTestUtil
{
    static final class Payload {
        public int keep = 1;
        public int drop = 2;
    }

    static class DynamicValue {
        private final Object _value;

        DynamicValue(Object value) {
            this._value = value;
        }

        @JsonValue
        public Object value() {
            return _value;
        }
    }

    static class StaticValue {
        @JsonValue
        public Payload value() {
            return new Payload();
        }
    }

    static class IgnoreDynamicField {
        @JsonIgnoreProperties("drop")
        public DynamicValue value = new DynamicValue(new Payload());
    }

    static class IncludeDynamicField {
        @JsonIncludeProperties("keep")
        public DynamicValue value = new DynamicValue(new Payload());
    }

    static class IgnoreStaticField {
        @JsonIgnoreProperties("drop")
        public StaticValue value = new StaticValue();
    }

    static class IncludeStaticField {
        @JsonIncludeProperties("keep")
        public StaticValue value = new StaticValue();
    }

    static class IgnoreDynamicGetter {
        @JsonIgnoreProperties("drop")
        public DynamicValue getValue() {
            return new DynamicValue(new Payload());
        }
    }

    static class IncludeDynamicGetter {
        @JsonIncludeProperties("keep")
        public DynamicValue getValue() {
            return new DynamicValue(new Payload());
        }
    }

    static class IgnoreStaticGetter {
        @JsonIgnoreProperties("drop")
        public StaticValue getValue() {
            return new StaticValue();
        }
    }

    static class IncludeStaticGetter {
        @JsonIncludeProperties("keep")
        public StaticValue getValue() {
            return new StaticValue();
        }
    }

    static class PlainIgnore {
        @JsonIgnoreProperties("drop")
        public Payload value = new Payload();
    }

    static class PlainInclude {
        @JsonIncludeProperties("keep")
        public Payload value = new Payload();
    }

    static class AccessorIgnore extends DynamicValue {
        AccessorIgnore(Object value) {
            super(value);
        }

        @Override
        @JsonIgnoreProperties("drop")
        public Object value() {
            return super.value();
        }
    }

    static class AccessorInclude extends DynamicValue {
        AccessorInclude(Object value) {
            super(value);
        }

        @Override
        @JsonIncludeProperties("keep")
        public Object value() {
            return super.value();
        }
    }

    static class IgnoreKeep {
        @JsonIgnoreProperties("keep")
        public Object value;

        IgnoreKeep(Object value) {
            this.value = value;
        }
    }

    static class IncludeDrop {
        @JsonIncludeProperties("drop")
        public Object value;

        IncludeDrop(Object value) {
            this.value = value;
        }
    }

    static class AllowKeep {
        @JsonIgnoreProperties(value = "keep", allowGetters = true)
        public Object value;

        AllowKeep(Object value) {
            this.value = value;
        }
    }

    static class AccessorAllowDrop extends DynamicValue {
        AccessorAllowDrop(Object value) {
            super(value);
        }

        @Override
        @JsonIgnoreProperties(value = "drop", allowGetters = true)
        public Object value() {
            return super.value();
        }
    }

    static class ThreeProperties {
        public int keep = 1;
        public int drop = 2;
        public int other = 3;
    }

    static class PartialInclude extends DynamicValue {
        PartialInclude(Object value) {
            super(value);
        }

        @Override
        @JsonIncludeProperties({"keep", "drop"})
        public Object value() {
            return super.value();
        }
    }

    static class IncludeKeepOther {
        @JsonIncludeProperties({"keep", "other"})
        public Object value;

        IncludeKeepOther(Object value) {
            this.value = value;
        }
    }

    @JsonPropertyOrder({"keep", "drop"})
    static class IgnorePair {
        @JsonIgnoreProperties("drop")
        public Object keep;
        @JsonIgnoreProperties("keep")
        public Object drop;

        IgnorePair(Object value) {
            keep = drop = value;
        }
    }

    @JsonPropertyOrder({"keep", "drop"})
    static class IncludePair {
        @JsonIncludeProperties("keep")
        public Object keep;
        @JsonIncludeProperties("drop")
        public Object drop;

        IncludePair(Object value) {
            keep = drop = value;
        }
    }

    @JsonPropertyOrder({"drop", "keep"})
    static class ReverseOrder { }

    static class ListValue {
        @JsonValue
        public List<Payload> values = List.of(new Payload());
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.WRAPPER_ARRAY)
    @JsonTypeName("wrapped")
    static class TypedValue extends DynamicValue {
        TypedValue(Object value) {
            super(value);
        }
    }

    static class TypedIgnoreHolder {
        @JsonIgnoreProperties("keep")
        public TypedValue value = new TypedValue(new Payload());
    }

    static class TypedIncludeHolder {
        @JsonIncludeProperties("drop")
        public TypedValue value = new TypedValue(new Payload());
    }

    static class IntrospectedHolder {
        public DynamicValue value = new DynamicValue(new ThreeProperties());
    }

    static class FilterIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public JsonIgnoreProperties.Value findPropertyIgnoralByName(
                MapperConfig<?> config, Annotated member) {
            if (member instanceof AnnotatedField field
                    && field.getDeclaringClass() == IntrospectedHolder.class
                    && field.getMember().getName().equals("value")) {
                return JsonIgnoreProperties.Value.forIgnoredProperties("drop");
            }
            if (member instanceof AnnotatedMethod method
                    && method.getDeclaringClass() == DynamicValue.class
                    && method.getMember().getName().equals("value")) {
                return JsonIgnoreProperties.Value.forIgnoredProperties("other");
            }
            return super.findPropertyIgnoralByName(config, member);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    static Stream<Object> enclosingProperties() {
        return Stream.of(new IgnoreDynamicField(), new IncludeDynamicField(),
                new IgnoreStaticField(), new IncludeStaticField(),
                new IgnoreDynamicGetter(), new IncludeDynamicGetter(),
                new IgnoreStaticGetter(), new IncludeStaticGetter());
    }

    @ParameterizedTest
    @MethodSource("enclosingProperties")
    void filtersEnclosingProperty(Object input) {
        _assertJson("""
                {"value":{"keep":1}}
                """, input);
    }

    static Stream<Object> plainProperties() {
        return Stream.of(new PlainIgnore(), new PlainInclude());
    }

    @ParameterizedTest
    @MethodSource("plainProperties")
    void preservesPlainPropertyFilters(Object input) {
        _assertJson("""
                {"value":{"keep":1}}
                """, input);
    }

    static Stream<Object> payloads() {
        return Stream.of(new Payload(), Map.of("keep", 1, "drop", 2));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void preservesAccessorFilters(Object payload) {
        _assertJson("""
                {"keep":1}
                """, new AccessorIgnore(payload));
        _assertJson("""
                {"keep":1}
                """, new AccessorInclude(payload));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void unionsIgnoredNames(Object payload) {
        _assertJson("""
                {"value":{}}
                """, new IgnoreKeep(new AccessorIgnore(payload)));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void intersectsIncludedNames(Object payload) {
        _assertJson("""
                {"value":{}}
                """, new IncludeDrop(new AccessorInclude(payload)));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void keepsAllowGettersLocalToItsAnnotation(Object payload) {
        _assertJson("""
                {"value":{"keep":1}}
                """, new AllowKeep(new AccessorIgnore(payload)));
        _assertJson("""
                {"value":{"drop":2}}
                """, new IgnoreKeep(new AccessorAllowDrop(payload)));
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void filtersNestedValues(Object payload) {
        IgnoreDynamicField ignore = new IgnoreDynamicField();
        ignore.value = new DynamicValue(new DynamicValue(payload));
        IncludeDynamicField include = new IncludeDynamicField();
        include.value = new DynamicValue(new DynamicValue(payload));
        _assertJson("""
                {"value":{"keep":1}}
                """, ignore);
        _assertJson("""
                {"value":{"keep":1}}
                """, include);
    }

    @ParameterizedTest
    @MethodSource("payloads")
    void isolatesPropertiesInEitherOrder(Object payload) {
        IgnoreDynamicField ignore = new IgnoreDynamicField();
        ignore.value = new DynamicValue(payload);
        IgnoreKeep other = new IgnoreKeep(ignore.value);
        IncludeDynamicField include = new IncludeDynamicField();
        include.value = ignore.value;
        IncludeDrop otherInclude = new IncludeDrop(ignore.value);
        String keep = """
                {"value":{"keep":1}}
                """;
        String drop = """
                {"value":{"drop":2}}
                """;
        _assertJson(keep, ignore);
        _assertJson(drop, other);
        _assertJson(keep, ignore);
        _assertJson(drop, otherInclude);
        _assertJson(keep, include);
        _assertJson(drop, otherInclude);
    }

    static Stream<Object> threeProperties() {
        return Stream.of(new ThreeProperties(), Map.of("keep", 1, "drop", 2, "other", 3));
    }

    @ParameterizedTest
    @MethodSource("threeProperties")
    void preservesNamesOutsideIgnoreUnion(Object payload) {
        _assertJson("""
                {"value":{"other":3}}
                """, new IgnoreKeep(new AccessorIgnore(payload)));
    }

    @ParameterizedTest
    @MethodSource("threeProperties")
    void preservesNamesInIncludeIntersection(Object payload) {
        _assertJson("""
                {"value":{"keep":1}}
                """, new IncludeKeepOther(new PartialInclude(payload)));
    }

    static Stream<Object> valueWrappers() {
        return Stream.of(new DynamicValue(new Payload()), new StaticValue());
    }

    @ParameterizedTest
    @MethodSource("valueWrappers")
    void isolatesTwoPropertiesOnTheSameBean(Object value) {
        String expected = """
                {"keep":{"keep":1},"drop":{"drop":2}}
                """;
        ObjectMapper forward = newJsonMapper();
        ObjectMapper reverse = jsonMapperBuilder()
                .addMixIn(IgnorePair.class, ReverseOrder.class)
                .addMixIn(IncludePair.class, ReverseOrder.class)
                .build();
        Object ignore = new IgnorePair(value);
        Object include = new IncludePair(value);
        _assertJson(forward, expected, ignore);
        _assertJson(forward, expected, include);
        _assertJson(reverse, expected, include);
        _assertJson(reverse, expected, ignore);
        _assertJson(forward, expected, ignore);
        _assertJson(reverse, expected, include);
    }

    @Test
    void filtersGenericContainerFromJsonValueField() {
        _assertJson("""
                {"value":[{"drop":2}]}
                """, new IgnoreKeep(new ListValue()));
        _assertJson("""
                {"value":[{"drop":2}]}
                """, new IncludeDrop(new ListValue()));
    }

    @Test
    void filtersValuesInsideContainerProperty() {
        _assertJson("""
                {"value":[{"drop":2}]}
                """, new IgnoreKeep(List.of(new DynamicValue(new Payload()))));
        _assertJson("""
                {"value":[{"drop":2}]}
                """, new IncludeDrop(List.of(new StaticValue())));
    }

    @Test
    void filtersPolymorphicValue() {
        _assertJson("""
                {"value":["wrapped",{"drop":2}]}
                """, new TypedIgnoreHolder());
        _assertJson("""
                {"value":["wrapped",{"drop":2}]}
                """, new TypedIncludeHolder());
    }

    @Test
    void usesIntrospectorWithOriginalMembers() {
        ObjectMapper mapper = jsonMapperBuilder()
                .annotationIntrospector(new FilterIntrospector())
                .build();
        _assertJson(mapper, """
                {"value":{"keep":1}}
                """, new IntrospectedHolder());
    }

    @Test
    void preservesNulls() {
        IgnoreDynamicField ignore = new IgnoreDynamicField();
        ignore.value = new DynamicValue(null);
        IncludeDynamicField include = new IncludeDynamicField();
        include.value = new DynamicValue(null);
        String expected = """
                {"value":null}
                """;
        _assertJson(expected, ignore);
        _assertJson(expected, include);
        ignore.value = null;
        include.value = null;
        _assertJson(expected, ignore);
        _assertJson(expected, include);
    }

    private void _assertJson(String expected, Object input) {
        _assertJson(MAPPER, expected, input);
    }

    private void _assertJson(ObjectMapper mapper, String expected, Object input) {
        assertEquals(mapper.readTree(expected), mapper.readTree(mapper.writeValueAsString(input)));
    }
}
