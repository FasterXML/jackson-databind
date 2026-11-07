package tools.jackson.databind.deser.filter;

import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonIncludeProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.node.ObjectNode;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#6264]: {@code @JsonIgnoreProperties} / {@code @JsonIncludeProperties}
 * declared on an {@code @JsonAnySetter} must apply to the keys it accepts, same as
 * they already do for a regular {@code Map}-valued property.
 */
public class AnySetterIgnoreProperties6264Test extends DatabindTestUtil
{
    static class MapFieldBean {
        public int a;

        @JsonAnySetter
        @JsonIgnoreProperties("secret")
        public Map<String, Object> other = new LinkedHashMap<>();
    }

    static class MethodBean {
        public final Map<String, Object> other = new LinkedHashMap<>();

        @JsonAnySetter
        @JsonIgnoreProperties("secret")
        public void any(String key, Object value) {
            other.put(key, value);
        }
    }

    static class NodeFieldBean {
        @JsonAnySetter
        @JsonIgnoreProperties("secret")
        public ObjectNode other;
    }

    // Property-based Creator plus field any-setter: values are buffered before the
    // bean exists, so goes through `PropertyValueBuffer.bufferAnyProperty()`
    static class CreatorFieldBean {
        final int a;

        @JsonAnySetter
        @JsonIgnoreProperties("secret")
        public Map<String, Object> other = new LinkedHashMap<>();

        @JsonCreator
        CreatorFieldBean(@JsonProperty("a") int a) {
            this.a = a;
        }
    }


    static class IncludeBean {
        public int a;

        @JsonAnySetter
        @JsonIncludeProperties("ok")
        public Map<String, Object> other = new LinkedHashMap<>();
    }

    static class NoAnnotationBean {
        public int a;

        @JsonAnySetter
        public Map<String, Object> other = new LinkedHashMap<>();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String DOC = """
            {"a":1, "secret":"s3cr3t", "ok":2}
            """;

    @Test
    public void ignoralOnMapField() throws Exception {
        MapFieldBean bean = MAPPER.readValue(DOC, MapFieldBean.class);
        assertEquals(1, bean.a);
        assertEquals(Map.of("ok", 2), bean.other);
    }

    @Test
    public void ignoralOnSetterMethod() throws Exception {
        MethodBean bean = MAPPER.readValue(DOC, MethodBean.class);
        assertEquals(Map.of("a", 1, "ok", 2), bean.other);
    }

    @Test
    public void ignoralOnObjectNodeField() throws Exception {
        NodeFieldBean bean = MAPPER.readValue(DOC, NodeFieldBean.class);
        assertEquals(MAPPER.readTree("{\"a\":1, \"ok\":2}"), bean.other);
    }

    @Test
    public void ignoralWithPropertyBasedCreator() throws Exception {
        CreatorFieldBean bean = MAPPER.readValue(DOC, CreatorFieldBean.class);
        assertEquals(1, bean.a);
        assertEquals(Map.of("ok", 2), bean.other);
    }

    @Test
    public void inclusionOnMapField() throws Exception {
        IncludeBean bean = MAPPER.readValue(DOC, IncludeBean.class);
        assertEquals(1, bean.a);
        assertEquals(Map.of("ok", 2), bean.other);
    }

    // Excluded entry must be consumed whole, not just its first token
    @Test
    public void ignoralSkipsStructuredValue() throws Exception {
        MapFieldBean bean = MAPPER.readValue("""
                {"secret":{"nested":[1,2,{"deep":true}]}, "ok":2, "a":1}
                """, MapFieldBean.class);
        assertEquals(1, bean.a);
        assertEquals(Map.of("ok", 2), bean.other);
    }

    @Test
    public void explicitNullForIgnoredName() throws Exception {
        MapFieldBean bean = MAPPER.readValue("""
                {"secret":null, "ok":2}
                """, MapFieldBean.class);
        assertEquals(Map.of("ok", 2), bean.other);
    }

    // Without either annotation nothing is dropped
    @Test
    public void noAnnotationKeepsEverything() throws Exception {
        NoAnnotationBean bean = MAPPER.readValue(DOC, NoAnnotationBean.class);
        assertEquals(1, bean.a);
        assertEquals(Map.of("secret", "s3cr3t", "ok", 2), bean.other);
    }
}
