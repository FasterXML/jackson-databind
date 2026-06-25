package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests that {@code @JsonUnwrapped} does not bypass {@code @JsonView} restrictions
 * during deserialization.
 *<p>
 * When a property is annotated with both {@code @JsonUnwrapped} and a restrictive
 * {@code @JsonView}, its inlined nested properties must not be deserialized when the
 * active view does not include the property's declared view.
 */
public class ViewUnwrappedBypassTest extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}
    
    static class AccountFlags {
        @JsonView(AdminView.class) public String role;
        @JsonView(AdminView.class) public boolean approved;
        @JsonView(AdminView.class) public long creditBalance;
    }

    /**
     * Control case: {@code flags} is a plain (non-unwrapped) property.
     * The view check for regular properties has always worked; verifies our
     * baseline assumption.
     */
    static class Control {
        @JsonView(PublicView.class)  public String email;
        @JsonView(AdminView.class)   public AccountFlags flags;
    }

    /**
     * Bypass candidate: {@code flags} is an {@code @JsonUnwrapped} property.
     * Its nested fields ({@code role}, {@code approved}, {@code creditBalance}) are
     * inlined into the parent JSON object, which previously allowed them to leak
     * through when the active view excluded {@code flags}.
     */
    static class Registration {
        @JsonView(PublicView.class)  public String email;
        @JsonView(PublicView.class)  public String password;
        @JsonView(AdminView.class) @JsonUnwrapped public AccountFlags flags;
    }

    private static final String JSON =
            a2q("{'email':'mallory@evil.test','password':'pw',"
            + "'role':'ADMIN','approved':true,'creditBalance':1000000}");

    private final ObjectMapper MAPPER = newJsonMapper();

    // -----------------------------------------------------------------------
    // Control: AdminView populates everything

    @Test
    public void controlAdminViewPopulatesFlags() throws Exception {
        String json = a2q("{'email':'a@b.com',"
                        + "'flags':{'role':'ADMIN','approved':true,'creditBalance':1000000}}");
        Control result = MAPPER.readerWithView(AdminView.class)
                .forType(Control.class)
                .readValue(json);
        assertEquals("a@b.com", result.email);
        assertNotNull(result.flags);
        assertEquals("ADMIN", result.flags.role);
    }

    @Test
    public void controlPublicViewLeaveFlagsNull() throws Exception {
        String json = a2q("{'email':'mallory@evil.test','password':'pw',"
                + "'flags':{'role':'ADMIN','approved':true,'creditBalance':1000000}}");
        Control result = MAPPER.readerWithView(PublicView.class)
                .forType(Control.class)
                .readValue(json);
        assertEquals("mallory@evil.test", result.email);
        assertNull(result.flags,
                "Control: @JsonView(AdminView) plain property must be null in PublicView");
    }

    // -----------------------------------------------------------------------
    // Registration (unwrapped): AdminView populates everything

    @Test
    public void registrationAdminViewPopulatesFlags() throws Exception {
        Registration result = MAPPER.readerWithView(AdminView.class)
                .forType(Registration.class)
                .readValue(JSON);
        assertEquals("mallory@evil.test", result.email);
        assertNotNull(result.flags);
        assertEquals("ADMIN", result.flags.role);
        assertTrue(result.flags.approved);
        assertEquals(1000000L, result.flags.creditBalance);
    }

    // -----------------------------------------------------------------------
    // Registration (unwrapped): PublicView must NOT populate view-restricted flags

    @Test
    public void registrationPublicViewLeaveFlagsNull() throws Exception {
        Registration result = MAPPER.readerWithView(PublicView.class)
                .forType(Registration.class)
                .readValue(JSON);
        assertEquals("mallory@evil.test", result.email);
        assertEquals("pw", result.password);
        assertNull(result.flags,
                "@JsonView(AdminView) @JsonUnwrapped property must be null in PublicView: "
                + result.flags);
    }
}
