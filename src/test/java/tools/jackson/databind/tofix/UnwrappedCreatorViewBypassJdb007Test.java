package tools.jackson.databind.tofix;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * JDB-007: @JsonView bypass for unwrapped creator parameters.
 *
 * Commit 502fe888 (Fix #1467) added support for @JsonUnwrapped creator parameters.
 * UnwrappedPropertyHandler.processUnwrappedCreatorProperties() was added to replay
 * the buffered token stream into creator parameters.
 *
 * Security regression: processUnwrappedCreatorProperties() iterates over _creatorProperties
 * and calls prop.deserialize() without checking visibleInView(activeView). When an
 * application uses JsonView as a write-side access-control boundary, an @JsonView(Admin)
 * constructor parameter annotated with @JsonUnwrapped can still be populated from
 * untrusted JSON while reading with a different (less-privileged) active view.
 *
 * Patch: in processUnwrappedCreatorProperties(), obtain the active view from ctxt and
 * skip (do not assign) any creator property not visible in that view.
 */
public class UnwrappedCreatorViewBypassJdb007Test extends DatabindTestUtil
{
    // --- Views ---
    static class PublicView {}
    static class AdminView extends PublicView {}

    // --- Models ---

    public static class Address {
        @JsonView(AdminView.class)
        public String street;

        @JsonView(PublicView.class)
        public String city;
    }

    /**
     * Outer bean with @JsonCreator constructor taking an @JsonUnwrapped
     * parameter restricted to AdminView.
     */
    public static class UserBean {
        @JsonView(PublicView.class)
        public final String name;

        // address is AdminView-only via @JsonView + @JsonUnwrapped
        @JsonView(AdminView.class)
        public final Address address;

        @JsonCreator
        public UserBean(
                @JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonView(AdminView.class) @JsonUnwrapped Address address) {
            this.name = name;
            this.address = address;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * NEGATIVE CONTROL: AdminView sees all fields.
     */
    @Test
    public void testAdminView_negativeControl() throws Exception {
        UserBean result = MAPPER
                .readerWithView(AdminView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"street\":\"1 Main St\",\"city\":\"Springfield\"}");
        assertEquals("alice", result.name);
        assertNotNull(result.address);
        assertEquals("1 Main St", result.address.street);
        assertEquals("Springfield", result.address.city);
    }

    /**
     * EXPLOIT PATH: PublicView is active but attacker supplies admin-only address fields.
     *
     * Security assertion: with PublicView active, the @JsonView(AdminView.class)
     * @JsonUnwrapped creator parameter must not be populated from attacker-supplied JSON.
     * The entire Address creator parameter is AdminView-only; in PublicView the parameter
     * must be null (processUnwrappedCreatorProperties must skip non-visible params).
     */
    @JacksonTestFailureExpected
    @Test
    public void testJdb007_unwrappedCreatorParamBypassesJsonView() throws Exception {
        UserBean result = MAPPER
                .readerWithView(PublicView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"street\":\"1 Main St\",\"city\":\"Springfield\"}");

        assertEquals("alice", result.name);
        // The entire Address creator param is @JsonView(AdminView): with PublicView active,
        // processUnwrappedCreatorProperties must not assign the address parameter.
        // Without the fix, address is non-null (deserialized despite view restriction).
        assertNull(result.address,
                "JDB-007 VULNERABLE: @JsonView(AdminView) @JsonUnwrapped creator parameter " +
                "was populated in PublicView. address = " + result.address);
    }
}
