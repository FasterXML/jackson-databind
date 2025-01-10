package tools.jackson.databind.ext.jdk8;

import java.util.Optional;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

public class CreatorForOptionalTest
    extends DatabindTestUtil
{
    static class CreatorWithOptionalStrings
    {
        Optional<String> a, b;

        // note: something weird with test setup, should not need annotations
        @JsonCreator
        public CreatorWithOptionalStrings(@JsonProperty("a") Optional<String> a,
                @JsonProperty("b") Optional<String> b)
        {
            this.a = a;
            this.b = b;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    /**
     * Test to ensure that creator parameters use defaulting
     */
    @Test
    public void testCreatorWithOptional() throws Exception
    {
        CreatorWithOptionalStrings bean = MAPPER.readValue(
                a2q("{'a':'foo'}"), CreatorWithOptionalStrings.class);
        assertNotNull(bean);
        assertNotNull(bean.a);
        assertTrue(bean.a.isPresent());

        // 21-Sep-2022, tatu: [databind#3601] Changed this to now become proper
        //    `null`. Should probably also check coercion in future.

        assertNull(bean.b);
    }
}
