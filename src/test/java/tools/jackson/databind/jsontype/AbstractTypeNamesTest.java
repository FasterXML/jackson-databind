package tools.jackson.databind.jsontype;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.*;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Unit tests for checking how combination of interfaces, implementation
 * classes are handled, with respect to type names.
 */
public class AbstractTypeNamesTest  extends DatabindTestUtil
{
    @JsonTypeName("Employee")
    public interface Employee extends User {
        public abstract String getEmployer();
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="userType")
    @JsonTypeName("User")
    @JsonSubTypes({ @JsonSubTypes.Type(value=Employee.class,name="Employee") })
    public interface User {
            public abstract String getName();
            public abstract List<User> getFriends();
    }

    @JsonTypeName("Employee")
    static class DefaultEmployee extends DefaultUser implements Employee
    {
        private String _employer;

        @JsonCreator
        public DefaultEmployee(@JsonProperty("name") String name,
                @JsonProperty("friends") List<User> friends,
                @JsonProperty("employer") String employer) {
            super(name, friends);
            _employer = employer;
        }

        @Override
        public String getEmployer() {
            return _employer;
        }
    }

    @JsonTypeInfo(use=Id.NAME, include=As.PROPERTY, property="userType")
    @JsonTypeName("User")
    @JsonSubTypes({ @JsonSubTypes.Type(value=DefaultEmployee.class,name="Employee") })
    static class DefaultUser implements User
    {
        private String _name;
        private List<User> _friends;

        @JsonCreator
        public DefaultUser(@JsonProperty("name") String name,
                @JsonProperty("friends") List<User> friends)
        {
            super();
            _name = name;
            _friends = friends;
        }

        @Override
        public String getName() {
            return _name;
        }

        @Override
        public List<User> getFriends() {
            return _friends;
        }
    }

    // [databind#1186]
    public interface IContainer<T> {
        @JsonProperty("ts")
        List<T> getTs();
    }

    static class MyContainer<T> implements IContainer<T> {

        final List<T> ts;

        @JsonCreator
        public MyContainer(@JsonProperty("ts") List<T> ts) {
            this.ts = ts;
        }

        @Override
        public List<T> getTs() {
            return ts;
        }
    }

    public static class MyObject {
        public String msg;
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    @Test
    public void testEmptyCollection() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .configure(SerializationFeature.INDENT_OUTPUT, true)
                .build();
        List<User>friends = new ArrayList<User>();
        friends.add(new DefaultUser("Joe Hildebrandt", null));
        friends.add(new DefaultEmployee("Richard Nasr",null,"MDA"));

        User user = new DefaultEmployee("John Vanspronssen", friends, "MDA");
        String json = mapper.writeValueAsString(user);

        // 24-Feb-2011, tatu: For now let's simply require registration of
        //   concrete subtypes; can't think of a way to avoid that for now
        mapper = jsonMapperBuilder()
                .registerSubtypes(DefaultEmployee.class,
                        DefaultUser.class)
                .build();

        User result = mapper.readValue(json, User.class);
        assertNotNull(result);
        assertEquals(DefaultEmployee.class, result.getClass());

        friends = result.getFriends();
        assertEquals(2, friends.size());
        assertEquals(DefaultUser.class, friends.get(0).getClass());
        assertEquals(DefaultEmployee.class, friends.get(1).getClass());
    }

    // [databind#1186]
    @Test
    public void testDeserializeMyContainer() throws Exception {
        SimpleModule module = new SimpleModule().addAbstractTypeMapping(IContainer.class, MyContainer.class);
        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();
        String json = "{\"ts\": [ { \"msg\": \"hello\"} ] }";
        final Object o = mapper.readValue(json,
                mapper.getTypeFactory().constructParametricType(IContainer.class, MyObject.class));
        assertEquals(MyContainer.class, o.getClass());
        MyContainer<?> myc = (MyContainer<?>) o;
        assertEquals(1, myc.ts.size());
        Object value = myc.ts.get(0);
        assertEquals(MyObject.class, value.getClass());
    }
}
