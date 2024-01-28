package tools.jackson.failing;

import java.util.Collections;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class AnySetterForCreator562Test extends DatabindTestUtil
{
    // [databind#562]
    static class POJO562
    {
        String a;

        Map<String,Object> stuff;

        @JsonCreator
        public POJO562(@JsonProperty("a") String a,
            @JsonAnySetter Map<String, Object>
        leftovers) {
            this.a = a;
            stuff = leftovers;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#562]
    @Test
    public void testAnySetterViaCreator562() throws Exception
    {
        POJO562 pojo = MAPPER.readValue(a2q(
                "{'a':'value', 'b':42}"
                ),
                POJO562.class);
        assertEquals(pojo.a, "value");
        assertEquals(Collections.singletonMap("b", Integer.valueOf(42)),
                pojo.stuff);
    }
}
