package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @JsonView} filtering is applied by {@code BeanDeserializer} (and the array/builder
 * variants), but {@link tools.jackson.databind.deser.jdk.ThrowableDeserializer} overrides
 * {@code deserializeFromObject} wholesale and never consulted the active view. For an
 * exception type that reaches that loop (default-constructor, no property-based creator),
 * a view-hidden property was populated from input even when the active view excluded it.
 */
public class ThrowableViewDeserializationBypassTest extends DatabindTestUtil
{
    static class Public {}
    static class Internal {}

    @SuppressWarnings("serial")
    static class ViewException extends RuntimeException {
        @JsonView(Public.class) public String pub;
        @JsonView(Internal.class) public String sec; // internal-only
        public ViewException() { super(); }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Under the Public view the Internal-only property must not be set from input
    @Test
    public void throwableHonorsViewOnDeserialize() throws Exception {
        ViewException ex = MAPPER.readerWithView(Public.class)
                .forType(ViewException.class)
                .readValue("{\"pub\":\"visible\",\"sec\":\"leaked\"}");

        assertEquals("visible", ex.pub);
        assertNull(ex.sec,
                "view-hidden 'sec' should stay null under the Public view but was: " + ex.sec);
    }

    // Control: with no active view every property is set as before
    @Test
    public void throwableWithoutViewSetsAll() throws Exception {
        ViewException ex = MAPPER.readerFor(ViewException.class)
                .readValue("{\"pub\":\"visible\",\"sec\":\"kept\"}");

        assertEquals("visible", ex.pub);
        assertEquals("kept", ex.sec);
    }
}
