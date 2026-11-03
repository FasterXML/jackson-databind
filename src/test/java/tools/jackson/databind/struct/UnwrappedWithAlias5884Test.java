package tools.jackson.databind.struct;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

// [databind#5884] ArrayIndexOutOfBoundsException with @JsonUnwrapped and @JsonAlias
public class UnwrappedWithAlias5884Test extends DatabindTestUtil
{
    @JsonRootName("person")
    static class Person {
        @JsonAlias("n")
        public String name;

        @JsonUnwrapped
        public Job job;
    }

    static class Job {
        public String jobName;
        public String jobAddress;
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(DeserializationFeature.UNWRAP_ROOT_VALUE)
            .build();

    @Test
    public void testUnwrappedWithAlias() throws Exception
    {
        String json = a2q("{'person':{'name':'Alice','jobName':'engineer','jobAddress':'NYC'}}");
        Person person = MAPPER.readValue(json, Person.class);
        assertEquals("Alice", person.name);
        assertNotNull(person.job);
        assertEquals("engineer", person.job.jobName);
        assertEquals("NYC", person.job.jobAddress);
    }

    @Test
    public void testUnwrappedWithAliasUsed() throws Exception
    {
        String json = a2q("{'person':{'n':'Alice','jobName':'engineer','jobAddress':'NYC'}}");
        Person person = MAPPER.readValue(json, Person.class);
        assertEquals("Alice", person.name);
        assertNotNull(person.job);
        assertEquals("engineer", person.job.jobName);
        assertEquals("NYC", person.job.jobAddress);
    }
}
