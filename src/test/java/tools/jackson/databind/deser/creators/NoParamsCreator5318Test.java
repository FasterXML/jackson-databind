package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import tools.jackson.databind.*;
import tools.jackson.databind.cfg.ConstructorDetector;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

// For [databind#5318]: allow auto-detection of Properties-based Constructor
// even with class having Default (no-parameters) Constructor
public class NoParamsCreator5318Test extends DatabindTestUtil
{
    static class Pojo5318Working {
         final int productId;
         final String name;

         public Pojo5318Working() {
              this(0, null);
         }

         public Pojo5318Working(int productId, String name) {
              this.productId = productId;
              this.name = name;
         }
    }

    // No auto-detection, due to explicit annotation for 0-params ctor
    static class Pojo5318Annotated {
        @JsonCreator
        public Pojo5318Annotated() { }

        public Pojo5318Annotated(int productId, String name) {
            throw new IllegalStateException("Should not be called");  
        }
    }

    // No auto-detection, due to explicit ignoral of 0-params ctor
    static class Pojo5318Ignore {
        protected Pojo5318Ignore() { }

        @JsonIgnore
        public Pojo5318Ignore(int productId, String name) {
            throw new IllegalStateException("Should not be called");  
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
            .build();
    
    // For [databind#5318]: intended usage
    @Test
    void creatorDetectionWithNoParamsCtor() throws Exception {
        Pojo5318Working pojo = MAPPER.readValue("{\"productId\":1,\"name\":\"foo\"}", Pojo5318Working.class);
        assertEquals(1, pojo.productId);
        assertEquals("foo", pojo.name);
    }

    // For [databind#5318]: avoid detection with explicit annotation on 0-params ctor
    @Test
    void noCreatorDetectionDueToCreatorAnnotation() throws Exception {
        assertNotNull(MAPPER.readValue("{}", Pojo5318Annotated.class));
    }

    // For [databind#5318]: avoid detection with explicit ignoral of parameterized ctor
    @Test
    void noCreatorDetectionDueToIgnore() throws Exception {
        assertNotNull(MAPPER.readValue("{}", Pojo5318Ignore.class));
    }

    // For [databind#5318]: avoid detection when configured to do so (not allow
    // implicit with default ctor)
    @Test
    void noCreatorDetectionDueToFeatureDisabled() throws Exception {
        final ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                .constructorDetector(ConstructorDetector.DEFAULT
                        .withAllowImplicitWithDefaultConstructor(false))
                .build();
        try {
            mapper.readValue("{\"productId\": 1}", Pojo5318Working.class);
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Unrecognized property \"productId\"");
        }
    }
}
