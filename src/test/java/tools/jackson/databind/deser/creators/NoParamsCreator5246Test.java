package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class NoParamsCreator5246Test extends DatabindTestUtil
{
    static class Pojo5246Ok {
         final int productId;
         final String name;

         public Pojo5246Ok() {
              this(0, null);
         }

         public Pojo5246Ok(int productId, String name) {
              this.productId = productId;
              this.name = name;
         }
    }

    // No auto-detection, due to explicit annotation for 0-params ctor
    static class Pojo5246Annotated {
        @JsonCreator
        public Pojo5246Annotated() { }

        public Pojo5246Annotated(int productId, String name) {
            throw new IllegalStateException("Should not be called");  
        }
    }

    // No auto-detection, due to explicit ignoral of 0-params ctor
    static class Pojo5246Ignore {
        protected Pojo5246Ignore() { }

        @JsonIgnore
        public Pojo5246Ignore(int productId, String name) {
            throw new IllegalStateException("Should not be called");  
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
            .build();
    
    // For [databind#5246]: intended usage
    @Test
    void creatorDetectionWithNoParamsCtor() throws Exception {
        Pojo5246Ok pojo = MAPPER.readValue("{\"productId\":1,\"name\":\"foo\"}", Pojo5246Ok.class);
        assertEquals(1, pojo.productId);
        assertEquals("foo", pojo.name);
    }

    // For [databind#5246]: avoid detection with 2 alternatives
    @Test
    void noCreatorDetectionDueToCreatorAnnotation() throws Exception {
        assertNotNull(MAPPER.readValue("{}", Pojo5246Annotated.class));
    }

    @Test
    void noCreatorDetectionDueToIgnore() throws Exception {
        assertNotNull(MAPPER.readValue("{}", Pojo5246Ignore.class));
    }
}
