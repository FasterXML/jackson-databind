package tools.jackson.databind.introspect;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// Setting a `null` AnnotationIntrospector should disable annotation
// processing (same as disabling `MapperFeature.USE_ANNOTATIONS`), not
// cause a `NullPointerException`
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
        // annotations not processed, so no renaming
        assertEquals("""
                {"x":1}""", MAPPER.writeValueAsString(new Bean()));
    }

    @Test
    public void deserializeWithNullIntrospector() throws Exception {
        assertEquals(3, MAPPER.readValue("""
                {"x":3}""", Bean.class).x);
    }

    @Test
    public void annotationProcessingDisabledWithNullIntrospector() {
        assertFalse(MAPPER.serializationConfig().isAnnotationProcessingEnabled());
        assertFalse(MAPPER.deserializationConfig().isAnnotationProcessingEnabled());
    }

    // Introspector registered by a module on top of `null` one is used as-is
    @Test
    public void moduleIntrospectorOverNullIntrospector() throws Exception {
        final AnnotationIntrospector intr = new JacksonAnnotationIntrospector();
        ObjectMapper mapper = JsonMapper.builder()
                .annotationIntrospector(null)
                .addModule(new SimpleModule() {
                    @Override
                    public void setupModule(SetupContext ctxt) {
                        super.setupModule(ctxt);
                        ctxt.insertAnnotationIntrospector(intr);
                    }
                })
                .build();
        assertSame(intr, mapper.serializationConfig().getAnnotationIntrospector());
        assertEquals("""
                {"renamed":1}""", mapper.writeValueAsString(new Bean()));
    }
}
