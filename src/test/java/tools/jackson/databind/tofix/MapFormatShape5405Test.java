package tools.jackson.databind.tofix;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SuppressWarnings("serial")
public class MapFormatShape5405Test extends DatabindTestUtil
{
    @JsonPropertyOrder({ "extra" })
    static class Map5405Base extends LinkedHashMap<String,Integer> {
        public int extra = 13;
    }

    @JsonFormat(shape=JsonFormat.Shape.POJO)
    static class Map5405AsPOJO extends Map5405Base { }

    @JsonPropertyOrder({ "a", "b", "c" })
    @JsonInclude(JsonInclude.Include.NON_NULL)
    static class Bean5405Container
    {
        public Map5405AsPOJO a;
        public Map5405Base b;
        @JsonFormat(shape=JsonFormat.Shape.POJO)
        public Map5405Base c;

        public Bean5405Container(int forA, int forB, int forC) {
            if (forA != 0) {
                a = new Map5405AsPOJO();
                a.put("value", forA);
            }
            if (forB != 0) {
                b = new Map5405Base();
                b.put("value", forB);
            }
            if (forC != 0) {
                c = new Map5405Base();
                c.put("value", forC);
            }
        }
    }

    static class Bean5405Override
    {
        @JsonFormat(shape=JsonFormat.Shape.NATURAL)
        public Map5405AsPOJO stuff;

        public Bean5405Override(int value) {
            stuff = new Map5405AsPOJO();
            stuff.put("value", value);
        }
    }

    /*
    /**********************************************************************
    /* Test methods, serialization
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#5045]: property overrides for @JsonFormat.shape won't work for Maps
    // 30-Nov-2025, tatu: Something about caching is the issue: if "b" commented out,
    //    override appears to work; with "b" not
    @JacksonTestFailureExpected
    @Test
    public void serializeAsPOJOViaProperty() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean5405Container(1,2,3));
        assertEquals(a2q(
                "{'a':{'extra':13,'empty':false},'b':{'value':2},'c':{'extra':13,'empty':false}}"),
                result);
    }

    @Test
    public void serializeNaturalViaOverride() throws Exception
    {
        String result = MAPPER.writeValueAsString(new Bean5405Override(123));
        assertEquals(a2q("{'stuff':{'value':123}}"),
                result);
    }
}
