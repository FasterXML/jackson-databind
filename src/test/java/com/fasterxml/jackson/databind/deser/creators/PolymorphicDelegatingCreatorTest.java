package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.*;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class PolymorphicDelegatingCreatorTest
{
    // For [databind#580]

    @JsonTypeInfo(use = JsonTypeInfo.Id.CLASS)
    static abstract class Issue580Base {
    }

    static class Issue580Impl extends Issue580Base {
        public int id = 3;

        public Issue580Impl() { }
        public Issue580Impl(int id) { this.id = id; }
    }

    static class Issue580Bean {
        public Issue580Base value;

        @JsonCreator
        public Issue580Bean(Issue580Base v) {
            value = v;
        }

        @JsonValue
        public Issue580Base value() {
            return value;
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @Test
    public void testAbstractDelegateWithCreator() throws Exception
    {
        Issue580Bean input = new Issue580Bean(new Issue580Impl(13));
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(input);
        Issue580Bean result = mapper.readValue(json, Issue580Bean.class);
        assertNotNull(result);
        assertNotNull(result.value);
        assertEquals(13, ((Issue580Impl) result.value).id);
    }
}
