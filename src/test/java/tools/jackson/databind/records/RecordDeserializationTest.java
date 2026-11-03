package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;
import tools.jackson.databind.introspect.VisibilityChecker;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class RecordDeserializationTest extends DatabindTestUtil
{
    // [databind#3897]
    record ExampleWriteOnly(
        @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
        String value
    ) {}

    // [databind#3906]
    record Record3906(String string, int integer) {
    }

    @JsonAutoDetect(creatorVisibility = Visibility.NON_PRIVATE)
    record Record3906Annotated(String string, int integer) {
    }

    record Record3906Creator(String string, int integer) {
        @JsonCreator
        Record3906Creator {
        }
    }

    private record PrivateRecord3906(String string, int integer) {
    }

    // [databind#5683]
    @JsonDeserialize(using = Inner5683.Deser.class)
    public record Inner5683(@JsonValue long value) {
        static class Deser extends StdDeserializer<Inner5683> {
            protected Deser() { super(Inner5683.class); }

            @Override
            public Inner5683 deserialize(JsonParser p, DeserializationContext ctxt) {
                return new Inner5683(p.readValueAs(Long.class));
            }
        }
    }

    public record Outer5683(Inner5683 inner) { }

    // [databind#4690]
    record DuplicatePropRecord4690(String first) { }

    static class DuplicatePropPojo4690 {
        private String first;

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        DuplicatePropPojo4690(String first) { this.first = first; }

        public void setFirst(String first) {
            this.first = first;
        }

        public String getFirst() {
            return first;
        }
    }

    /*
    /**********************************************************************
    /* Test methods, WRITE_ONLY access [databind#3897]
    /**********************************************************************
     */

    // [databind#3897]
    @Test
    public void testRecordWithWriteOnly3897() throws Exception {
        final String JSON = a2q("{'value':'foo'}");

        ExampleWriteOnly result = newJsonMapper().readValue(JSON, ExampleWriteOnly.class);

        assertEquals("foo", result.value());
    }

    /*
    /**********************************************************************
    /* Test methods, visibility configuration workarounds [databind#3906]
    /**********************************************************************
     */

    // [databind#3906]
    @Test
    public void testEmptyJsonToRecordWorkAround() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .changeDefaultVisibility(vc ->
                   vc.withVisibility(PropertyAccessor.ALL, Visibility.NONE)
                   .withVisibility(PropertyAccessor.CREATOR, Visibility.ANY))
                .build();
        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordCreatorsVisible() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .changeDefaultVisibility(vc ->
                   vc.withVisibility(PropertyAccessor.CREATOR, Visibility.NON_PRIVATE))
                .build();

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);
        assertEquals(new Record3906(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordUsingModule() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().addModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                    @Override
                    public VisibilityChecker findAutoDetectVisibility(MapperConfig<?> cfg,
                            AnnotatedClass ac,
                            VisibilityChecker checker) {
                        return ac.getType().isRecordType()
                                ? checker.withCreatorVisibility(JsonAutoDetect.Visibility.NON_PRIVATE)
                                : checker;
                    }
                });
            }
        })
            .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
            .build();

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);
        assertEquals(new Record3906(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordDirectAutoDetectConfig() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        Record3906Annotated recordDeser = mapper.readValue("{}", Record3906Annotated.class);
        assertEquals(new Record3906Annotated(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordJsonCreator() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        Record3906Creator recordDeser = mapper.readValue("{}", Record3906Creator.class);
        assertEquals(new Record3906Creator(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordUsingModuleOther() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder().addModule(
                new SimpleModule() {
                    @Override
                    public void setupModule(SetupContext context) {
                        super.setupModule(context);
                        context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                            @Override
                            public VisibilityChecker findAutoDetectVisibility(MapperConfig<?> cfg,
                                    AnnotatedClass ac,
                                    VisibilityChecker checker) {
                                if (ac.getType() == null) {
                                    return checker;
                                }
                                if (!ac.getType().isRecordType()) {
                                    return checker;
                                }
                                return checker.withCreatorVisibility(Visibility.ANY);
                            }
                        });
                    }
                })
                .disable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
                .build();

        assertEquals(new Record3906(null, 0),
                mapper.readValue("{}", Record3906.class));
        assertEquals(new PrivateRecord3906(null, 0),
                mapper.readValue("{}", PrivateRecord3906.class));
    }

    /*
    /**********************************************************************
    /* Test methods, custom deserializer via JsonParser [databind#5683]
    /**********************************************************************
     */

    // [databind#5683]
    @Test
    public void testIssue5683()
    {
        final ObjectMapper mapper = newJsonMapper();
        final String json = "{\"inner\":\"123\"}";
        final JsonNode tree = mapper.readTree(json);

        Outer5683 value;

        value = mapper.readValue(json, Outer5683.class);
        assertEquals(123L, value.inner.value());

        value = mapper.treeToValue(tree, Outer5683.class);
        assertEquals(123L, value.inner.value());

        value = mapper.reader().treeToValue(tree, Outer5683.class);
        assertEquals(123L, value.inner.value());
    }

    /*
    /**********************************************************************
    /* Test methods, duplicate properties in JSON [databind#4690]
    /**********************************************************************
     */

    // [databind#4690] InvalidDefinitionException "No fallback setter/field defined
    // for creator property" when deserializing JSON with duplicated property to
    // single-property Record
    @Test
    void testDuplicatePropertyDeserialization() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'first':'value','first':'value2'}");

        DuplicatePropRecord4690 result = mapper.readValue(json, DuplicatePropRecord4690.class);

        assertNotNull(result);
        assertEquals("value2", result.first());
    }

    // [databind#4690]
    @Test
    void testDuplicatePropertyDeserialization2() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'first':'value','second':'test1','first':'value2'}");

        DuplicatePropRecord4690 result = mapper.readValue(json, DuplicatePropRecord4690.class);
        assertEquals("value2", result.first());
    }

    // [databind#4690]
    @Test
    void testDuplicatePropertyClassDeserialization() throws Exception {
        final ObjectMapper mapper = newJsonMapper();
        final String json = a2q("{'first':'value','second':'test1','first':'value2'}");

        DuplicatePropPojo4690 result = mapper.readValue(json, DuplicatePropPojo4690.class);
        assertEquals("value2", result.getFirst());
    }
}
