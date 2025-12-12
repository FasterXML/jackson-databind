package tools.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

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

    @JsonInclude(JsonInclude.Include.NON_DEFAULT)
    static class MultiUnwrappedContainer {
        public String id;
        @JsonUnwrapped(prefix = "a_")
        public Unwrapped first;
        @JsonUnwrapped(prefix = "b_")
        public Unwrapped second;
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .enable(DeserializationFeature.ACCEPT_EMPTY_UNWRAPPED_AS_NULL)
            .build();

    @Test
    public void testEmptyUnwrappedAsNull() throws Exception {
        Container result = MAPPER.readValue("{\"name\":\"test\"}", Container.class);
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
        Container result = MAPPER.readValue("{\"name\":\"test\",\"s\":\"value\"}", Container.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
    }

    @Test
    public void testPartialNonNullUnwrappedPreserved() throws Exception {
        Container result = MAPPER.readValue("{\"s\":\"value\"}", Container.class);
        assertNotNull(result);
        assertNotNull(result.u);
        assertEquals("value", result.u.s);
        assertNull(result.u.n);
    }

    @Test
    public void testExplicitNullsTreatedAsEmpty() throws Exception {
        Container result = MAPPER.readValue("{\"name\":\"test\",\"s\":null,\"n\":null}", Container.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNull(result.u);
    }

    @Test
    public void testFeatureDisabledCreatesInstance() throws Exception {
        ObjectMapper defaultMapper = newJsonMapper();
        Container result = defaultMapper.readValue("{\"name\":\"test\"}", Container.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNotNull(result.u); // instance created with null fields
        assertNull(result.u.s);
    }

    @Test
    public void testMultipleUnwrappedBothEmpty() throws Exception {
        MultiUnwrappedContainer result = MAPPER.readValue("{\"id\":\"123\"}", MultiUnwrappedContainer.class);
        assertNotNull(result);
        assertEquals("123", result.id);
        assertNull(result.first);
        assertNull(result.second);
    }

    @Test
    public void testRoundTripSymmetry() throws Exception {
        Container c1 = new Container();
        c1.name = "test";
        c1.u = new Unwrapped(); // empty unwrapped

        Container c2 = new Container();
        c2.name = "test";
        c2.u = null;

        String json1 = MAPPER.writeValueAsString(c1);
        String json2 = MAPPER.writeValueAsString(c2);
        assertEquals(json1, json2);

        Container result = MAPPER.readValue(json1, Container.class);
        assertNotNull(result);
        assertEquals("test", result.name);
        assertNull(result.u);
    }
}