package tools.jackson.databind.node;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.Fail.fail;

// [databind#5309] Confusing exception for DoubleNode to Integer conversion in Jackson 3 #5309
public class DoubleNodeToIntExceptionMessage5309Test
    extends DatabindTestUtil
{
    @Test
    public void exceptionMessageTest()
    {
        ObjectMapper mapper = JsonMapper.builder().build();
        JsonNode jsonNode = mapper.valueToTree(2.718);

        try {
            Integer a = mapper.treeToValue(jsonNode, Integer.class);
            fail("Should have thrown an exception");
        } catch (Exception e) {
            Assertions.assertTrue(e.getMessage().contains(String.format("Failed to parse Int value from '%s' of type", "2.718")));
        }

    }
}
