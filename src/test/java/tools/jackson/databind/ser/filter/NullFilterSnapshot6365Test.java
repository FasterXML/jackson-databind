package tools.jackson.databind.ser.filter;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.DatabindException;
import tools.jackson.databind.ObjectMapper;
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
    void nullPropertyFilterRemovesBinding() {
        Map<String, PropertyFilter> filters = new HashMap<>();
        SimpleFilterProvider provider = new SimpleFilterProvider(filters);
        PropertyFilter filter = SimpleBeanPropertyFilter.filterOutAllExcept("a");
        assertSame(provider, provider.addFilter("filter", filter));
        assertSame(filter, filters.get("filter"));

        assertSame(provider, provider.addFilter("filter", (PropertyFilter) null));
        assertFalse(filters.containsKey("filter"));
        assertSame(provider, provider.addFilter("missing", (PropertyFilter) null));
        assertFalse(filters.containsKey("missing"));
    }

    @Test
    void nullSimpleBeanPropertyFilterRemovesBinding() {
        Map<String, PropertyFilter> filters = new HashMap<>();
        SimpleFilterProvider provider = new SimpleFilterProvider(filters);
        SimpleBeanPropertyFilter filter = SimpleBeanPropertyFilter.filterOutAllExcept("a");
        assertSame(provider, provider.addFilter("filter", filter));
        assertSame(filter, filters.get("filter"));

        assertSame(provider, provider.addFilter("filter", (SimpleBeanPropertyFilter) null));
        assertFalse(filters.containsKey("filter"));
        assertSame(provider, provider.addFilter("missing", (SimpleBeanPropertyFilter) null));
        assertFalse(filters.containsKey("missing"));
    }

    @Test
    void removedFilterUsesDefaultAfterMapperBuild() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", SimpleBeanPropertyFilter.filterOutAllExcept("b"))
                .addFilter("filter", (PropertyFilter) null)
                .setDefaultFilter(SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        ObjectMapper mapper = jsonMapperBuilder().filterProvider(provider).build();

        assertEquals("""
                {"a":1}""", mapper.writeValueAsString(new Bean()));
    }

    @Test
    void removedFilterWithoutDefaultFailsDuringSerialization() {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
                .addFilter("filter", (SimpleBeanPropertyFilter) null);
        ObjectMapper mapper = jsonMapperBuilder().filterProvider(provider).build();

        DatabindException e = assertThrows(DatabindException.class,
                () -> mapper.writeValueAsString(new Bean()));
        assertTrue(e.getMessage().contains("No filter configured with id 'filter'"));
    }

    @Test
    void removedFilterCanBeIgnoredAfterMapperBuild() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", SimpleBeanPropertyFilter.filterOutAllExcept("a"))
                .addFilter("filter", (PropertyFilter) null)
                .setFailOnUnknownId(false);
        ObjectMapper mapper = jsonMapperBuilder().filterProvider(provider).build();

        assertEquals("""
                {"a":1,"b":2}""", mapper.writeValueAsString(new Bean()));
    }

    @Test
    void nonNullFilterStillAppliesAfterMapperBuild() throws Exception {
        SimpleFilterProvider provider = new SimpleFilterProvider()
                .addFilter("filter", SimpleBeanPropertyFilter.filterOutAllExcept("a"));
        ObjectMapper mapper = jsonMapperBuilder().filterProvider(provider).build();

        assertEquals("""
                {"a":1}""", mapper.writeValueAsString(new Bean()));
    }
}
