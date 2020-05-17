package com.fasterxml.jackson.databind.deser;

import java.util.*;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.*;

// [databind#2283]: ignore read-only Lists even if "getter-as-setter" enabled
public class ReadOnlyListDeser2283Test
    extends BaseMapTest
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

    public void testRenamedToSameOnGetter() throws Exception
    {
        assertEquals("{\"list\":[]}",
                MAPPER.writeValueAsString(new RenamedToSameOnGetter()));
        String payload = "{\"list\":[1,2,3,4]}";
        RenamedToSameOnGetter foo = MAPPER.readValue(payload, RenamedToSameOnGetter.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }

    public void testRenamedToDifferentOnGetter() throws Exception
    {
        assertEquals("{\"renamedList\":[]}",
                MAPPER.writeValueAsString(new RenamedToDifferentOnGetter()));
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedToDifferentOnGetter foo = MAPPER.readValue(payload, RenamedToDifferentOnGetter.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }

    public void testRenamedOnClass() throws Exception
    {
        assertEquals("{\"renamedList\":[]}",
                MAPPER.writeValueAsString(new RenamedOnClass()));
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedOnClass foo = MAPPER.readValue(payload, RenamedOnClass.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }
}
