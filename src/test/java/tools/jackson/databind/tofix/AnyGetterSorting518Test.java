package tools.jackson.databind.tofix;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

class AnyGetterSorting518Test extends DatabindTestUtil
{
    @JsonPropertyOrder(alphabetic = true)
    static class Bean
    {
        public int b;

        protected Map<String,Object> extra = new HashMap<>();

        public int a;

        public Bean(int a, int b, Map<String,Object> x) {
            this.a = a;
            this.b = b;
            extra = x;
        }

        @JsonAnyGetter
        public Map<String,Object> getExtra() { return extra; }
    }

    @JsonPropertyOrder(alphabetic = true)
    static class AnyGetterBeforeFieldsBean
    {
        public int x;

        protected Map<String,Object> extra = new HashMap<>();

        public int y;

        public AnyGetterBeforeFieldsBean(int x, int y, Map<String,Object> a) {
            this.x = x;
            this.y = y;
            extra = a;
        }

        @JsonAnyGetter
        public Map<String,Object> getExtra() { return extra; }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void anyBeanWithSort() throws Exception
    {
        Map<String,Object> extra = new LinkedHashMap<>();
        extra.put("y", 4);
        extra.put("x", 3);
        String json = MAPPER.writeValueAsString(new Bean(1, 2, extra));
        assertEquals(a2q("{'a':1,'b':2,'x':3,'y':4}"), json);
    }

    @Test
    void anyGetterSortingBeforeFields() throws Exception
    {
        Map<String,Object> extra = new LinkedHashMap<>();
        extra.put("b", 4);
        extra.put("a", 3);
        String json = MAPPER.writeValueAsString(new AnyGetterBeforeFieldsBean(1, 2, extra));
        assertEquals(a2q("{'a':3,'b':4,'x':1,'y':2}"), json);
    }
}
