package tools.jackson.databind.deser;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4639] 2.18.1 : regression when using @JsonAnySetter outside of @JsonCreator
public class AnySetterFieldWithCreator4639Test
    extends DatabindTestUtil
{
    public static class Bean {
        int b;
        int d;

        @JsonAnySetter
        Map<String, ?> any;

        @JsonCreator
        public Bean(@JsonProperty("b") int b, @JsonProperty("d") int d) {
            this.b = b;
            this.d = d;
        }
    }

    public static class SomeBean {
        final int b;
        final int d;
        final Map<String, Object> any = new HashMap<>();

        @JsonCreator
        public SomeBean(@JsonProperty("b") int b, @JsonProperty("d") int d) {
            this.b = b;
            this.d = d;
        }

        @JsonAnySetter
        public void setAny(String name, Object value) {
            any.put(name, value);
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testJsonAnySetter()
            throws Exception
    {
        String json = "{\"a\":1,\"b\":2,\"c\":3,\"d\":4,\"e\":5,\"f\":6}";

        Bean bean = MAPPER.readValue(json, Bean.class);
        assertEquals(2, bean.b);
        assertEquals(4, bean.d);

        // failed with:
        // org.opentest4j.AssertionFailedError:
        // Expected :{b=2, c=3, e=5, f=6}
        // Actual   :{e=5, f=6}
        assertEquals(mapOf("a", 1, "c", 3, "e", 5, "f", 6), bean.any);
    }

    @Test
    public void testJsonAnySetterWithField()
            throws Exception
    {
        String json = "{\"a\":1,\"b\":2,\"c\":3,\"d\":4,\"e\":5,\"f\":6}";

        SomeBean bean = MAPPER.readValue(json, SomeBean.class);
        assertEquals(2, bean.b);
        assertEquals(4, bean.d);

        // failed with:
        // org.opentest4j.AssertionFailedError:
        // Expected :{b=2, c=3, e=5, f=6}
        // Actual   :{e=5, f=6}
        assertEquals(mapOf("a", 1, "c", 3, "e", 5, "f", 6), bean.any);
    }

    private Map<String, Integer> mapOf(String a, int i, String b, int i1, String c, int i2, String d, int i3)
    {
        Map<String, Integer> map = new java.util.HashMap<>();
        map.put(a, i);
        map.put(b, i1);
        map.put(c, i2);
        map.put(d, i3);
        return map;
    }

}
