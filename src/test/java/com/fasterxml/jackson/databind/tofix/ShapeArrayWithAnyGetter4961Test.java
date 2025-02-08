package com.fasterxml.jackson.databind.tofix;

import java.util.Map;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#4961] Serialization for `JsonFormat.Shape.ARRAY` does not work when there is `@JsonAnyGetter`
public class ShapeArrayWithAnyGetter4961Test
    extends DatabindTestUtil
{

    static class WrapperForAnyGetter {
        public BeanWithAnyGetter value;
    }

    @JsonPropertyOrder({ "firstProperty", "secondProperties", "anyProperty", "forthProperty" })
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    static class BeanWithAnyGetter {
        public String firstProperty = "first";
        public String secondProperties = "second";
        public String forthProperty = "forth";
        @JsonAnyGetter
        public Map<String, String> getAnyProperty() {
            Map<String, String> map = new TreeMap<>();
            map.put("third_A", "third_A");
            map.put("third_B", "third_B");
            return map;
        }
    }

    final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testSerializeArrayWithAnyGetterWithWrapper() throws Exception {
        WrapperForAnyGetter wrapper = new WrapperForAnyGetter();
        wrapper.value = new BeanWithAnyGetter();

        String json = MAPPER.writeValueAsString(wrapper);

        // Fails Actual   :{"value":{"firstProperty":"first","secondProperties":"second","forthProperty":"forth","third_A":"third_A","third_B":"third_B"}}
        assertEquals(a2q("{'value':" +
                "[" +
                "'first'," +
                "'second'," +
                "'forth'," +
                "'third_A'," +
                "'third_B'" +
                "]" +
                "}"), json);
    }

    @Test
    public void testSerializeArrayWithAnyGetterAsRoot() throws Exception {
        BeanWithAnyGetter bean = new BeanWithAnyGetter();

        String json = MAPPER.writeValueAsString(bean);

        // Fails Actual : {"firstProperty":"first","secondProperties":"second","forthProperty":"forth","third_A":"third_A","third_B":"third_B"}
        assertEquals(a2q("[" +
                "'first'," +
                "'second'," +
                "'forth'," +
                "'third_A'," +
                "'third_B'" +
                "]"), json);
    }
}
