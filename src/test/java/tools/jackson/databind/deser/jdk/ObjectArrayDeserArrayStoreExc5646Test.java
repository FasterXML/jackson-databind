package tools.jackson.databind.deser.jdk;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.testutil.NoCheckSubTypeValidator;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#5646]: ArrayStoreException in ObjectArrayDeserializer._handleNonArray
 * (OSS-Fuzz issue 4548745300869120)
 */
public class ObjectArrayDeserArrayStoreExc5646Test extends DatabindTestUtil
{
    // [databind#5646]: catch internal problem, translate to Jackson API exception
    // (do not hide tho)
    @Test
    public void testArrayStoreExceptionInObjectArrayDeserializer() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
            .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                DefaultTyping.NON_FINAL)
            .build();

        ObjectReader reader = mapper.readerFor(Object.class)
            .with(DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY);

        String json = "[\"java.util.ArrayList\",[[\"[Ljava.util.Arrays$ArrayList;\",6]]]";

        // Should throw DatabindException with "Internal error:" message, not ArrayStoreException
        try {
            reader.readValue(json);
            fail("Should throw exception");
        } catch (DatabindException e) {
            verifyException(e, "Internal error: deserialized value of type `java.util.ArrayList`");
        }
    }
}
