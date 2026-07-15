package tools.jackson.databind.views;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.fasterxml.jackson.annotation.JsonView;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @JsonView} filtering was applied on the create paths of
 * {@link tools.jackson.databind.deser.bean.BeanAsArrayDeserializer} (see
 * {@code FailOnUnexpectedView6077Test}) but not on its "update existing value"
 * path ({@code readerForUpdating} / {@code withValueToUpdate}), so a
 * {@code @JsonFormat(shape = ARRAY)} POJO let a view-hidden property be written
 * during an update even when the active view excluded it.
 */
public class POJOAsArrayUpdateViewBypassTest extends DatabindTestUtil
{
    static class Public {}
    static class Internal {}

    @JsonFormat(shape = JsonFormat.Shape.ARRAY)
    @JsonPropertyOrder({ "email", "role" })
    static class Account {
        @JsonView(Public.class) public String email;
        @JsonView(Internal.class) public String role; // admin-only

        public Account() {}
        public Account(String email, String role) {
            this.email = email;
            this.role = role;
        }
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .configure(MapperFeature.DEFAULT_VIEW_INCLUSION, false)
            .build();

    // Update under the Public view must not overwrite the Internal-only property
    @Test
    public void updatePathHonorsView() throws Exception {
        Account existing = new Account("orig@corp.test", "user");

        Account updated = MAPPER.readerWithView(Public.class)
                .forType(Account.class)
                .withValueToUpdate(existing)
                .readValue("[\"mallory@evil.test\",\"ADMIN\"]");

        // email is visible in Public view -> updated
        assertEquals("mallory@evil.test", updated.email);
        // role is Internal-only -> must be left untouched under the Public view
        assertEquals("user", updated.role,
                "view-hidden 'role' should keep its original value but was: " + updated.role);
    }

    // Control: the create path already filters (matches update-path expectation)
    @Test
    public void createPathHonorsView() throws Exception {
        Account created = MAPPER.readerWithView(Public.class)
                .forType(Account.class)
                .readValue("[\"mallory@evil.test\",\"ADMIN\"]");

        assertEquals("mallory@evil.test", created.email);
        assertNull(created.role);
    }

    // Without an active view every position updates as before
    @Test
    public void updateWithoutViewUpdatesAll() throws Exception {
        Account existing = new Account("orig@corp.test", "user");

        Account updated = MAPPER.readerForUpdating(existing)
                .forType(Account.class)
                .readValue("[\"mallory@evil.test\",\"ADMIN\"]");

        assertEquals("mallory@evil.test", updated.email);
        assertEquals("ADMIN", updated.role);
    }

    // With FAIL_ON_UNEXPECTED_VIEW_PROPERTIES the hidden position is an error
    @Test
    public void updateFailsOnHiddenPropertyWhenConfigured() throws Exception {
        Account existing = new Account("orig@corp.test", "user");

        assertThrows(MismatchedInputException.class, () ->
                MAPPER.readerWithView(Public.class)
                        .forType(Account.class)
                        .with(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                        .withValueToUpdate(existing)
                        .readValue("[\"mallory@evil.test\",\"ADMIN\"]"));
    }
}
