package tools.jackson.databind.views;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.annotation.JsonPOJOBuilder;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Coverage for [databind#6077]: {@code DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES}
 * used to be honored only on the plain field/setter path
 * ({@code BeanDeserializer.deserializeWithView()}). A view-hidden property fed through any
 * other binding path was silently dropped instead of triggering the documented failure.
 *<p>
 * The fix routes every such skip site through
 * {@code BeanDeserializerBase.handleUnexpectedView(...)}. This suite exercises <b>each modified
 * skip site</b> at least once. The nested-class Javadoc names the exact method + condition it
 * pins so a future refactor that moves the site can be re-mapped mechanically:
 *<ul>
 *  <li>{@link Records} -- {@code BeanDeserializer._deserializeUsingPropertyBased} creator arm;</li>
 *  <li>{@link PropertiesCreator} -- same method, regular-property buffering arm (before + after
 *      the creator arg);</li>
 *  <li>{@link RecordUpdate} -- {@code BeanDeserializer._deserializeRecordForUpdate} (the split
 *      view/injection-only condition);</li>
 *  <li>{@link BuilderCreator} -- {@code BuilderBasedDeserializer._deserializeUsingPropertyBased};</li>
 *  <li>{@link SetterUnwrapped} / {@link BuilderUnwrapped} -- the setter/mutator buffering loops
 *      entered when a bean carries an {@code @JsonUnwrapped} member
 *      ({@code deserializeWithUnwrapped} / {@code deserializeUsingPropertyBasedWithUnwrapped});</li>
 *  <li>{@link PropertiesCreatorUnwrapped} -- regular-property arm of
 *      {@code BeanDeserializer.deserializeUsingPropertyBasedWithUnwrapped};</li>
 *  <li>{@link UnwrappedHiddenCreatorParam} -- <i>creator</i> arm of the same method;</li>
 *  <li>{@link ExternalTypeIdSetter} -- {@code BeanDeserializer._deserializeWithExternalTypeId};</li>
 *  <li>{@link ExternalTypeIdCreator} -- both arms of
 *      {@code BeanDeserializer.deserializeUsingPropertyBasedWithExternalTypeId};</li>
 *  <li>{@link BuilderExternalTypeId} -- {@code BuilderBasedDeserializer.deserializeWithExternalTypeId}.</li>
 *</ul>
 *
 * Each fixture asserts the three-way contract, which is also what proves the property really
 * travelled through a {@code handleUnexpectedView} site (that message and the feature fork exist
 * nowhere else):
 *<ol>
 *  <li>the wider view still binds the restricted property (guards against a vacuous pass);</li>
 *  <li>feature ON + hidden property present in input -> {@link MismatchedInputException};</li>
 *  <li>feature OFF -> the property is silently skipped (unchanged legacy behavior).</li>
 *</ol>
 *
 * Deliberately <b>out of scope</b> (see {@link NotCovered}): properties resolved through the
 * {@code ExternalTypeHandler} / {@code UnwrappedPropertyHandler} helper classes, i.e. the
 * {@code @JsonTypeInfo(include = EXTERNAL_PROPERTY)} <i>value</i> itself and {@code @JsonUnwrapped}
 * <i>creator parameters</i>. Those handlers walk buffered declared properties rather than incoming
 * JSON names, so honoring the feature there is a larger change left for a follow-up; the
 * {@link NotCovered} tests pin the current (still-silent) behavior so a future change is a visible,
 * intentional edit rather than a silent regression.
 */
public class FailOnUnexpectedView6077Test extends DatabindTestUtil
{
    static class Public {}
    static class Admin extends Public {}

    // Shared payload hierarchy for the EXTERNAL_PROPERTY fixtures.
    interface Payload {}

    @JsonTypeName("text")
    static class TextPayload implements Payload {
        public String body;
    }

    // DEFAULT_VIEW_INCLUSION differs between 2.x and 3.x defaults; enable it so that
    // fixture properties left without a @JsonView (plain data, not access-controlled) stay
    // visible in every view and don't muddy the assertions.
    private final ObjectMapper FAIL_ON = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .enable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .build();

    private final ObjectMapper FAIL_OFF = jsonMapperBuilder()
            .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
            .disable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
            .build();

    private void expectRejected(Class<?> view, Class<?> type, String json) throws Exception {
        try {
            FAIL_ON.readerWithView(view).forType(type).readValue(json);
            fail("Expected MismatchedInputException for view-hidden property, but parse succeeded");
        } catch (MismatchedInputException e) {
            verifyException(e, "Input mismatch while deserializing");
            verifyException(e, "is not part of current active view");
        }
    }

    private <T> T readWithView(ObjectMapper mapper, Class<?> view, Class<T> type, String json)
        throws Exception
    {
        return mapper.readerWithView(view).forType(type).readValue(json);
    }

    /*
    /**********************************************************************
    /* Property-based creators: records and @JsonCreator(PROPERTIES)
    /**********************************************************************
     */

    /** {@code BeanDeserializer._deserializeUsingPropertyBased} -- creator arm (records bind
     *  every component through the canonical constructor). This is the exact #6077 repro. */
    @Nested
    class Records
    {
        record User(
                @JsonView(Public.class) String name,
                @JsonView(Admin.class) String role) {
        }

        static final String JSON = "{\"name\":\"alice\",\"role\":\"admin\"}";

        @Test
        void adminViewBindsRole() throws Exception {
            User u = readWithView(FAIL_ON, Admin.class, User.class, JSON);
            assertEquals("alice", u.name());
            assertEquals("admin", u.role());
        }

        @Test
        void hiddenRoleRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, User.class, JSON);
        }

        @Test
        void hiddenRoleSkippedWhenDisabled() throws Exception {
            User u = readWithView(FAIL_OFF, Public.class, User.class, JSON);
            assertEquals("alice", u.name());
            assertNull(u.role());
        }
    }

    /** {@code BeanDeserializer._deserializeUsingPropertyBased} -- regular-property arm.
     *  {@code role} is a non-creator property buffered alongside the creator arg; feeding it
     *  both before and after {@code name} exercises the buffering branch in either ordering. */
    @Nested
    class PropertiesCreator
    {
        static class User {
            final String name;
            @JsonView(Admin.class)
            String role;

            @JsonCreator
            User(@JsonProperty("name") @JsonView(Public.class) String name) {
                this.name = name;
            }
        }

        static final String JSON = "{\"name\":\"alice\",\"role\":\"admin\"}";

        @Test
        void adminViewBindsRole() throws Exception {
            User u = readWithView(FAIL_ON, Admin.class, User.class, JSON);
            assertEquals("admin", u.role);
        }

        @Test
        void hiddenRoleBeforeCreatorRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, User.class, "{\"role\":\"admin\",\"name\":\"alice\"}");
        }

        @Test
        void hiddenRoleAfterCreatorRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, User.class, JSON);
        }

        @Test
        void hiddenRoleSkippedWhenDisabled() throws Exception {
            User u = readWithView(FAIL_OFF, Public.class, User.class, JSON);
            assertEquals("alice", u.name);
            assertNull(u.role);
        }
    }

    /** {@code BeanDeserializer._deserializeRecordForUpdate} -- the condition that was split so a
     *  view-hidden creator property fails while an injection-only one still skips silently. */
    @Nested
    class RecordUpdate
    {
        public record User(
                @JsonView(Public.class) String name,
                @JsonView(Admin.class) String role) {
        }

        @Test
        void adminViewOverwritesRole() throws Exception {
            User updated = FAIL_ON.readerWithView(Admin.class)
                    .forType(User.class)
                    .withValueToUpdate(new User("alice", "user"))
                    .readValue("{\"role\":\"admin\"}");
            assertEquals("admin", updated.role());
        }

        @Test
        void hiddenRoleUpdateRejectedWhenEnabled() throws Exception {
            try {
                FAIL_ON.readerWithView(Public.class)
                        .forType(User.class)
                        .withValueToUpdate(new User("alice", "user"))
                        .readValue("{\"role\":\"HACKED\"}");
                fail("Expected MismatchedInputException for view-hidden property, but parse succeeded");
            } catch (MismatchedInputException e) {
                verifyException(e, "Input mismatch while deserializing");
                verifyException(e, "is not part of current active view");
            }
        }

        @Test
        void hiddenRoleUpdateSkippedWhenDisabled() throws Exception {
            User updated = FAIL_OFF.readerWithView(Public.class)
                    .forType(User.class)
                    .withValueToUpdate(new User("alice", "user"))
                    .readValue("{\"role\":\"HACKED\"}");
            assertEquals("user", updated.role());
        }
    }

    /*
    /**********************************************************************
    /* Builder-based property creators
    /**********************************************************************
     */

    /** {@code BuilderBasedDeserializer._deserializeUsingPropertyBased}. */
    @Nested
    class BuilderCreator
    {
        @JsonDeserialize(builder = User.Builder.class)
        static class User {
            final String name;
            final String role;

            User(String name, String role) {
                this.name = name;
                this.role = role;
            }

            @JsonPOJOBuilder(withPrefix = "")
            static class Builder {
                String name;
                String role;

                @JsonCreator
                Builder(@JsonProperty("name") @JsonView(Public.class) String name) {
                    this.name = name;
                }

                @JsonView(Public.class) Builder name(String n) { this.name = n; return this; }
                @JsonView(Admin.class)  Builder role(String r) { this.role = r; return this; }

                User build() { return new User(name, role); }
            }
        }

        static final String JSON = "{\"name\":\"alice\",\"role\":\"admin\"}";

        @Test
        void adminViewBindsRole() throws Exception {
            User u = readWithView(FAIL_ON, Admin.class, User.class, JSON);
            assertEquals("admin", u.role);
        }

        @Test
        void hiddenRoleRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, User.class, JSON);
        }

        @Test
        void hiddenRoleSkippedWhenDisabled() throws Exception {
            User u = readWithView(FAIL_OFF, Public.class, User.class, JSON);
            assertEquals("alice", u.name);
            assertNull(u.role);
        }
    }

    /*
    /**********************************************************************
    /* Buffering paths entered via @JsonUnwrapped
    /*
    /* The @JsonUnwrapped block itself is resolved by UnwrappedPropertyHandler (out of
    /* scope, see NotCovered). What these fixtures verify is that a *co-located* ordinary
    /* property -- one bound by the surrounding buffering loop -- now honors the feature.
    /**********************************************************************
     */

    /** {@code BeanDeserializer.deserializeWithUnwrapped} (setter buffering loop). */
    @Nested
    class SetterUnwrapped
    {
        static class Flags {
            public String tier;
        }

        static class Account {
            String email;
            @JsonView(Admin.class)
            String role;
            Flags flags;

            @JsonView(Public.class) public void setEmail(String e) { this.email = e; }
            @JsonView(Admin.class)  public void setRole(String r)  { this.role = r; }
            @JsonUnwrapped          public void setFlags(Flags f)  { this.flags = f; }
        }

        static final String JSON = "{\"email\":\"e\",\"role\":\"admin\",\"tier\":\"gold\"}";

        @Test
        void adminViewBindsRole() throws Exception {
            Account a = readWithView(FAIL_ON, Admin.class, Account.class, JSON);
            assertEquals("admin", a.role);
            assertNotNull(a.flags);
        }

        @Test
        void hiddenRoleRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Account.class, JSON);
        }

        @Test
        void hiddenRoleSkippedWhenDisabled() throws Exception {
            Account a = readWithView(FAIL_OFF, Public.class, Account.class, JSON);
            assertEquals("e", a.email);
            assertNull(a.role);
        }
    }

    /** {@code BuilderBasedDeserializer.deserializeWithUnwrapped} (builder buffering loop). */
    @Nested
    class BuilderUnwrapped
    {
        static class Flags {
            public String tier;
        }

        @JsonDeserialize(builder = Account.Builder.class)
        static class Account {
            final String email;
            final String role;
            final Flags flags;

            Account(String email, String role, Flags flags) {
                this.email = email;
                this.role = role;
                this.flags = flags;
            }

            @JsonPOJOBuilder(withPrefix = "")
            static class Builder {
                String email;
                String role;
                Flags flags;

                @JsonView(Public.class) Builder email(String e) { this.email = e; return this; }
                @JsonView(Admin.class)  Builder role(String r)  { this.role = r; return this; }
                @JsonUnwrapped          Builder flags(Flags f)  { this.flags = f; return this; }

                Account build() { return new Account(email, role, flags); }
            }
        }

        static final String JSON = "{\"email\":\"e\",\"role\":\"admin\",\"tier\":\"gold\"}";

        @Test
        void adminViewBindsRole() throws Exception {
            Account a = readWithView(FAIL_ON, Admin.class, Account.class, JSON);
            assertEquals("admin", a.role);
            assertNotNull(a.flags);
        }

        @Test
        void hiddenRoleRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Account.class, JSON);
        }

        @Test
        void hiddenRoleSkippedWhenDisabled() throws Exception {
            Account a = readWithView(FAIL_OFF, Public.class, Account.class, JSON);
            assertEquals("e", a.email);
            assertNull(a.role);
        }
    }

    /** {@code BeanDeserializer.deserializeUsingPropertyBasedWithUnwrapped} -- regular-property
     *  arm ({@code role} is buffered while the property-based creator is being assembled). */
    @Nested
    class PropertiesCreatorUnwrapped
    {
        static class Flags {
            public String tier;
        }

        static class Account {
            final String name;
            @JsonView(Admin.class)
            String role;
            @JsonUnwrapped
            Flags flags;

            @JsonCreator
            Account(@JsonProperty("name") @JsonView(Public.class) String name) {
                this.name = name;
            }
        }

        static final String JSON = "{\"name\":\"alice\",\"role\":\"admin\",\"tier\":\"gold\"}";

        @Test
        void adminViewBindsRole() throws Exception {
            Account a = readWithView(FAIL_ON, Admin.class, Account.class, JSON);
            assertEquals("admin", a.role);
            assertNotNull(a.flags);
        }

        @Test
        void hiddenRoleRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Account.class, JSON);
        }

        @Test
        void hiddenRoleSkippedWhenDisabled() throws Exception {
            Account a = readWithView(FAIL_OFF, Public.class, Account.class, JSON);
            assertEquals("alice", a.name);
            assertNull(a.role);
        }
    }

    /** {@code BeanDeserializer.deserializeUsingPropertyBasedWithUnwrapped} -- <i>creator</i> arm.
     *  {@code secret} is a hidden creator parameter (not the unwrapped block), so it flows through
     *  the creator branch of the unwrapped property-based loop rather than the buffering branch. */
    @Nested
    class UnwrappedHiddenCreatorParam
    {
        static class Flags {
            public String tier;
        }

        static class Account {
            final String name;
            final String secret;
            @JsonUnwrapped
            Flags flags;

            @JsonCreator
            Account(@JsonProperty("name") @JsonView(Public.class) String name,
                    @JsonProperty("secret") @JsonView(Admin.class) String secret) {
                this.name = name;
                this.secret = secret;
            }
        }

        static final String JSON = "{\"name\":\"alice\",\"secret\":\"s\",\"tier\":\"gold\"}";

        @Test
        void adminViewBindsSecret() throws Exception {
            Account a = readWithView(FAIL_ON, Admin.class, Account.class, JSON);
            assertEquals("alice", a.name);
            assertEquals("s", a.secret);
            assertNotNull(a.flags);
        }

        @Test
        void hiddenSecretRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Account.class, JSON);
        }

        @Test
        void hiddenSecretSkippedWhenDisabled() throws Exception {
            Account a = readWithView(FAIL_OFF, Public.class, Account.class, JSON);
            assertEquals("alice", a.name);
            assertNull(a.secret);
        }
    }

    /*
    /**********************************************************************
    /* External type id: co-located regular/creator properties
    /*
    /* The EXTERNAL_PROPERTY *value* stays silent (resolved by ExternalTypeHandler, see
    /* NotCovered). These fixtures verify a *co-located* property bound by the surrounding
    /* name loop now honors the feature.
    /**********************************************************************
     */

    /** {@code BeanDeserializer._deserializeWithExternalTypeId} -- non-creator (default ctor)
     *  bean; the hidden {@code secret} is bound by the name loop, the {@code payload} value by
     *  the external-type handler. */
    @Nested
    class ExternalTypeIdSetter
    {
        static class Envelope {
            @JsonView(Public.class)
            public String label;
            @JsonView(Admin.class)
            public String secret;

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
            @JsonSubTypes({ @JsonSubTypes.Type(TextPayload.class) })
            public Payload payload;
        }

        static final String JSON =
                "{\"label\":\"hi\",\"secret\":\"s\",\"kind\":\"text\",\"payload\":{\"body\":\"b\"}}";

        @Test
        void adminViewBindsSecret() throws Exception {
            Envelope e = readWithView(FAIL_ON, Admin.class, Envelope.class, JSON);
            assertEquals("s", e.secret);
            assertNotNull(e.payload);
        }

        @Test
        void hiddenSecretRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Envelope.class, JSON);
        }

        @Test
        void hiddenSecretSkippedWhenDisabled() throws Exception {
            Envelope e = readWithView(FAIL_OFF, Public.class, Envelope.class, JSON);
            assertEquals("hi", e.label);
            assertNull(e.secret);
            assertNotNull(e.payload);
        }
    }

    /** {@code BeanDeserializer.deserializeUsingPropertyBasedWithExternalTypeId} -- both arms:
     *  a hidden creator parameter ({@code secret}) and a hidden regular property ({@code note}). */
    @Nested
    class ExternalTypeIdCreator
    {
        static class Envelope {
            final String label;
            final String secret;
            @JsonView(Admin.class)
            public String note;

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
            @JsonSubTypes({ @JsonSubTypes.Type(TextPayload.class) })
            public Payload payload;

            @JsonCreator
            Envelope(@JsonProperty("label") @JsonView(Public.class) String label,
                    @JsonProperty("secret") @JsonView(Admin.class) String secret) {
                this.label = label;
                this.secret = secret;
            }
        }

        static final String PAYLOAD = "\"kind\":\"text\",\"payload\":{\"body\":\"b\"}";

        @Test
        void adminViewBindsHiddenProps() throws Exception {
            Envelope e = readWithView(FAIL_ON, Admin.class, Envelope.class,
                    "{\"label\":\"hi\",\"secret\":\"s\",\"note\":\"n\"," + PAYLOAD + "}");
            assertEquals("s", e.secret);
            assertEquals("n", e.note);
            assertNotNull(e.payload);
        }

        // Hidden creator parameter -> creator arm.
        @Test
        void hiddenCreatorParamRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Envelope.class,
                    "{\"label\":\"hi\",\"secret\":\"s\"," + PAYLOAD + "}");
        }

        // Hidden regular property -> buffering arm.
        @Test
        void hiddenRegularPropRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Envelope.class,
                    "{\"label\":\"hi\",\"note\":\"n\"," + PAYLOAD + "}");
        }

        @Test
        void hiddenPropsSkippedWhenDisabled() throws Exception {
            Envelope e = readWithView(FAIL_OFF, Public.class, Envelope.class,
                    "{\"label\":\"hi\",\"secret\":\"s\",\"note\":\"n\"," + PAYLOAD + "}");
            assertEquals("hi", e.label);
            assertNull(e.secret);
            assertNull(e.note);
            assertNotNull(e.payload);
        }
    }

    /** {@code BuilderBasedDeserializer.deserializeWithExternalTypeId} (default-ctor builder). */
    @Nested
    class BuilderExternalTypeId
    {
        @JsonDeserialize(builder = Envelope.Builder.class)
        static class Envelope {
            final String label;
            final String secret;
            final Payload payload;

            Envelope(String label, String secret, Payload payload) {
                this.label = label;
                this.secret = secret;
                this.payload = payload;
            }

            @JsonPOJOBuilder(withPrefix = "")
            static class Builder {
                String label;
                String secret;
                Payload payload;

                @JsonView(Public.class) Builder label(String l) { this.label = l; return this; }
                @JsonView(Admin.class)  Builder secret(String s) { this.secret = s; return this; }

                @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                        include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
                @JsonSubTypes({ @JsonSubTypes.Type(TextPayload.class) })
                Builder payload(Payload p) { this.payload = p; return this; }

                Envelope build() { return new Envelope(label, secret, payload); }
            }
        }

        static final String JSON =
                "{\"label\":\"hi\",\"secret\":\"s\",\"kind\":\"text\",\"payload\":{\"body\":\"b\"}}";

        @Test
        void adminViewBindsSecret() throws Exception {
            Envelope e = readWithView(FAIL_ON, Admin.class, Envelope.class, JSON);
            assertEquals("s", e.secret);
            assertNotNull(e.payload);
        }

        @Test
        void hiddenSecretRejectedWhenEnabled() throws Exception {
            expectRejected(Public.class, Envelope.class, JSON);
        }

        @Test
        void hiddenSecretSkippedWhenDisabled() throws Exception {
            Envelope e = readWithView(FAIL_OFF, Public.class, Envelope.class, JSON);
            assertEquals("hi", e.label);
            assertNull(e.secret);
            assertNotNull(e.payload);
        }
    }

    /*
    /**********************************************************************
    /* Known limitations: paths NOT covered by the fix
    /*
    /* These pin the current behavior (silent skip even when the feature is enabled) so
    /* that a later fix of the handler-class paths shows up as an intentional change here.
    /**********************************************************************
     */

    @Nested
    class NotCovered
    {
        // (a) External type id VALUE itself: resolved by ExternalTypeHandler, never reaches the
        // name-loop view check.
        static class Envelope {
            @JsonView(Public.class)
            public String label;

            @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                    include = JsonTypeInfo.As.EXTERNAL_PROPERTY, property = "kind")
            @JsonSubTypes({ @JsonSubTypes.Type(TextPayload.class) })
            @JsonView(Admin.class)
            public Payload payload;
        }

        static final String ENVELOPE_JSON =
                "{\"label\":\"hi\",\"kind\":\"text\",\"payload\":{\"body\":\"x\"}}";

        @Test
        void externalTypeIdValueStillSilentlySkippedWhenEnabled() throws Exception {
            // Admin-only "payload" is hidden under Public view; feature is ON, yet it is
            // dropped rather than rejected -- this path does not (yet) honor the feature.
            Envelope e = readWithView(FAIL_ON, Public.class, Envelope.class, ENVELOPE_JSON);
            assertEquals("hi", e.label);
            assertNull(e.payload);
        }

        // (b) @JsonUnwrapped creator parameter: resolved by UnwrappedPropertyHandler.
        static class Address {
            @JsonView(Admin.class)
            public String street;
        }

        static class Person {
            final String name;
            final Address address;

            @JsonCreator
            Person(@JsonProperty("name") @JsonView(Public.class) String name,
                    @JsonView(Admin.class) @JsonUnwrapped Address address) {
                this.name = name;
                this.address = address;
            }
        }

        static final String PERSON_JSON = "{\"name\":\"alice\",\"street\":\"1 Main\"}";

        @Test
        void unwrappedCreatorParamStillSilentlySkippedWhenEnabled() throws Exception {
            Person p = readWithView(FAIL_ON, Public.class, Person.class, PERSON_JSON);
            assertEquals("alice", p.name);
            // Address is admin-only; under Public the whole unwrapped parameter is left
            // unbound (null) even with the feature ON, rather than rejected.
            assertNull(p.address);
        }
    }
}
