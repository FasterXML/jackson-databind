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
        final String JSON = a2q("{'a':'foo'}");
        ObjectReader r = MAPPER.readerFor(CreatorWithOptionalStrings.class);

        CreatorWithOptionalStrings bean = r.readValue(JSON);
        assertNotNull(bean);
        assertNotNull(bean.a);
        assertTrue(bean.a.isPresent());

        // 21-Sep-2022, tatu: [databind#3601] Changed this to now become proper
        //    `null`. Should probably also check coercion in future.
        // 15-Oct-2025, tatu: [databind#5335] Revert above change for 3.0.1 to

        //assertNull(bean.b);
        assertEquals(Optional.empty(), bean.b);

        // But can be reconfigured

        bean = r.with(DeserializationFeature.USE_NULL_FOR_MISSING_REFERENCE_VALUES)
                .readValue(JSON);
        assertNotNull(bean);
        assertNotNull(bean.a);
        assertTrue(bean.a.isPresent());

        assertNull(bean.b);
    }
}
