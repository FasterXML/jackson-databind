package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5969] / [databind#5971] (BeanDeserializer unwrapped path): @JsonView
 * bypass for regular (non-creator) and creator properties handled by
 * {@code BeanDeserializer.deserializeUsingPropertyBasedWithUnwrapped()}.
 *
 * The property-based-creator-with-unwrapped path on the POJO (non-builder)
 * deserializer computed no active view and so skipped the {@code visibleInView}
 * check on both the creator-property ([databind#5971]) and the regular-property
 * ([databind#5969]) buffering branches. A {@code @JsonView(Admin)} property could
 * be populated under a non-Admin view when an {@code @JsonUnwrapped} member forces
 * this code path.
 */
public class UnwrappedPojoViewBypassTest extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    public static class Address {
        @JsonView(PublicView.class)
        public String city;
    }

    public static class UserBean {
        @JsonView(PublicView.class)
        public final String name;

        // Creator parameter, view-restricted -> exercises the creator-property
        // branch under the unwrapped code path ([databind#5971]).
        @JsonView(AdminView.class)
        public final String secret;

        // Non-creator, view-restricted property -> exercises the regular-property
        // buffering branch under the unwrapped code path ([databind#5969]).
        @JsonView(AdminView.class)
        public String password;

        @JsonView(PublicView.class)
        @JsonUnwrapped
        public Address address;

        @JsonCreator
        public UserBean(@JsonProperty("name") @JsonView(PublicView.class) String name,
                @JsonProperty("secret") @JsonView(AdminView.class) String secret) {
            this.name = name;
            this.secret = secret;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Negative control: AdminView populates everything.
    @Test
    public void testAdminView_negativeControl() throws Exception {
        UserBean result = MAPPER.readerWithView(AdminView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"secret\":\"sssh\",\"password\":\"secret\",\"city\":\"NY\"}");
        assertEquals("alice", result.name);
        assertEquals("sssh", result.secret);
        assertEquals("secret", result.password);
        assertNotNull(result.address);
        assertEquals("NY", result.address.city);
    }

    // Formerly failing: under PublicView the @JsonView(AdminView) "password" must
    // not be populated even though @JsonUnwrapped routes deserialization through
    // deserializeUsingPropertyBasedWithUnwrapped().
    @Test
    public void testRegularPropertyHonorsViewInUnwrappedPath() throws Exception {
        UserBean result = MAPPER.readerWithView(PublicView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"password\":\"BYPASS\",\"city\":\"NY\"}");

        assertEquals("alice", result.name);
        assertNotNull(result.address);
        assertEquals("NY", result.address.city);
        assertNull(result.password,
                "[databind#5969] @JsonView(AdminView) property was populated in PublicView " +
                "via the unwrapped property-based-creator path. password = " + result.password);
    }

    // Formerly failing: under PublicView the @JsonView(AdminView) creator parameter
    // "secret" must not be populated via the creator-property branch of the unwrapped
    // path ([databind#5971]).
    @Test
    public void testCreatorPropertyHonorsViewInUnwrappedPath() throws Exception {
        UserBean result = MAPPER.readerWithView(PublicView.class)
                .forType(UserBean.class)
                .readValue("{\"name\":\"alice\",\"secret\":\"BYPASS\",\"city\":\"NY\"}");

        assertEquals("alice", result.name);
        assertNotNull(result.address);
        assertEquals("NY", result.address.city);
        assertNull(result.secret,
                "[databind#5971] @JsonView(AdminView) creator parameter was populated in PublicView " +
                "via the unwrapped property-based-creator path. secret = " + result.secret);
    }
}
