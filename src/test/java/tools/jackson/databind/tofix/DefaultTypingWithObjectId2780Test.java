package tools.jackson.databind.tofix;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;
import tools.jackson.databind.testutil.failure.JacksonTestFailureExpected;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#2780]: Deserialization with Default Typing (PTH) and
 * {@code @JsonIdentityInfo} in untyped collections.
 *<p>
 * When a container object appears before its referenced object in an untyped
 * collection, the referenced object is embedded inline in the container with
 * full type info. Any later occurrence of that object in the collection is
 * serialized as a bare object-id back-reference without type info. On
 * deserialization Jackson cannot resolve the bare id back to the original
 * object and instead produces an {@code Integer}.
 */
public class DefaultTypingWithObjectId2780Test extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class User {
        public int id;
        public String login;

        public User() {}
        public User(int id, String login) {
            this.id = id;
            this.login = login;
        }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class UserContainer {
        public int id;
        public User user;

        public UserContainer() {}
        public UserContainer(int id, User user) {
            this.id = id;
            this.user = user;
        }
    }

    private ObjectMapper defaultTypingMapper() {
        return jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance, DefaultTyping.NON_FINAL)
            .build();
    }

    // User appears first: serialized with full type/field info.
    // Container appears second: its "user" field is a bare id back-reference.
    // Deserialization resolves that reference correctly -- this case works fine.
    @Test
    public void testUserFirstThenContainer() throws Exception
    {
        ObjectMapper mapper = defaultTypingMapper();

        User user = new User(42, "cool_man");
        UserContainer container = new UserContainer(1, user);

        List<Object> list = new ArrayList<>();
        list.add(user);
        list.add(container);

        String json = mapper.writeValueAsString(list);

        List<?> result = mapper.readValue(json, List.class);

        assertEquals(2, result.size());
        assertInstanceOf(User.class, result.get(0), "First element should be User");
        assertInstanceOf(UserContainer.class, result.get(1), "Second element should be UserContainer");

        User resultUser = (User) result.get(0);
        UserContainer resultContainer = (UserContainer) result.get(1);

        assertEquals(42, resultUser.id);
        assertEquals("cool_man", resultUser.login);
        assertSame(resultUser, resultContainer.user,
            "Back-reference in container should point to the same User instance");
    }

    // Container appears first: User is embedded inline inside it.
    // User then appears second in the list as a bare object-id (e.g. just 42) with no
    // type wrapper, because the serializer already emitted the full object above.
    // On deserialization the bare id lacks type info and is read as Integer instead
    // of being resolved to the User -- this is the bug described in #2780.
    @JacksonTestFailureExpected
    @Test
    public void testContainerFirstThenUser() throws Exception
    {
        ObjectMapper mapper = defaultTypingMapper();

        User user = new User(42, "cool_man");
        UserContainer container = new UserContainer(1, user);

        List<Object> list = new ArrayList<>();
        list.add(container);
        list.add(user);

        String json = mapper.writeValueAsString(list);

        List<?> result = mapper.readValue(json, List.class);

        assertEquals(2, result.size());
        assertInstanceOf(UserContainer.class, result.get(0),
            "First element should be UserContainer");
        assertInstanceOf(User.class, result.get(1),
            "Second element should be User (not Integer) -- bare id back-reference must be resolved with type info");

        UserContainer resultContainer = (UserContainer) result.get(0);
        User resultUser = (User) result.get(1);

        assertEquals(42, resultUser.id);
        assertEquals("cool_man", resultUser.login);
        assertSame(resultUser, resultContainer.user,
            "Container's user field and the list's second element should be the same instance");
    }
}
