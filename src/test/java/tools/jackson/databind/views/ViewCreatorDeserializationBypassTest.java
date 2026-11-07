package tools.jackson.databind.views;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Verifies that an active {@code @JsonView} is honored across every
 * property-based-creator deserialization path -- i.e. that a
 * {@code @JsonView(Admin)} property cannot be populated while reading under a
 * less-privileged view by reordering JSON fields or by routing through a
 * specialized code path.
 *<p>
 * Consolidates the per-path regression tests for [databind#5969] / [databind#5971]
 * and their builder/external-type-id/record-update siblings.
 * Each {@link Nested} class targets one deserialization
 * path; all share the {@code PublicView}/{@code AdminView} hierarchy and the same
 * "negative control + bypass" structure.
 */
public class ViewCreatorDeserializationBypassTest extends DatabindTestUtil
{
    static class PublicView {}
    static class AdminView extends PublicView {}

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * [databind#5969]: regression where setterless collection properties enrolled
     * in the merging buffer path of {@code BeanDeserializer._deserializeUsingPropertyBased()}
     * skipped the {@code visibleInView} check.
     */
    @Nested
    class SetterlessCreator5969
    {
        static class CreatorBean {
            @JsonView(PublicView.class)
            private String name;

            // Setterless collection -> SetterlessProperty; @JsonView(Admin) means
            // it must be invisible in PublicView.
            @JsonView(AdminView.class)
            private final List<String> roles = new ArrayList<>();

            @JsonCreator
            public CreatorBean(@JsonProperty("name") @JsonView(PublicView.class) String name) {
                this.name = name;
            }

            @JsonView(PublicView.class)
            public String getName() { return name; }

            @JsonView(AdminView.class)
            public List<String> getRoles() { return roles; }
        }

        @Test
        public void adminViewPopulatesBoth() throws Exception {
            CreatorBean result = MAPPER.readerWithView(AdminView.class)
                    .forType(CreatorBean.class)
                    .readValue("""
                            {"name":"alice","roles":["admin"]}
                            """);
            assertEquals("alice", result.getName());
            assertEquals(List.of("admin"), result.getRoles());
        }

        // roles BEFORE the creator property "name" forces the regular-property buffering path.
        @Test
        public void rolesBeforeCreatorHonorsView() throws Exception {
            CreatorBean result = MAPPER.readerWithView(PublicView.class)
                    .forType(CreatorBean.class)
                    .readValue("""
                            {"roles":["admin"],"name":"alice"}
                            """);
            assertEquals("alice", result.getName());
            assertTrue(result.getRoles().isEmpty(),
                    "[databind#5969] setterless @JsonView(AdminView) populated in PublicView: " + result.getRoles());
        }

        @Test
        public void rolesAfterCreatorHonorsView() throws Exception {
            CreatorBean result = MAPPER.readerWithView(PublicView.class)
                    .forType(CreatorBean.class)
                    .readValue("""
                            {"name":"alice","roles":["admin"]}
                            """);
            assertEquals("alice", result.getName());
            assertTrue(result.getRoles().isEmpty(),
                    "[databind#5969] setterless @JsonView(AdminView) populated in PublicView: " + result.getRoles());
        }
    }

    /**
     * [databind#5971]: {@code UnwrappedPropertyHandler.processUnwrappedCreatorProperties()}
     * iterated creator properties without checking {@code visibleInView}, so an
     * {@code @JsonView(Admin)} {@code @JsonUnwrapped} constructor parameter could be
     * populated under a less-privileged view.
     */
    @Nested
    class UnwrappedCreatorParam5971
    {
        static class Address {
            @JsonView(AdminView.class)
            public String street;
            @JsonView(PublicView.class)
            public String city;
        }

        static class UserBean {
            @JsonView(PublicView.class)
            public final String name;
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

        @Test
        public void adminViewPopulatesBoth() throws Exception {
            UserBean result = MAPPER.readerWithView(AdminView.class)
                    .forType(UserBean.class)
                    .readValue("""
                            {"name":"alice","street":"1 Main St","city":"Springfield"}
                            """);
            assertEquals("alice", result.name);
            assertNotNull(result.address);
            assertEquals("1 Main St", result.address.street);
            assertEquals("Springfield", result.address.city);
        }

        @Test
        public void unwrappedCreatorParamHonorsView() throws Exception {
            UserBean result = MAPPER.readerWithView(PublicView.class)
                    .forType(UserBean.class)
                    .readValue("""
                            {"name":"alice","street":"1 Main St","city":"Springfield"}
                            """);
            assertEquals("alice", result.name);
            assertNull(result.address,
                    "[databind#5971] @JsonView(AdminView) @JsonUnwrapped creator param populated in PublicView: "
                    + result.address);
        }
    }

    /**
     * GHSA-x94j-jhxw-j455 (builder path): {@code BuilderBasedDeserializer._deserializeUsingPropertyBased()}
     * skipped the view check on the regular-property buffering branch, so a
     * {@code @JsonView(Admin)} property ordered between the creator's parameters
     * could be populated under a non-Admin view ([databind#5969] sibling).
     */
    @Nested
    class BuilderBased
    {
        @JsonDeserialize(builder = User.Builder.class)
        static class User {
            @JsonView(PublicView.class) public final String name;
            @JsonView(PublicView.class) public final String city;
            @JsonView(AdminView.class)  public final String password;

            private User(String n, String c, String p) {
                this.name = n; this.city = c; this.password = p;
            }

            @JsonPOJOBuilder(withPrefix = "")
            static class Builder {
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

        @Test
        public void adminViewPopulatesAll() throws Exception {
            User u = MAPPER.readerWithView(AdminView.class).forType(User.class)
                    .readValue("""
                            {"name":"alice","password":"secret","city":"NY"}
                            """);
            assertEquals("alice", u.name);
            assertEquals("NY", u.city);
            assertEquals("secret", u.password);
        }

        // password BETWEEN the two creator props -> regular-property buffering branch.
        @Test
        public void regularPropertyInCreatorWindowHonorsView() throws Exception {
            User u = MAPPER.readerWithView(PublicView.class).forType(User.class)
                    .readValue("""
                            {"name":"alice","password":"BYPASS","city":"NY"}
                            """);
            assertEquals("alice", u.name);
            assertEquals("NY", u.city);
            assertNull(u.password,
                    "[databind#5969] @JsonView(AdminView) builder property populated in PublicView (creator window): "
                    + u.password);
        }

        @Test
        public void regularPropertyAfterCreatorHonorsView() throws Exception {
            User u = MAPPER.readerWithView(PublicView.class).forType(User.class)
                    .readValue("""
                            {"name":"alice","city":"NY","password":"BYPASS"}
                            """);
            assertEquals("alice", u.name);
            assertEquals("NY", u.city);
            assertNull(u.password,
                    "[databind#5969] @JsonView(AdminView) builder property populated in PublicView: " + u.password);
        }
    }

    /**
     * GHSA-x94j-jhxw-j455 (BeanDeserializer unwrapped path):
     * {@code deserializeUsingPropertyBasedWithUnwrapped()} skipped the view check on
     * both the creator-property ([databind#5971]) and regular-property
     * ([databind#5969]) branches when an {@code @JsonUnwrapped} member forced that path.
     */
    @Nested
    class UnwrappedPojo
    {
        static class Address {
            @JsonView(PublicView.class)
            public String city;
        }

        static class UserBean {
            @JsonView(PublicView.class)
            public final String name;
            // Creator parameter, view-restricted -> creator branch ([databind#5971]).
            @JsonView(AdminView.class)
            public final String secret;
            // Non-creator, view-restricted -> regular-property branch ([databind#5969]).
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

        @Test
        public void adminViewPopulatesAll() throws Exception {
            UserBean result = MAPPER.readerWithView(AdminView.class).forType(UserBean.class)
                    .readValue("""
                            {"name":"alice","secret":"sssh","password":"secret","city":"NY"}
                            """);
            assertEquals("alice", result.name);
            assertEquals("sssh", result.secret);
            assertEquals("secret", result.password);
            assertNotNull(result.address);
            assertEquals("NY", result.address.city);
        }

        @Test
        public void regularPropertyHonorsView() throws Exception {
            UserBean result = MAPPER.readerWithView(PublicView.class).forType(UserBean.class)
                    .readValue("""
                            {"name":"alice","password":"BYPASS","city":"NY"}
                            """);
            assertEquals("alice", result.name);
            assertNotNull(result.address);
            assertEquals("NY", result.address.city);
            assertNull(result.password,
                    "[databind#5969] @JsonView(AdminView) property populated in PublicView (unwrapped path): "
                    + result.password);
        }

        @Test
        public void creatorPropertyHonorsView() throws Exception {
            UserBean result = MAPPER.readerWithView(PublicView.class).forType(UserBean.class)
                    .readValue("""
                            {"name":"alice","secret":"BYPASS","city":"NY"}
                            """);
            assertEquals("alice", result.name);
            assertNotNull(result.address);
            assertEquals("NY", result.address.city);
            assertNull(result.secret,
                    "[databind#5971] @JsonView(AdminView) creator param populated in PublicView (unwrapped path): "
                    + result.secret);
        }
    }

    /**
     * GHSA-x94j-jhxw-j455 (external-type-id path):
     * {@code deserializeUsingPropertyBasedWithExternalTypeId()} skipped the view check
     * on creator-property ([databind#5971]) and regular-property ([databind#5969])
     * branches. (The nested polymorphic value content is not deep-checked: its binding
     * under property-based creators is an unrelated existing quirk.)
     */
    @Nested
    class ExternalTypeId
    {
        static final String FULL_JSON = """
                {"name":"alice","secret":"sssh","password":"pw","valueType":"string","value":{"v":"x"}}
                """;

        public interface Value { }

        @JsonTypeName("string")
        static class StringValue implements Value {
            public String v;
        }

        static class Envelope {
            @JsonView(PublicView.class)
            public final String name;
            @JsonView(AdminView.class)
            public final String secret;
            @JsonView(AdminView.class)
            public String password;

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

        @Test
        public void adminViewPopulatesRestricted() throws Exception {
            Envelope result = MAPPER.readerWithView(AdminView.class).forType(Envelope.class)
                    .readValue(FULL_JSON);
            assertEquals("alice", result.name);
            assertEquals("sssh", result.secret);
            assertEquals("pw", result.password);
            assertTrue(result.value instanceof StringValue);
        }

        @Test
        public void regularPropertyHonorsView() throws Exception {
            Envelope result = MAPPER.readerWithView(PublicView.class).forType(Envelope.class)
                    .readValue(FULL_JSON);
            assertEquals("alice", result.name);
            assertTrue(result.value instanceof StringValue);
            assertNull(result.password,
                    "[databind#5969] @JsonView(AdminView) property populated in PublicView (external-type-id): "
                    + result.password);
        }

        @Test
        public void creatorPropertyHonorsView() throws Exception {
            Envelope result = MAPPER.readerWithView(PublicView.class).forType(Envelope.class)
                    .readValue(FULL_JSON);
            assertEquals("alice", result.name);
            assertTrue(result.value instanceof StringValue);
            assertNull(result.secret,
                    "[databind#5971] @JsonView(AdminView) creator param populated in PublicView (external-type-id): "
                    + result.secret);
        }
    }

    /**
     * [databind#5971] / [databind#5966] (record-update path):
     * {@code BeanDeserializer._deserializeRecordForUpdate()} (new in 3.1) must not
     * overwrite a {@code @JsonView(Admin)} record component from JSON while updating
     * under a less-privileged view.
     */
    public record UserRecord(
            @JsonView(PublicView.class) String name,
            @JsonView(AdminView.class) String role) {
    }

    @Nested
    class RecordUpdate
    {
        @Test
        public void adminViewAllowsOverride() throws Exception {
            UserRecord existing = new UserRecord("alice", "user");
            UserRecord result = MAPPER.readerWithView(AdminView.class)
                    .forType(UserRecord.class)
                    .withValueToUpdate(existing)
                    .readValue("""
                            {"name":"alice2","role":"admin"}
                            """);
            assertEquals("alice2", result.name());
            assertEquals("admin", result.role());
        }

        @Test
        public void creatorPropertyHonorsViewOnUpdate() throws Exception {
            UserRecord existing = new UserRecord("alice", "user");
            UserRecord result = MAPPER.readerWithView(PublicView.class)
                    .forType(UserRecord.class)
                    .withValueToUpdate(existing)
                    .readValue("""
                            {"name":"alice2","role":"HACKED"}
                            """);
            assertEquals("alice2", result.name());
            assertEquals("user", result.role(),
                    "[databind#5971] @JsonView(AdminView) record component overwritten in PublicView (record-update): "
                    + result.role());
        }
    }
}
