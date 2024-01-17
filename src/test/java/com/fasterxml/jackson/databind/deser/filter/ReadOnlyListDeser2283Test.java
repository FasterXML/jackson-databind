package com.fasterxml.jackson.databind.deser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

// [databind#2283]: ignore read-only Lists even if "getter-as-setter" enabled
public class ReadOnlyListDeser2283Test
{
    static class RenamedToSameOnGetter {
        @JsonProperty(value = "list", access = JsonProperty.Access.READ_ONLY)
        List<Long> getList() {
            return Collections.emptyList();
        }
    }

    static class RenamedToDifferentOnGetter {
        @JsonProperty(value = "renamedList", access = JsonProperty.Access.READ_ONLY)
        List<Long> getList() {
            return Collections.emptyList();
        }
    }

    @JsonIgnoreProperties(value={ "renamedList" }, allowGetters=true)
    static class RenamedOnClass {
        @JsonProperty("renamedList")
        List<Long> getList() {
            return Collections.emptyList();
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .configure(MapperFeature.USE_GETTERS_AS_SETTERS, true).build();

    @Test
    public void testRenamedToSameOnGetter() throws Exception
    {
        assertEquals("{\"list\":[]}",
                MAPPER.writeValueAsString(new RenamedToSameOnGetter()));
        String payload = "{\"list\":[1,2,3,4]}";
        RenamedToSameOnGetter foo = MAPPER.readValue(payload, RenamedToSameOnGetter.class);
        assertTrue(foo.getList().isEmpty(), "List should be empty");
    }

    @Test
    public void testRenamedToDifferentOnGetter() throws Exception
    {
        assertEquals("{\"renamedList\":[]}",
                MAPPER.writeValueAsString(new RenamedToDifferentOnGetter()));
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedToDifferentOnGetter foo = MAPPER.readValue(payload, RenamedToDifferentOnGetter.class);
        assertTrue(foo.getList().isEmpty(), "List should be empty");
    }

    @Test
    public void testRenamedOnClass() throws Exception
    {
        assertEquals("{\"renamedList\":[]}",
                MAPPER.writeValueAsString(new RenamedOnClass()));
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedOnClass foo = MAPPER.readValue(payload, RenamedOnClass.class);
        assertTrue(foo.getList().isEmpty(), "List should be empty");
    }
}
