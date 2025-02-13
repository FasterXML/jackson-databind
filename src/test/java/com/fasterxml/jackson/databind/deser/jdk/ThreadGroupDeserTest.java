package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class ThreadGroupDeserTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = sharedMapper();

    // [databind#4939]
    @Test
    public void deserThreadGroupFromEmpty() throws Exception {
        ThreadGroup tg = MAPPER.readValue("{}", ThreadGroup.class);
        assertNotNull(tg);
    }

    @Test
    public void roundtripThreadGroup() throws Exception {
        ThreadGroup tg = new ThreadGroup("testTG");
        String json = MAPPER.writeValueAsString(tg);
        ThreadGroup tg2 = MAPPER.readValue(json, ThreadGroup.class);
        assertNotNull(tg2);
        assertEquals(tg.getName(), tg2.getName());
    }
}
