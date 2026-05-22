package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5969] / [databind#5971] (external-type-id path): @JsonView bypass for
 * regular (non-creator) and creator properties handled by
 * {@code BeanDeserializer.deserializeUsingPropertyBasedWithExternalTypeId()}.
 *
 * Same gap as the plain and unwrapped property-based-creator paths: an external
 * type id ({@code @JsonTypeInfo(include = EXTERNAL_PROPERTY)}) combined with a
 * property-based {@code @JsonCreator} routes through a dedicated method whose
 * creator-property ([databind#5971]) and regular-property ([databind#5969])
 * branches did not check {@code visibleInView}.
 */
public class ExternalTypeIdViewBypassTest extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    public interface Value { }

    @JsonTypeName("string")
    public static class StringValue implements Value {
        public String v;
    }

    public static class Envelope {
        @JsonView(PublicView.class)
        public final String name;

        // Creator parameter, view-restricted -> creator-property branch ([databind#5971]).
        @JsonView(AdminView.class)
        public final String secret;

        // Non-creator, view-restricted property -> regular-property branch ([databind#5969]).
        @JsonView(AdminView.class)
        public String password;

        // External-type-id property forces the dedicated code path; "valueType"
        // is the external type id property.
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "valueType")
        @JsonSubTypes({ @JsonSubTypes.Type(StringValue.class) })
        @JsonView(PublicView.class)
        public final Value value;

        @JsonCreator
        public Envelope(@JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonProperty("secret") @JsonView(AdminView.class) String secret,
                @JsonProperty("value") @JsonView(PublicView.class) Value value) {
            this.name = name;
            this.secret = secret;
            this.value = value;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private static final String FULL_JSON =
            "{\"name\":\"alice\",\"secret\":\"sssh\",\"password\":\"pw\","
            + "\"valueType\":\"string\",\"value\":{\"v\":\"x\"}}";

    // Negative control: AdminView populates the view-restricted properties.
    // (Note: the nested external-type-id value content is not deep-checked here;
    // its binding under property-based creators is an unrelated existing quirk.)
    @Test
    public void testAdminView_negativeControl() throws Exception {
        Envelope result = MAPPER.readerWithView(AdminView.class)
                .forType(Envelope.class)
                .readValue(FULL_JSON);
        assertEquals("alice", result.name);
        assertEquals("sssh", result.secret);
        assertEquals("pw", result.password);
        assertTrue(result.value instanceof StringValue);
    }

    // Formerly failing: under PublicView the @JsonView(AdminView) regular property
    // "password" must not be populated via the external-type-id path.
    @Test
    public void testRegularPropertyHonorsViewWithExternalTypeId() throws Exception {
        Envelope result = MAPPER.readerWithView(PublicView.class)
                .forType(Envelope.class)
                .readValue(FULL_JSON);

        assertEquals("alice", result.name);
        assertTrue(result.value instanceof StringValue);
        assertNull(result.password,
                "[databind#5969] @JsonView(AdminView) property was populated in PublicView " +
                "via the external-type-id path. password = " + result.password);
    }

    // Formerly failing: under PublicView the @JsonView(AdminView) creator parameter
    // "secret" must not be populated via the external-type-id path.
    @Test
    public void testCreatorPropertyHonorsViewWithExternalTypeId() throws Exception {
        Envelope result = MAPPER.readerWithView(PublicView.class)
                .forType(Envelope.class)
                .readValue(FULL_JSON);

        assertEquals("alice", result.name);
        assertTrue(result.value instanceof StringValue);
        assertNull(result.secret,
                "[databind#5971] @JsonView(AdminView) creator parameter was populated in PublicView " +
                "via the external-type-id path. secret = " + result.secret);
    }
}
