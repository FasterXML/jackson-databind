package com.fasterxml.jackson.databind.deser.filter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

// [databind#6338]: readerForUpdating() must honor Nulls.SKIP for
// creator properties too
public class SkipNullsCreatorUpdate6338Test
    extends DatabindTestUtil
{
    static class FieldBean {
        @JsonSetter(nulls = Nulls.SKIP)
        public List<String> values;
        public String name;

        @JsonCreator
        public FieldBean(@JsonProperty("values") List<String> values) {
            this.values = values;
        }
    }

    static class SetterBean {
        protected List<String> values;
        public String name;

        @JsonCreator
        public SetterBean(@JsonProperty("values") List<String> values) {
            this.values = values;
        }

        @JsonSetter(nulls = Nulls.SKIP)
        public void setValues(List<String> values) {
            this.values = values;
        }
    }

    static class PlainBean {
        public List<String> values;
        public String name;

        @JsonCreator
        public PlainBean(@JsonProperty("values") List<String> values) {
            this.values = values;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final static String JSON = a2q("{'values':null,'name':'x'}");

    @Test
    public void testSkipNullsForCreatorField() throws Exception
    {
        FieldBean bean = new FieldBean(new ArrayList<>(Arrays.asList("a")));
        MAPPER.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    @Test
    public void testSkipNullsForCreatorSetter() throws Exception
    {
        SetterBean bean = new SetterBean(new ArrayList<>(Arrays.asList("a")));
        MAPPER.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    @Test
    public void testSkipNullsForCreatorGlobalDefault() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder()
                .defaultSetterInfo(JsonSetter.Value.forValueNulls(Nulls.SKIP))
                .build();
        PlainBean bean = new PlainBean(new ArrayList<>(Arrays.asList("a")));
        mapper.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertEquals(Arrays.asList("a"), bean.values);
    }

    // Without "skip nulls", null is still set as before
    @Test
    public void testNullSetForCreatorWithoutSkip() throws Exception
    {
        PlainBean bean = new PlainBean(new ArrayList<>(Arrays.asList("a")));
        MAPPER.readerForUpdating(bean).readValue(JSON);
        assertEquals("x", bean.name);
        assertNull(bean.values);
    }
}
