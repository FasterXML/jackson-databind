package tools.jackson.databind.ser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFilter;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.ser.std.SimpleBeanPropertyFilter;
import tools.jackson.databind.ser.std.SimpleFilterProvider;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for verifying handling of a POJO configured with both {@code @JsonFilter}
 * and {@code @JsonFormat(shape=ARRAY)}.
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

    @JsonPropertyOrder({ "name", "secret", "age" })
    static class PlainBean {
        public String name = "Bob";
        public String secret = "s3cr3t";
        public int age = 30;
    }

    static class PropertyWrapper {
        @JsonFilter("beanFilter")
        @JsonFormat(shape = JsonFormat.Shape.ARRAY)
        public PlainBean value = new PlainBean();
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
    public void filterWithArrayShapeFails() throws Exception {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> writerExcluding("secret").writeValueAsString(new AsArrayBean()));

        verifyException(e, "JsonFormat(shape = ARRAY)");
        verifyException(e, "JsonFilter");
        verifyException(e, "not compatible with array serialization");
    }

    @Test
    public void filterWithObjectShapeStillWorks() throws Exception {
        assertEquals("{\"name\":\"Bob\",\"age\":30}",
                writerExcluding("secret").writeValueAsString(new AsObjectBean()));
        assertEquals("{\"name\":\"Bob\"}",
                writerIncludingOnly("name").writeValueAsString(new AsObjectBean()));
    }

    @Test
    public void propertyFilterWithArrayShapeDoesNotPoisonSerializerCache() throws Exception {
        String plainJson = "{\"name\":\"Bob\",\"secret\":\"s3cr3t\",\"age\":30}";

        // Warm the unfiltered serializer cache before applying property-level overrides.
        assertEquals(plainJson, MAPPER.writeValueAsString(new PlainBean()));

        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> writerExcluding("secret").writeValueAsString(new PropertyWrapper()));

        verifyException(e, "JsonFormat(shape = ARRAY)");
        verifyException(e, "JsonFilter");

        // A failed contextualization must not contaminate the cached base serializer.
        assertEquals(plainJson, MAPPER.writeValueAsString(new PlainBean()));
    }
}
