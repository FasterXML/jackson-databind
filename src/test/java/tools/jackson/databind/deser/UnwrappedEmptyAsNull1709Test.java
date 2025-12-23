package tools.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// https://github.com/FasterXML/jackson-databind/issues/1709
public class UnwrappedEmptyAsNull1709Test extends DatabindTestUtil
{
    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class Container {
        public String name;
        @JsonUnwrapped
        public Unwrapped u;
    }

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class Unwrapped {
        public String s;
        public Integer n;
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.USE_NULL_FOR_EMPTY_UNWRAPPED)
            .build();

    @Test
    public void testEmptyUnwrappedAsNull() throws Exception {
        String json = a2q("{'name':'test'}");
        Container result = MAPPER.readValue(json, Container.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNull(result.u);
    }

    @Test
    public void testEmptyJsonEmptyUnwrappedAsNull() throws Exception {
        Container result = MAPPER.readValue("{}", Container.class);
        assertNotNull(result);
        assertNull(result.name);
        assertNull(result.u);
    }

    @Test
    public void testNonNullUnwrappedPreserved() throws Exception {
        String json = a2q("{'name':'test','s':'value'}");
        Container result = MAPPER.readValue(json, Container.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
    }

    @Test
    public void testPartialNonNullUnwrappedPreserved() throws Exception {
        String json = a2q("{'s':'value'}");
        Container result = MAPPER.readValue(json, Container.class);
        assertNotNull(result);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
        assertNull(result.u.n);
    }
}