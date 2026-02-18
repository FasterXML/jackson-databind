package tools.jackson.databind.records;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
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
}
