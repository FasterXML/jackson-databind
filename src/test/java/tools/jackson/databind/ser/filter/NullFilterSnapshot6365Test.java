package tools.jackson.databind.ser.filter;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.ser.PropertyFilter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;

import static org.junit.jupiter.api.Assertions.*;

class NullFilterSnapshot6365Test extends DatabindTestUtil
{
    @JsonFilter("filter")
    @JsonPropertyOrder({ "a", "b" })
    static class Bean {
        public int a = 1;
        public int b = 2;
    }

    @Test
    void nullFilterUsesDefaultAfterMapperBuild() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", (PropertyFilter) null)
                .setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        ObjectMapper mapper = JsonMapper.builder().filterProvider(provider).build();

        assertEquals(a2q("{'a':1}"), mapper.writeValueAsString(new Bean()));
    }

    @Test
    void nullMappingUsesDefaultAfterMapperBuild() throws Exception {
        Map<String, PropertyFilter> filters = new HashMap<>();
        filters.put("filter", null);
        SimpleFilterProvider provider = new SimpleFilterProvider(filters)
                .setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("b"));
        ObjectMapper mapper = JsonMapper.builder().filterProvider(provider).build();

        assertEquals(a2q("{'b':2}"), mapper.writeValueAsString(new Bean()));
    }

    @Test
    void nullFilterWithoutDefaultFailsDuringSerialization() {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", (SimpleBeanPropertyFilter) null);
        ObjectMapper mapper = JsonMapper.builder().filterProvider(provider).build();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(new Bean()));
        assertTrue(e.getMessage().contains("No filter configured with id 'filter'"));
    }

    @Test
    void nullFilterCanBeIgnoredAfterMapperBuild() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", (PropertyFilter) null)
                .setFailOnUnknownId(false);
        ObjectMapper mapper = JsonMapper.builder().filterProvider(provider).build();

        assertEquals(a2q("{'a':1,'b':2}"), mapper.writeValueAsString(new Bean()));
    }

    @Test
    void nonNullFilterStillAppliesAfterMapperBuild() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        ObjectMapper mapper = JsonMapper.builder().filterProvider(provider).build();

        assertEquals(a2q("{'a':1}"), mapper.writeValueAsString(new Bean()));
    }
}
