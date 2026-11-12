package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Setting a `null` AnnotationIntrospector should disable annotation
// introspection, not cause a `NullPointerException`
public class NullAnnotationIntrospectorTest extends DatabindTestUtil
{
    static class Bean {
        @JsonProperty("renamed")
        public int x = 1;
    }

    private final ObjectMapper MAPPER = JsonMapper.builder()
            .annotationIntrospector(null)
            .build();

    @Test
    public void serializeWithNullIntrospector() throws Exception {
        // annotations not introspected, so no renaming
        assertEquals("""
                {"x":1}""", MAPPER.writeValueAsString(new Bean()));
    }

    @Test
    public void deserializeWithNullIntrospector() throws Exception {
        assertEquals(3, MAPPER.readValue("""
                {"x":3}""", Bean.class).x);
    }

    @Test
    public void nullIntrospectorReplacedWithNop() {
        assertSame(AnnotationIntrospector.nopInstance(),
                MAPPER.serializationConfig().getAnnotationIntrospector());
        assertSame(AnnotationIntrospector.nopInstance(),
                JsonMapper.builder().annotationIntrospector(null).annotationIntrospector());
    }
}
