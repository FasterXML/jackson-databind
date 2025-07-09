package com.fasterxml.jackson.databind.ser;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

// For [databind#5215]: Any-getter should be sorted last, by default
public class AnyGetterOrdering5215Test
    extends DatabindTestUtil
{
    static class DynaBean {
        public String l;
        public String j;
        public String a;

        protected Map<String, Object> extensions = new LinkedHashMap<>();

        @JsonAnyGetter
        public Map<String, Object> getExtensions() {
            return extensions;
        }

        @JsonAnySetter
        public void addExtension(String name, Object value) {
            extensions.put(name, value);
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(MapperFeature.SORT_PROPERTIES_ALPHABETICALLY)
            .configure(SerializationFeature.ORDER_MAP_ENTRIES_BY_KEYS, true)
            .build();

    @Test
    public void testDynaBean() throws Exception
    {
        DynaBean b = new DynaBean();
        b.a = "1";
        b.j = "2";
        b.l = "3";
        b.addExtension("z", "5");
        b.addExtension("b", "4");
        assertEquals(a2q("{" +
                "'a':'1'," +
                "'j':'2'," +
                "'l':'3'," +
                "'b':'4'," +
                "'z':'5'}"), MAPPER.writeValueAsString(b));
    }
}
