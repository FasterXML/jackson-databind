package tools.jackson.databind.deser.creators;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class NoParamsCreator5246Test extends DatabindTestUtil
{
    static class Pojo5246 {
         final int productId;
         final String name;

         public Pojo5246() {
              this(0, null);
         }

         public Pojo5246(int productId, String name) {
              this.productId = productId;
              this.name = name;
         }
    }

    // For [databind#5246]
    @Test
    void testNoParamsCreator() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, true)
                .build();
        Pojo5246 pojo = mapper.readValue("{\"productId\":1,\"name\":\"foo\"}", Pojo5246.class);
        assertEquals(1, pojo.productId);
        assertEquals("foo", pojo.name);
    }
}
