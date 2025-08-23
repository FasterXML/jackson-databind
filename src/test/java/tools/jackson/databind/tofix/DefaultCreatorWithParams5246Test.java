package tools.jackson.databind.tofix;

import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

// [databind#5246] Increased need for @JsonCreator with Jackson 3
public class DefaultCreatorWithParams5246Test
    extends DatabindTestUtil
{
//    @JacksonTestFailureExpected
    @Test
    public void deserWithParams() throws Exception {
        String json = "{\"productId\":5, \"name\":\"test\", \"weight\":42}";

        JsonMapper mapper = JsonMapper.builder().build();

        Assertions.assertEquals(5, mapper.readValue(json, Pojo.class).getProductId());
    }

    public static class Pojo {
        private final int productId;
        private final String name;
        private final int weight;

        // Option 1 : Either remove this... or
        public Pojo() {
            throw new RuntimeException("Default constructor should not be used");
        }

        // Option 2 : Add @JsonCreator here
        // @JsonCreator
        public Pojo(int productId, String name, int weight) {
            this.productId = productId;
            this.name = name;
            this.weight = weight;
        }

        public int getProductId() {return this.productId;}
        public String getName() {return this.name;}
        public int getWeight() {return this.weight;}

    }
}
