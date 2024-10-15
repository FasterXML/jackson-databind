package com.fasterxml.jackson.databind.deser.dos;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadConstraints;
import com.fasterxml.jackson.core.StreamWriteConstraints;
import com.fasterxml.jackson.core.exc.StreamConstraintsException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.fail;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.verifyException;

public class DeepJsonNodeSerTest
{
    private final ObjectMapper NO_LIMITS_MAPPER;
    {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
                .streamWriteConstraints(StreamWriteConstraints.builder().maxNestingDepth(Integer.MAX_VALUE).build())
                .build();
        NO_LIMITS_MAPPER = JsonMapper.builder(jsonFactory).build();
    }

    // 10-Jul-2023, tatu: With recursive implementation there will be practical upper
    //   limit; but we are not testing specific size here but rather that users
    //   may relax/remove limits. For that all we need is level bit higher than
    //   the default there is
    private final int TEST_NESTING = StreamWriteConstraints.DEFAULT_MAX_DEPTH + 100;

    @Test
    public void testDeepNodeSerWithStreamingLimits() throws Exception
    {
        JsonNode jsonNode = NO_LIMITS_MAPPER.readTree(_nestedDoc(TEST_NESTING));
        final ObjectMapper defaultMapper = newJsonMapper();
        try {
            /*String json =*/ defaultMapper.writeValueAsString(jsonNode);
            fail("Should not pass");
        } catch (StreamConstraintsException e) {
            verifyException(e, "Document nesting depth");
            verifyException(e, "exceeds the maximum allowed");
        }
    }

    @Test
    public void testDeepNodeSerNoStreamingLimits() throws Exception
    {
        JsonNode jsonNode = NO_LIMITS_MAPPER.readTree(_nestedDoc(TEST_NESTING));
        String json = NO_LIMITS_MAPPER.writeValueAsString(jsonNode);
        assertNotNull(json);
    }

    private String _nestedDoc(int nesting) {
        StringBuilder jsonString = new StringBuilder(16 * nesting);
        jsonString.append("{");

        for (int i=0; i < nesting; i++) {
          jsonString.append(String.format("\"abc%s\": {", i));
        }

        for (int i=0; i < nesting; i++) {
          jsonString.append("}");
        }

        jsonString.append("}");
        return jsonString.toString();
    }
}
