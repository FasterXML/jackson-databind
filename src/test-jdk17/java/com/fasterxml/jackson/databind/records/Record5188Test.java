package com.fasterxml.jackson.databind.records;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonBackReference;
import com.fasterxml.jackson.annotation.JsonManagedReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.fail;

// [databind#5188] Should JsonManagedReference/JsonBackReference for records #5188
public class Record5188Test
    extends DatabindTestUtil
{
    private final ObjectMapper objectMapper = newJsonMapper();

    @Test
    public void testRecordDeserializationFail() throws Exception {
        final String json = "{\"children\":[{}]}";

        try {
            objectMapper.readValue(json, Parent.class);
            fail("Should not pass");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Cannot add back-reference to record type class");
            verifyException(e, "Invalid type definition for");
        }
    }


    record Child(@JsonBackReference Parent parent) {}

    record Parent(@JsonManagedReference List<Child> children) {}

}
