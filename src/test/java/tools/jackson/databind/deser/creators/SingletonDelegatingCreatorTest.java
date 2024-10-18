package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertSame;

// [databind#4688]
public class SingletonDelegatingCreatorTest extends DatabindTestUtil
{
    static final class NoFieldSingletonWithDelegatingCreator {
        static final NoFieldSingletonWithDelegatingCreator INSTANCE = new NoFieldSingletonWithDelegatingCreator();

        private NoFieldSingletonWithDelegatingCreator() {}

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static NoFieldSingletonWithDelegatingCreator of() {
            return INSTANCE;
        }
    }

    static final class NoFieldSingletonWithPropertiesCreator {
        static final NoFieldSingletonWithPropertiesCreator INSTANCE = new NoFieldSingletonWithPropertiesCreator();

        private NoFieldSingletonWithPropertiesCreator() {}

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static NoFieldSingletonWithPropertiesCreator of() {
            return INSTANCE;
        }
    }

    static final class NoFieldSingletonWithDefaultCreator {
        static final NoFieldSingletonWithDefaultCreator INSTANCE = new NoFieldSingletonWithDefaultCreator();

        private NoFieldSingletonWithDefaultCreator() {}

        @JsonCreator
        static NoFieldSingletonWithDefaultCreator of() {
            return INSTANCE;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testNoFieldSingletonWithDelegatingCreator() throws Exception
    {
        NoFieldSingletonWithDelegatingCreator deserialized = MAPPER.readValue("{}",
                NoFieldSingletonWithDelegatingCreator.class);
        assertSame(NoFieldSingletonWithDelegatingCreator.INSTANCE, deserialized);
    }

    @Test
    public void testNoFieldSingletonWithPropertiesCreator() throws Exception
    {
        NoFieldSingletonWithPropertiesCreator deserialized = MAPPER.readValue("{}",
                NoFieldSingletonWithPropertiesCreator.class);
        assertSame(NoFieldSingletonWithPropertiesCreator.INSTANCE, deserialized);
    }

    @Test
    public void testNoFieldSingletonWithDefaultCreator() throws Exception
    {
        NoFieldSingletonWithDefaultCreator deserialized = MAPPER.readValue("{}",
                NoFieldSingletonWithDefaultCreator.class);
        assertSame(NoFieldSingletonWithDefaultCreator.INSTANCE, deserialized);
    }
}
