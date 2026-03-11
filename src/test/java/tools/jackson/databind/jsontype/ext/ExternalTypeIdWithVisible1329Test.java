package tools.jackson.databind.jsontype.ext;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#1329]: External property deserialized even if visible = false
 */
public class ExternalTypeIdWithVisible1329Test extends DatabindTestUtil
{
    // Base types for polymorphic deserialization
    static abstract class InviteTo {
        public String name;
    }

    static class InviteToContact extends InviteTo {
        public InviteToContact() { }
        public InviteToContact(String n) { name = n; }
    }

    static class InviteToEmail extends InviteTo {
        public String email;
        public InviteToEmail() { }
        public InviteToEmail(String n, String e) {
            name = n;
            email = e;
        }
    }

    enum InviteKind {
        CONTACT, EMAIL
    }

    // Main class with external type id and visible=false
    // Issue: when visible=false, the external property field should NOT be populated
    static class Invite {
        public InviteKind kind;

        // This field is used as the external property for type information
        // With visible=false, it should NOT be populated during deserialization
        public InviteKind kindForMapper;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kind",
                visible = false)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = InviteToContact.class, name = "CONTACT"),
            @JsonSubTypes.Type(value = InviteToEmail.class, name = "EMAIL")
        })
        public InviteTo to;

        public Invite() { }
        public Invite(InviteKind k, InviteTo t) {
            kind = k;
            to = t;
        }

        @Override
        public String toString() {
            return "Invite(kind=" + kind + ", kindForMapper=" + kindForMapper + ", to=" + to + ")";
        }
    }

    // Alternative version where a separate field exists but should not be visible
    static class Invite2 {
        // This is the main field used for the type id (external property)
        public InviteKind kindForMapper;

        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME,
                include = JsonTypeInfo.As.EXTERNAL_PROPERTY,
                property = "kindForMapper",
                visible = false)
        @JsonSubTypes({
            @JsonSubTypes.Type(value = InviteToContact.class, name = "CONTACT"),
            @JsonSubTypes.Type(value = InviteToEmail.class, name = "EMAIL")
        })
        public InviteTo to;

        public Invite2() { }
        public Invite2(InviteKind k, InviteTo t) {
            kindForMapper = k;
            to = t;
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testExternalTypeIdNotVisibleInTypeInfo() throws Exception
    {
        // When visible=false in @JsonTypeInfo, the external type id property
        // should not be set on the enclosing object, even though "kind" exists in JSON
        String json = a2q("{'kind':'CONTACT','to':{'name':'Foo'}}");

        Invite result = MAPPER.readValue(json, Invite.class);

        assertNotNull(result);
        assertNotNull(result.to);
        assertInstanceOf(InviteToContact.class, result.to);
        assertEquals("Foo", result.to.name);

        // "kind" is both a regular bean property AND the external type id property name.
        // With visible=false, the type id value should be consumed for type resolution
        // and NOT set on the bean.
        assertNull(result.kind,
                "kind should be null when @JsonTypeInfo has visible=false");
        assertNull(result.kindForMapper,
                "kindForMapper should be null when it's not in JSON");
    }

    @Test
    public void testExternalTypeIdWithVisibleFalse() throws Exception
    {
        // When visible=false (the default), the external property used for type id
        // should not be populated in the target object during deserialization
        String json = a2q("{'kindForMapper':'EMAIL','to':{'name':'Bar','email':'test@example.com'}}");

        Invite2 result = MAPPER.readValue(json, Invite2.class);

        assertNotNull(result);
        assertNotNull(result.to);
        assertInstanceOf(InviteToEmail.class, result.to);
        assertEquals("Bar", result.to.name);
        assertEquals("test@example.com", ((InviteToEmail)result.to).email);

        // kindForMapper should remain null because visible=false means it
        // should only be used for type resolution, not populated in the object
        assertNull(result.kindForMapper,
                "kindForMapper should be null when @JsonTypeInfo has visible=false");
    }

    // Verify that EXTERNAL_TYPE_ID_ALWAYS_VISIBLE overrides visible=false
    @Test
    public void testExternalTypeIdAlwaysVisibleFeature() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.EXTERNAL_TYPE_ID_ALWAYS_VISIBLE)
                .build();
        String json = a2q("{'kindForMapper':'EMAIL','to':{'name':'Bar','email':'test@example.com'}}");

        Invite2 result = mapper.readValue(json, Invite2.class);

        assertNotNull(result);
        assertNotNull(result.to);
        assertInstanceOf(InviteToEmail.class, result.to);
        // With EXTERNAL_TYPE_ID_ALWAYS_VISIBLE, the type property IS set even with visible=false
        assertEquals(InviteKind.EMAIL, result.kindForMapper);
    }
}
