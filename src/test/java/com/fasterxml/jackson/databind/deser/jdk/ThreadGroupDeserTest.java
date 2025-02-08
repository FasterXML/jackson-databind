package com.fasterxml.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

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
}
