package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Collections;
import java.util.List;

public class ReadOnlyCollectionDeserRenamed2283Test extends BaseMapTest
{
    static class RenamedToSameOnGetter {
        @JsonProperty(value = "list", access = JsonProperty.Access.READ_ONLY)
        List<Long> getList() {
            return Collections.unmodifiableList(Collections.emptyList());
        }
    }

    static class RenamedToDifferentOnGetter {
        @JsonProperty(value = "renamedList", access = JsonProperty.Access.READ_ONLY)
        List<Long> getList() {
            return Collections.unmodifiableList(Collections.emptyList());
        }
    }

    @JsonIgnoreProperties(value={ "renamedList" }, allowGetters=true)
    static class RenamedOnClass {
        @JsonProperty("renamedList")
        List<Long> getList() {
            return Collections.unmodifiableList(Collections.emptyList());
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = jsonMapperBuilder().configure(MapperFeature.USE_GETTERS_AS_SETTERS, true).build();

    public void testRenamedToSameOnGetter() throws Exception
    {
        String payload = "{\"list\":[1,2,3,4]}";
        RenamedToSameOnGetter foo = MAPPER.readValue(payload, RenamedToSameOnGetter.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }

    public void testRenamedToDifferentOnGetter() throws Exception
    {
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedToDifferentOnGetter foo = MAPPER.readValue(payload, RenamedToDifferentOnGetter.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }

    public void testRenamedOnClass() throws Exception
    {
        String payload = "{\"renamedList\":[1,2,3,4]}";
        RenamedOnClass foo = MAPPER.readValue(payload, RenamedOnClass.class);
        assertTrue("List should be empty", foo.getList().isEmpty());
    }
}
