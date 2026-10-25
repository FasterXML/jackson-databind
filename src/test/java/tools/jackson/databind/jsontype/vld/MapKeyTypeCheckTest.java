package tools.jackson.databind.jsontype.vld;

import java.util.*;
import java.util.logging.FileHandler;

import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.*;
import tools.jackson.databind.deser.DeserializationContextExt;
import tools.jackson.databind.deser.jdk.JDKKeyDeserializers;
import tools.jackson.databind.exc.InvalidDefinitionException;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Map key types deserialized via single-String constructor or factory method
 * must be checked against the same deny list as bean types.
 */
public class MapKeyTypeCheckTest extends DatabindTestUtil
{
    static class KeyedBean {
        public Map<FileHandler, String> values;
    }

    // NOTE: need custom ObjectMapper subtype to access DeserializationContext
    @SuppressWarnings("serial")
    static class AccessibleMapper extends ObjectMapper {
        public DeserializationContextExt deserializationContext() {
            return _deserializationContext();
        }
    }

    private final AccessibleMapper MAPPER = new AccessibleMapper();

    @Test
    public void illegalMapKeyTypeRejected() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> MAPPER.readValue(a2q("{'values':{'key':'x'}}"), KeyedBean.class));
        verifyException(e, "Illegal type");
        verifyException(e, FileHandler.class.getName());
    }

    // Check must also apply when key deserializer is requested directly
    @Test
    public void illegalTypeRejectedByJDKKeyDeserializers() throws Exception
    {
        InvalidDefinitionException e = assertThrows(InvalidDefinitionException.class,
                () -> JDKKeyDeserializers.findStringBasedKeyDeserializer(
                        MAPPER.deserializationContext(), MAPPER.constructType(FileHandler.class)));
        verifyException(e, "Illegal type");
    }

    @Test
    public void stringCtorMapKeyTypeStillWorks() throws Exception
    {
        Map<StringBuilder, String> result = MAPPER.readValue(a2q("{'key':'x'}"),
                new TypeReference<Map<StringBuilder, String>>() { });
        assertEquals(1, result.size());
        assertEquals("key", result.keySet().iterator().next().toString());
    }
}
