package tools.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;

import tools.jackson.databind.BaseMapTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.cfg.MapperConfig;
import tools.jackson.databind.introspect.AnnotatedClass;
import tools.jackson.databind.introspect.NopAnnotationIntrospector;
import tools.jackson.databind.introspect.VisibilityChecker;
import tools.jackson.databind.module.SimpleModule;

/**
 * Test case that covers both failing-by-regression tests and passing tests.
 * <p>For more details, refer to
 * <a href="https://github.com/FasterXML/jackson-databind/issues/3906">
 * [databind#3906]: Regression: 2.15.0 breaks deserialization for records when mapper.setVisibility(ALL, NONE);</a>
 */
@SuppressWarnings("serial")
public class RecordDeserialization3906Test extends BaseMapTest
{
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
    /**********************************************************
    /* Failing tests that pass in 2.x
    /**********************************************************
     */

    // Forked to test class under "failing/" for 3.0

    /*
    /**********************************************************
    /* Passing Tests : Suggested work-arounds
    /* for future modifications
    /**********************************************************
     */

    public void testEmptyJsonToRecordWorkAround() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> 
                   vc.withVisibility(PropertyAccessor.ALL, Visibility.NONE)
                   .withVisibility(PropertyAccessor.CREATOR, Visibility.ANY))
                .build();
        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }

    public void testEmptyJsonToRecordCreatorsVisibile() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .changeDefaultVisibility(vc -> 
                   vc.withVisibility(PropertyAccessor.CREATOR, Visibility.NON_PRIVATE))
                .build();

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);
        assertEquals(new Record3906(null, 0), recordDeser);
    }

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
        }).build();

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);
        assertEquals(new Record3906(null, 0), recordDeser);
    }

    public void testEmptyJsonToRecordDirectAutoDetectConfig() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        Record3906Annotated recordDeser = mapper.readValue("{}", Record3906Annotated.class);
        assertEquals(new Record3906Annotated(null, 0), recordDeser);
    }

    public void testEmptyJsonToRecordJsonCreator() throws Exception {
        ObjectMapper mapper = newJsonMapper();

        Record3906Creator recordDeser = mapper.readValue("{}", Record3906Creator.class);
        assertEquals(new Record3906Creator(null, 0), recordDeser);
    }

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
                                // If this is a Record, then increase the "creator" visibility again
                                return checker.withCreatorVisibility(Visibility.ANY);
                            }
                        });
                    }
                })
                .build();

        assertEquals(new Record3906(null, 0),
                mapper.readValue("{}", Record3906.class));
        assertEquals(new PrivateRecord3906(null, 0),
                mapper.readValue("{}", PrivateRecord3906.class));
    }
}
