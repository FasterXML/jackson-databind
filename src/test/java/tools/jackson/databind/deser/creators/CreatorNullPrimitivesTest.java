package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.introspect.AnnotatedMember;
import tools.jackson.databind.introspect.AnnotatedParameter;
import tools.jackson.databind.introspect.JacksonAnnotationIntrospector;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class CreatorNullPrimitivesTest
{
    // [databind#2101]
    static class JsonEntity {
        protected final int x;
        protected final int y;

        @JsonCreator
        private JsonEntity(@JsonProperty("x") int x, @JsonProperty("y") int y) {
            this.x = x;
            this.y = y;
        }
    }

    static class NestedJsonEntity {
        protected final JsonEntity entity;

        @JsonCreator
        private NestedJsonEntity(@JsonProperty("entity") JsonEntity entity) {
            this.entity = entity;
        }
    }

    // [databind#988]
    static class Person {
        String name;
        Integer age;

        @JsonCreator
        public Person(@JsonProperty(value="name") String name,
                      @JsonProperty(value="age") int age)
        {
            this.name = name;
            this.age = age;
        }
    }

    // [databind#2977]
    @SuppressWarnings("serial")
    static class ABCParamIntrospector extends JacksonAnnotationIntrospector {
        @Override
        public String findImplicitPropertyName(MapperConfig<?> config, AnnotatedMember param) {
            if (param instanceof AnnotatedParameter ap) {
                switch (ap.getIndex()) {
                    case 0:
                        return "a";
                    case 1:
                        return "b";
                    case 2:
                        return "c";
                    default:
                        return "param" + ap.getIndex();
                }
            }
            return super.findImplicitPropertyName(config, param);
        }
    }

    // [databind#5734]
    record PrimitiveRecord(int int1, int int2, boolean boolean1, boolean boolean2) {
    }

    // [databind#2977]
    static class TestClass2977 {
        @JsonProperty("aa")
        final int a;

        public TestClass2977(int a) {
            this.a = a;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2101]: ensure that the property is included in the path
    @Test
    public void testCreatorNullPrimitive() throws Exception {
        final ObjectReader r = MAPPER.readerFor(JsonEntity.class)
            .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        // [databind#5734]: explicit null should fail, but absent should not
        String json = a2q("{'x': 2, 'y': null}");
        try {
            r.readValue(json);
            fail("Should not have succeeded");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot map `null` into type `int`");
            assertEquals(1, e.getPath().size());
            assertEquals("y", e.getPath().get(0).getPropertyName());
        }
    }

    @Test
    public void testCreatorNullPrimitiveInNestedObject() throws Exception {
        final ObjectReader r = MAPPER.readerFor(NestedJsonEntity.class)
                .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        // [databind#5734]: explicit null should fail, but absent should not
        String json = a2q("{ 'entity': {'x': 2, 'y': null}}");
        try {
            r.readValue(json);
            fail("Should not have succeeded");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot map `null` into type `int`");
            assertEquals(2, e.getPath().size());
            assertEquals("y", e.getPath().get(1).getPropertyName());
            assertEquals("entity", e.getPath().get(0).getPropertyName());
        }
    }

    // [databind#5734]: absent primitive creator properties should get JVM defaults
    @Test
    public void testCreatorAbsentPrimitiveShouldDefault() throws Exception {
        final ObjectReader r = MAPPER.readerFor(JsonEntity.class)
            .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        // Missing "y" should default to 0, not fail
        String json = a2q("{'x': 2}");
        JsonEntity result = r.readValue(json);
        assertEquals(2, result.x);
        assertEquals(0, result.y);
    }

    // [databind#5734]: absent primitive creator properties in nested objects
    @Test
    public void testCreatorAbsentPrimitiveInNestedObjectShouldDefault() throws Exception {
        final ObjectReader r = MAPPER.readerFor(NestedJsonEntity.class)
                .with(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        // Missing "y" in nested entity should default to 0, not fail
        String json = a2q("{ 'entity': {'x': 2}}");
        NestedJsonEntity result = r.readValue(json);
        assertEquals(2, result.entity.x);
        assertEquals(0, result.entity.y);
    }

    // [databind#988]
    @Test
    public void testRequiredNonNullParam() throws Exception
    {
        final ObjectReader personReader = MAPPER
                .readerFor(Person.class)
                .without(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);

        Person p;
        // First: fine if feature is not enabled
        p = personReader.readValue(a2q("{}"));
        assertEquals(null, p.name);
        assertEquals(Integer.valueOf(0), p.age);

        // Second: fine if feature is enabled but default value is not null
        ObjectReader r = personReader.with(DeserializationFeature.FAIL_ON_NULL_CREATOR_PROPERTIES);
        p = r.readValue(a2q("{'name':'John', 'age': null}"));
        assertEquals("John", p.name);
        assertEquals(Integer.valueOf(0), p.age);

        // Third: throws exception if property is missing
        try {
            r.readValue(a2q("{}"));
            fail("Should not pass third test");
        } catch (MismatchedInputException e) {
            verifyException(e, "Null value for creator property 'name'");
        }

        // Fourth: throws exception if property is set to null explicitly
        try {
            r.readValue(a2q("{'age': 5, 'name': null}"));
            fail("Should not pass fourth test");
        } catch (MismatchedInputException e) {
            verifyException(e, "Null value for creator property 'name'");
        }
    }

    // [databind#2977]
    @Test
    void defaultingWithNull2977() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .annotationIntrospector(new ABCParamIntrospector())
                .constructorDetector(ConstructorDetector.USE_PROPERTIES_BASED)
                .build();
        TestClass2977 result = mapper.readValue(a2q("{'aa': 8}"), TestClass2977.class);
        assertEquals(8, result.a);
    }

    // [databind#5734]: record with absent primitives should use JVM defaults
    @Test
    void testRecordAbsentPrimitivesShouldDefault() throws Exception {
        // FAIL_ON_NULL_FOR_PRIMITIVES is enabled by default in 3.x;
        // absent values should still get JVM defaults
        String json = a2q("{'int2': 42, 'boolean1': true}");
        PrimitiveRecord result = MAPPER.readValue(json, PrimitiveRecord.class);
        assertEquals(0, result.int1());
        assertEquals(42, result.int2());
        assertEquals(true, result.boolean1());
        assertEquals(false, result.boolean2());
    }

    // [databind#5734]: record with explicit null primitives should still fail
    @Test
    void testRecordExplicitNullPrimitiveShouldFail() throws Exception {
        String json = a2q("{'int1': 111, 'int2': 222, 'boolean1': true, 'boolean2': null}");
        try {
            MAPPER.readValue(json, PrimitiveRecord.class);
            fail("Should not have succeeded");
        } catch (MismatchedInputException e) {
            verifyException(e, "Cannot map `null` into type `boolean`");
        }
    }
}
