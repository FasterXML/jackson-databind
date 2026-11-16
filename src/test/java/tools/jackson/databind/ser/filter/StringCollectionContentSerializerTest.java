package tools.jackson.databind.ser.filter;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.annotation.JsonSerialize;
import tools.jackson.databind.ser.std.StdSerializer;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// Custom content serializer (`contentUsing`) on `Collection<String>` must not
// drop content `@JsonInclude` or per-property `@JsonFormat` settings
public class StringCollectionContentSerializerTest extends DatabindTestUtil
{
    static class UpperCaseSerializer extends StdSerializer<String> {
        UpperCaseSerializer() { super(String.class); }

        @Override
        public boolean isEmpty(SerializationContext ctxt, String value) {
            return value.isEmpty();
        }

        @Override
        public void serialize(String value, JsonGenerator g, SerializationContext ctxt) {
            g.writeString(value.toUpperCase());
        }
    }

    @JsonPropertyOrder({ "list", "set" })
    static class InclusionBean {
        @JsonSerialize(contentUsing = UpperCaseSerializer.class)
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public List<String> list = new ArrayList<>(List.of("", "a"));

        @JsonSerialize(contentUsing = UpperCaseSerializer.class)
        @JsonInclude(content = JsonInclude.Include.NON_EMPTY)
        public Set<String> set = new LinkedHashSet<>(List.of("", "b"));
    }

    @JsonPropertyOrder({ "list", "set" })
    static class UnwrapBean {
        @JsonSerialize(contentUsing = UpperCaseSerializer.class)
        @JsonFormat(with = JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        public List<String> list = new ArrayList<>(List.of("a"));

        @JsonSerialize(contentUsing = UpperCaseSerializer.class)
        @JsonFormat(with = JsonFormat.Feature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
        public Set<String> set = new LinkedHashSet<>(List.of("b"));
    }

    @Test
    void contentInclusionRetained() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
                .build();
        assertEquals("""
                {"list":["A"],"set":["B"]}""",
                mapper.writeValueAsString(new InclusionBean()));

        // and without the feature, no filtering
        assertEquals("""
                {"list":["","A"],"set":["","B"]}""",
                jsonMapperBuilder()
                    .disable(SerializationFeature.APPLY_JSON_INCLUDE_FOR_CONTAINERS)
                    .build()
                    .writeValueAsString(new InclusionBean()));
    }

    @Test
    void perPropertyUnwrappingRetained() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .disable(SerializationFeature.WRITE_SINGLE_ELEM_ARRAYS_UNWRAPPED)
                .build();
        assertEquals("""
                {"list":"A","set":"B"}""",
                mapper.writeValueAsString(new UnwrapBean()));
    }
}
