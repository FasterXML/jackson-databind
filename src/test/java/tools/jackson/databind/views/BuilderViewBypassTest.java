package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * [databind#5969]: @JsonView bypass for builder-based POJOs using a
 * property-based creator.
 *
 * The active-view check was added to {@code BeanDeserializer} but the parallel
 * branch in {@code BuilderBasedDeserializer._deserializeUsingPropertyBased()}
 * was left unpatched. A regular (non-creator) property annotated
 * {@code @JsonView(Admin)} could be populated under a non-Admin view when ordered
 * between the creator's first and last parameters (forcing the regular-property
 * buffering path).
 */
public class BuilderViewBypassTest extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    @JsonDeserialize(builder = User.Builder.class)
    public static class User {
        @JsonView(PublicView.class) public final String name;
        @JsonView(PublicView.class) public final String city;
        @JsonView(AdminView.class)  public final String password;

        private User(String n, String c, String p) {
            this.name = n; this.city = c; this.password = p;
        }

        @JsonPOJOBuilder(withPrefix = "")
        public static class Builder {
            private String name, city, password;

            @JsonCreator
            public Builder(@JsonProperty("name") @JsonView(PublicView.class) String n,
                    @JsonProperty("city") @JsonView(PublicView.class) String c) {
                this.name = n; this.city = c;
            }
            @JsonView(PublicView.class) public Builder name(String n)     { this.name = n; return this; }
            @JsonView(PublicView.class) public Builder city(String c)     { this.city = c; return this; }
            @JsonView(AdminView.class)  public Builder password(String p) { this.password = p; return this; }
            public User build() { return new User(name, city, password); }
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // Negative control: AdminView populates all properties.
    @Test
    public void testAdminView_negativeControl() throws Exception {
        User u = MAPPER.readerWithView(AdminView.class)
                .forType(User.class)
                .readValue("{\"name\":\"alice\",\"password\":\"secret\",\"city\":\"NY\"}");
        assertEquals("alice", u.name);
        assertEquals("NY", u.city);
        assertEquals("secret", u.password);
    }

    // Formerly failing case: "password" ordered BETWEEN the two creator props,
    // forcing the regular-property buffering branch. Under PublicView the
    // @JsonView(AdminView) property must remain null.
    @Test
    public void testRegularPropertyHonorsViewInCreatorWindow() throws Exception {
        User u = MAPPER.readerWithView(PublicView.class)
                .forType(User.class)
                .readValue("{\"name\":\"alice\",\"password\":\"BYPASS\",\"city\":\"NY\"}");

        assertEquals("alice", u.name);
        assertEquals("NY", u.city);
        assertNull(u.password,
                "[databind#5969] @JsonView(AdminView) builder property was populated " +
                "in PublicView via the creator-window buffering path. password = " + u.password);
    }

    // Control: password ordered AFTER both creator props was already view-aware
    // (deserialization shifts to deserializeWithView once the creator completes).
    @Test
    public void testRegularPropertyHonorsViewAfterCreator() throws Exception {
        User u = MAPPER.readerWithView(PublicView.class)
                .forType(User.class)
                .readValue("{\"name\":\"alice\",\"city\":\"NY\",\"password\":\"BYPASS\"}");

        assertEquals("alice", u.name);
        assertEquals("NY", u.city);
        assertNull(u.password,
                "[databind#5969] @JsonView(AdminView) builder property was populated " +
                "in PublicView. password = " + u.password);
    }
}
