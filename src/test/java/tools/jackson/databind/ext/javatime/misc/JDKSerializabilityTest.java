package tools.jackson.databind.ext.javatime.misc;

import java.io.*;
import java.time.Year;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.ext.javatime.*;

import static org.junit.jupiter.api.Assertions.*;

public class JDKSerializabilityTest extends DateTimeTestBase
{
    @Test
    public void testJDKSerializability() throws Exception {
        final Year input = Year.of(1986);
        ObjectMapper mapper = newMapper();
        String json1 = mapper.writeValueAsString(input);

        // validate we can still use it to deserialize jackson objects
        ObjectMapper thawedMapper = serializeAndDeserialize(mapper);
        String json2 = thawedMapper.writeValueAsString(input);

        assertEquals(json1, json2);

        Year result = thawedMapper.readValue(json1, Year.class);
        assertEquals(input, result);
    }

    private ObjectMapper serializeAndDeserialize(ObjectMapper mapper) throws Exception {
        //verify serialization
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        ObjectOutputStream outputStream = new ObjectOutputStream(byteArrayOutputStream);

        outputStream.writeObject(mapper);
        byte[] serializedBytes = byteArrayOutputStream.toByteArray();

        //verify deserialization
        ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(serializedBytes);
        ObjectInputStream inputStream = new ObjectInputStream(byteArrayInputStream);

        Object deserializedObject = inputStream.readObject();
        return (ObjectMapper) deserializedObject;
    }
}
