package tools.jackson.databind.deser.filter;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class ReadOnlyDeserTest
{
    // [databind#95]
    @JsonIgnoreProperties(value={ "computed" }, allowGetters=true)
    static class ReadOnly95Bean
    {
        public int value = 3;

        public int getComputed() { return 32; }
    }

    static class Person {
        public String name;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private TestEnum testEnum = TestEnum.DEFAULT;

        Person() { }

        protected Person(TestEnum testEnum, String name) {
            this.testEnum = testEnum;
            this.name = name;
        }

        public TestEnum getTestEnum() {
            return testEnum;
        }

        public void setTestEnum(TestEnum testEnum) {
            this.testEnum = testEnum;
        }
   }

   enum TestEnum{
       DEFAULT, TEST;
   }

    // [databind#2719]
    static class UserWithReadOnly {
        @JsonProperty(value = "username", access = JsonProperty.Access.READ_ONLY)
        public String name;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        public String password;
        public String login;
    }

    /*
    /**********************************************************
    /* Test methods, [databind#95]
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#95]
    @Test
    public void testReadOnlyProps95() throws Exception
    {
        ObjectMapper m = new ObjectMapper();
        String json = m.writeValueAsString(new ReadOnly95Bean());
        if (json.indexOf("computed") < 0) {
            fail("Should have property 'computed', didn't: "+json);
        }
        ReadOnly95Bean bean = m.readValue(json, ReadOnly95Bean.class);
        assertNotNull(bean);
    }

    @Test
    public void testDeserializeOneField() throws Exception {
        Person person = MAPPER.readValue("{\"testEnum\":\"\"}", Person.class);
        assertEquals(TestEnum.DEFAULT, person.getTestEnum());
        assertNull(person.name);
    }

    @Test
    public void testDeserializeTwoFields() throws Exception {
        Person person = MAPPER.readValue("{\"testEnum\":\"\",\"name\":\"changyong\"}",
                Person.class);
        assertEquals(TestEnum.DEFAULT, person.getTestEnum());
        assertEquals("changyong", person.name);
    }

    /*
    /**********************************************************
    /* Test methods, [databind#2719]
    /**********************************************************
     */

    @Test
    public void testFailOnIgnore2719() throws Exception
    {
        ObjectReader r = MAPPER.readerFor(UserWithReadOnly.class);

        // First, fine to get 'login'
        UserWithReadOnly result = r.readValue(a2q("{'login':'foo'}"));
        assertEquals("foo", result.login);

        // but not 'password'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            r.readValue(a2q("{'login':'foo', 'password':'bar'}"));
            fail("Should fail");
        } catch (MismatchedInputException e) {
            verifyException(e, "Ignored field");
        }

        // or 'username'
        r = r.with(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES);
        try {
            r.readValue(a2q("{'login':'foo', 'username':'bar'}"));
            fail("Should fail");
        } catch (MismatchedInputException e) {
            verifyException(e, "Ignored field");
        }
    }
}
