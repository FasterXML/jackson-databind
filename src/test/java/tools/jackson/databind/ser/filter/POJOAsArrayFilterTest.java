package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * Tests for verifying that a {@code @JsonFilter} is still applied to a POJO that
 * also asks for {@code @JsonFormat(shape=ARRAY)} output.
 */
public class POJOAsArrayFilterTest extends DatabindTestUtil
{
    @JsonFilter("beanFilter")
    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "name", "secret", "age" })
    static class AsArrayBean {
        public String name = "Bob";
        public String secret = "s3cr3t";
        public int age = 30;
    }

    @JsonFilter("beanFilter")
    @JsonPropertyOrder({ "name", "secret", "age" })
    static class AsObjectBean {
        public String name = "Bob";
        public String secret = "s3cr3t";
        public int age = 30;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private ObjectWriter writerExcluding(String... toExclude) {
        return MAPPER.writer(new SimpleFilterProvider()
                .addFilter("beanFilter", SimpleBeanPropertyFilter.serializeAllExcept(toExclude)));
    }

    private ObjectWriter writerIncludingOnly(String... toInclude) {
        return MAPPER.writer(new SimpleFilterProvider()
                .addFilter("beanFilter", SimpleBeanPropertyFilter.filterOutAllExcept(toInclude)));
    }

    @Test
    public void excludeFilterWithArrayShape() throws Exception {
        assertEquals("{\"name\":\"Bob\",\"age\":30}",
                writerExcluding("secret").writeValueAsString(new AsArrayBean()));
        // ... and same as without the shape override:
        assertEquals("{\"name\":\"Bob\",\"age\":30}",
                writerExcluding("secret").writeValueAsString(new AsObjectBean()));
    }

    @Test
    public void includeFilterWithArrayShape() throws Exception {
        assertEquals("{\"name\":\"Bob\"}",
                writerIncludingOnly("name").writeValueAsString(new AsArrayBean()));
        assertEquals("{\"name\":\"Bob\"}",
                writerIncludingOnly("name").writeValueAsString(new AsObjectBean()));
    }
}
