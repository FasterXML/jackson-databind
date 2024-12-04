package tools.jackson.databind.node;

import org.junit.jupiter.api.Test;

import tools.jackson.core.JsonPointer;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

// For [core#1361] (not databind)
public class JsonPointerCore1361Test extends DatabindTestUtil
{
    @Test
    public void test1361() throws Exception
    {
        ObjectMapper om = newJsonMapper();
        ObjectNode jo = om.createObjectNode();
        ArrayNode nums = om.createArrayNode().add(42).add(-99);
        jo.set("num~s",nums);
        JsonPointer jp = JsonPointer.compile("/num~s/0");

        jo.remove("num~s");
        jo.set("nums~",nums);
        jp = JsonPointer.compile("/nums~");

        jo.remove("num~s");
        jo.set("nums~",nums);
        jp = JsonPointer.compile("/nums~/0");

        JsonNode elem0 = jo.at(jp);
        assertFalse(elem0.isMissingNode());
        assertEquals(42, elem0.asInt());
    }
}
