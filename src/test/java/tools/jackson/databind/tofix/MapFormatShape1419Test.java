package tools.jackson.databind.tofix;

import java.util.*;

import org.junit.jupiter.api.Test;
import tools.jackson.core.testutil.failure.JacksonTestFailureExpected;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("serial")
public class MapFormatShape1419Test extends DatabindTestUtil
{
    @JsonPropertyOrder({ "extra" })
    static class Map476Base extends LinkedHashMap<String,Integer> {
        public int extra = 13;
    }

    @JsonFormat(shape=JsonFormat.Shape.POJO)
    static class Map476AsPOJO extends Map476Base { }

    @JsonPropertyOrder({ "a", "b", "c" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Bean476Container
    {
        public Map476AsPOJO a;
        public Map476Base b;
        @JsonFormat(shape=JsonFormat.Shape.POJO)
        public Map476Base c;

        public Bean476Container(int forA, int forB, int forC) {
            if (forA != 0) {
                a = new Map476AsPOJO();
                a.put("value", forA);
            }
            if (forB != 0) {
                b = new Map476Base();
                b.put("value", forB);
            }
            if (forC != 0) {
                c = new Map476Base();
                c.put("value", forC);
            }
        }
    }

    static class Bean476Override
    {
        @JsonFormat(shape=JsonFormat.Shape.NATURAL)
        public Map476AsPOJO stuff;

        public Bean476Override(int value) {
            stuff = new Map476AsPOJO();
            stuff.put("value", value);
        }
    }

    /*
    /**********************************************************
    /* Test methods, serialization
    /**********************************************************
     */

    final private ObjectMapper MAPPER = newJsonMapper();

    // Can't yet use per-property overrides at all, see [databind#1419]
    @JacksonTestFailureExpected
    @Test
    public void testSerializeAsPOJOViaProperty() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean476Container(1,0,3));
        assertEquals(a2q("{'a':{'extra':13,'empty':false},'c':{'empty':false,'value':3}}"),
                result);
    }

    @Test
    public void testSerializeNaturalViaOverride() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean476Override(123));
        assertEquals(a2q("{'stuff':{'value':123}}"),
                result);
    }
}
