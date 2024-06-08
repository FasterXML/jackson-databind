package com.fasterxml.jackson.databind.records;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonAutoDetect.Visibility;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.introspect.AnnotatedClass;
import com.fasterxml.jackson.databind.introspect.NopAnnotationIntrospector;
import com.fasterxml.jackson.databind.introspect.VisibilityChecker;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test case that covers both failing-by-regression tests and passing tests.
 * <p>For more details, refer to
 * <a href="https://github.com/FasterXML/jackson-databind/issues/3906">
 * [databind#3906]: Regression: 2.15.0 breaks deserialization for records when mapper.setVisibility(ALL, NONE);</a>
 */
public class RecordDeserialization3906Test extends DatabindTestUtil
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
    /* Failing tests that pass in 2.14 (regression)
    /**********************************************************
     */

    // minimal config for reproduction
    @Test
    public void testEmptyJsonToRecordMiminal() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }

    // actual config used reproduction
    @Test
    public void testEmptyJsonToRecordActualImpl() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.FIELD, Visibility.ANY);

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }

    /*
    /**********************************************************
    /* Passing Tests : Suggested work-arounds
    /* for future modifications
    /**********************************************************
     */

    @Test
    public void testEmptyJsonToRecordWorkAround() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();
        mapper.setVisibility(PropertyAccessor.ALL, Visibility.NONE);
        mapper.setVisibility(PropertyAccessor.CREATOR, Visibility.ANY);

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);

        assertEquals(new Record3906(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordCreatorsVisibile() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();
        mapper.setVisibility(PropertyAccessor.CREATOR, Visibility.NON_PRIVATE);

        Record3906 recordDeser = mapper.readValue("{}", Record3906.class);
        assertEquals(new Record3906(null, 0), recordDeser);
    }

    @SuppressWarnings("serial")
    @Test
    public void testEmptyJsonToRecordUsingModule() throws JsonProcessingException {
        ObjectMapper mapper = jsonMapperBuilder().addModule(new SimpleModule() {
            @Override
            public void setupModule(SetupContext context) {
                super.setupModule(context);
                context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                    @Override
                    public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
                                                                         VisibilityChecker<?> checker) {
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

    @Test
    public void testEmptyJsonToRecordDirectAutoDetectConfig() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();

        Record3906Annotated recordDeser = mapper.readValue("{}", Record3906Annotated.class);
        assertEquals(new Record3906Annotated(null, 0), recordDeser);
    }

    @Test
    public void testEmptyJsonToRecordJsonCreator() throws JsonProcessingException {
        ObjectMapper mapper = newJsonMapper();

        Record3906Creator recordDeser = mapper.readValue("{}", Record3906Creator.class);
        assertEquals(new Record3906Creator(null, 0), recordDeser);
    }

    @SuppressWarnings("serial")
    @Test
    public void testEmptyJsonToRecordUsingModuleOther() throws JsonProcessingException {
        ObjectMapper mapper = jsonMapperBuilder().addModule(
                new SimpleModule() {
                    @Override
                    public void setupModule(SetupContext context) {
                        super.setupModule(context);
                        context.insertAnnotationIntrospector(new NopAnnotationIntrospector() {
                            @Override
                            public VisibilityChecker<?> findAutoDetectVisibility(AnnotatedClass ac,
                                                                                 VisibilityChecker<?> checker) {
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
