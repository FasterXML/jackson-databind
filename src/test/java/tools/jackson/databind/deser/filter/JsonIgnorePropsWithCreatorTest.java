package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

/**
 * Test that {@code @JsonIgnoreProperties} works correctly with POJO classes
 * that use {@code @JsonCreator} (property-based creators), not just Records.
 * <p>
 * Regression: previously the ignore check was guarded by {@code isRecord},
 * so POJOs with creators would bypass ignore configuration.
 */
public class JsonIgnorePropsWithCreatorTest
{
    @JsonIgnoreProperties("name")
    static class PojoWithCreator {
        final int id;
        final String name;

        @JsonCreator
        public PojoWithCreator(@JsonProperty("id") int id,
                @JsonProperty("name") String name)
        {
            this.id = id;
            this.name = name;
        }
    }

    // Wrapper to test field-level @JsonIgnoreProperties with creator-based inner type
    static class IdAndName {
        final int id;
        final String name;

        @JsonCreator
        public IdAndName(@JsonProperty("id") int id,
                @JsonProperty("name") String name)
        {
            this.id = id;
            this.name = name;
        }
    }

    static class WrapperWithIgnore {
        @JsonIgnoreProperties("name")
        public IdAndName child;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Verify class-level @JsonIgnoreProperties with @JsonCreator on a POJO
    @Test
    public void testClassIgnoreWithCreator() throws Exception
    {
        PojoWithCreator result = MAPPER.readValue(
                "{\"id\":123,\"name\":\"Bob\"}", PojoWithCreator.class);
        assertEquals(123, result.id);
        // "name" should be ignored and remain null
        assertNull(result.name);
    }

    // Verify field-level @JsonIgnoreProperties on a creator parameter
    @Test
    public void testFieldIgnoreWithCreator() throws Exception
    {
        WrapperWithIgnore result = MAPPER.readValue(
                "{\"child\":{\"id\":123,\"name\":\"Bob\"}}", WrapperWithIgnore.class);
        assertEquals(123, result.child.id);
        // "name" on child should be ignored
        assertNull(result.child.name);
    }
}
