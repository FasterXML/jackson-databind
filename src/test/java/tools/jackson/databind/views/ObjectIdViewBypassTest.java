package tools.jackson.databind.views;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.exc.MismatchedInputException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * {@code @JsonView} was not applied to the Object Id property of a
 * {@code @JsonIdentityInfo(generator = PropertyGenerator.class)} type: the
 * {@link tools.jackson.databind.deser.impl.ObjectIdValueProperty} that replaces
 * the declared id property in the property map did not carry its views, so the
 * id was always bound (and set on the bean) under an active view that excluded it.
 */
public class ObjectIdViewBypassTest extends DatabindTestUtil
{
    static class Public {}
    static class Internal {}

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Account {
        @JsonView(Internal.class) public long id;
        @JsonView(Public.class) public String email;
    }

    static class Accounts {
        @JsonView(Public.class) public List<Account> accounts;
    }

    // Same, but with only the id property having a view: view processing must
    // still be enabled (no other property to trigger it)
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class AccountIdOnlyView {
        @JsonView(Internal.class) public long id;
        public String email;
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    private final String ACCOUNT_DOC = """
            {"id":999,"email":"mallory@evil.test"}
            """;

    // Id property is Internal-only: must not be set when reading under Public view
    @Test
    public void idHiddenByViewNotBound() throws Exception {
        Account result = MAPPER.readerWithView(Public.class)
                .forType(Account.class)
                .readValue(ACCOUNT_DOC);
        assertEquals("mallory@evil.test", result.email);
        assertEquals(0L, result.id, "view-hidden 'id' should not be set but was: " + result.id);
    }

    @Test
    public void idHiddenByViewFailsOnUnexpectedViewProperty() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(DeserializationFeature.FAIL_ON_UNEXPECTED_VIEW_PROPERTIES)
                .build();
        MismatchedInputException e = assertThrows(MismatchedInputException.class,
                () -> mapper.readerWithView(Public.class)
                        .forType(Account.class)
                        .readValue(ACCOUNT_DOC));
        verifyException(e, "Property 'id' is not part of current active view");
    }

    // With default view inclusion enabled, the id's view is the only one declared:
    // it must still be recognized so that view processing is applied at all
    @Test
    public void idHiddenByViewNotBoundWithDefaultInclusion() throws Exception {
        ObjectMapper mapper = jsonMapperBuilder()
                .enable(MapperFeature.DEFAULT_VIEW_INCLUSION)
                .build();
        AccountIdOnlyView result = mapper.readerWithView(Public.class)
                .forType(AccountIdOnlyView.class)
                .readValue(ACCOUNT_DOC);
        assertEquals("mallory@evil.test", result.email);
        assertEquals(0L, result.id, "view-hidden 'id' should not be set but was: " + result.id);
    }

    // Control: id visible in the active view is bound, and still resolves references
    @Test
    public void idVisibleInViewBoundAndResolvesReferences() throws Exception {
        Accounts result = MAPPER.readerWithView(Internal.class)
                .forType(Accounts.class)
                .readValue("""
                        {"accounts":[{"id":1,"email":"a@corp.test"},1]}
                        """);
        assertNull(result.accounts, "'accounts' is Public-only, should not be set");

        Account[] accounts = MAPPER.readerWithView(Internal.class)
                .forType(Account[].class)
                .readValue("""
                        [{"id":1,"email":"a@corp.test"},1]
                        """);
        assertEquals(2, accounts.length);
        assertEquals(1L, accounts[0].id);
        assertNull(accounts[0].email);
        assertSame(accounts[0], accounts[1]);
    }

    // Control: without an active view everything is bound as before
    @Test
    public void noViewBindsAll() throws Exception {
        Account result = MAPPER.readValue(ACCOUNT_DOC, Account.class);
        assertEquals(999L, result.id);
        assertEquals("mallory@evil.test", result.email);
    }
}
