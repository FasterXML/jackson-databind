package com.fasterxml.jackson.databind.exc;

import java.util.*;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

// [databind#4316] : NPE when deserializing JsonAnySetter in Throwable
public class ExceptionWithAnySetter4316Test
{
    static class Problem extends Exception {
        private static final long serialVersionUID = 1L;
        @JsonInclude(content=JsonInclude.Include.NON_NULL)
        @JsonAnySetter
        @JsonAnyGetter
        Map<String, Object> additionalProperties = new HashMap<>();
    }

    @JsonIgnoreProperties({ "cause", "stackTrace", "response", "message", "localizedMessage", "suppressed" })
    static class ProblemWithIgnorals extends Exception {
        private static final long serialVersionUID = 1L;

        @JsonAnySetter
        @JsonAnyGetter
        Map<String, Object> additionalProperties = new HashMap<>();
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testWithAnySetter() throws Exception
    {
        Problem problem = new Problem();
        problem.additionalProperties.put("key", "value");
        String json = MAPPER.writeValueAsString(problem);
        Problem result = MAPPER.readValue(json, Problem.class);
        assertEquals(Collections.singletonMap("key", "value"),
                result.additionalProperties);
    }

    // Map with ignored props keys specified in @JsonIgnoreProperties
    @Test
    public void testWithAnySetterAndIgnoralsPut() throws Exception
    {
        // Given
        ProblemWithIgnorals problem = new ProblemWithIgnorals();
        problem.additionalProperties.put("key", "value");
        // Below key-value pairs also ignored from here....
        problem.additionalProperties.put("cause", "ignored");
        problem.additionalProperties.put("stackTrace", "ignored");
        problem.additionalProperties.put("response", "ignored");
        problem.additionalProperties.put("message", "ignored");
        problem.additionalProperties.put("localizedMessage", "ignored");
        problem.additionalProperties.put("suppressed", "ignored");

        // When
        String json = MAPPER.writeValueAsString(problem);
        ProblemWithIgnorals result = MAPPER.readValue(json, ProblemWithIgnorals.class);

        // Then
        assertEquals(Collections.singletonMap("key", "value"),
            result.additionalProperties);
    }

    // With ignorals
    @Test
    public void testWithAnySetterAndIgnoralSimple() throws Exception
    {
        // Given
        ProblemWithIgnorals problem = new ProblemWithIgnorals();
        problem.additionalProperties.put("key", "value");

        // When
        String json = MAPPER.writeValueAsString(problem);
        ProblemWithIgnorals result = MAPPER.readValue(json, ProblemWithIgnorals.class);

        // Then
        assertEquals(Collections.singletonMap("key", "value"),
            result.additionalProperties);
    }

    // With Include.NON_NULL
    @Test
    public void testWithAnySetterButEmptyIncludedFalse() throws Exception
    {
        Problem problem = new Problem();
        problem.additionalProperties.put("exclude", null);
        String json = MAPPER.writeValueAsString(problem);
        Problem result = MAPPER.readValue(json, Problem.class);
        assertEquals(Collections.emptyMap(), result.additionalProperties);
    }
}
