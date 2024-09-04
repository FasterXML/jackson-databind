package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static org.junit.jupiter.api.Assertions.assertSame;

// [databind#4688]
public class SingletonDelegatingCreatorTest
{

    static final class NoFieldSingletonWithDelegatingCreator {
        private static final NoFieldSingletonWithDelegatingCreator INSTANCE = new NoFieldSingletonWithDelegatingCreator();

        private NoFieldSingletonWithDelegatingCreator() {}

        @JsonCreator(mode = JsonCreator.Mode.DELEGATING)
        static NoFieldSingletonWithDelegatingCreator of() {
            return INSTANCE;
        }
    }

    static final class NoFieldSingletonWithPropertiesCreator {
        private static final NoFieldSingletonWithPropertiesCreator INSTANCE = new NoFieldSingletonWithPropertiesCreator();

        private NoFieldSingletonWithPropertiesCreator() {}

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static NoFieldSingletonWithPropertiesCreator of() {
            return INSTANCE;
        }
    }

    static final class NoFieldSingletonWithDefaultCreator {
        private static final NoFieldSingletonWithDefaultCreator INSTANCE = new NoFieldSingletonWithDefaultCreator();

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
