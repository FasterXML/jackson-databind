package tools.jackson.databind.ext.jdk8;

import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;
import tools.jackson.databind.type.ReferenceType;

import static org.junit.jupiter.api.Assertions.*;

public class JDK8TypesTest
    extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testOptionalsAreReferentialTypes() throws Exception
    {
        JavaType t = MAPPER.constructType(Optional.class);
        assertTrue(t.isReferenceType());
        ReferenceType rt = (ReferenceType) t;
        assertEquals(Object.class, rt.getContentType().getRawClass());

        t = MAPPER.constructType(OptionalInt.class);
        assertTrue(t.isReferenceType());
        rt = (ReferenceType) t;
        assertEquals(Integer.TYPE, rt.getContentType().getRawClass());

        t = MAPPER.constructType(OptionalLong.class);
        assertTrue(t.isReferenceType());
        rt = (ReferenceType) t;
        assertEquals(Long.TYPE, rt.getContentType().getRawClass());

        t = MAPPER.constructType(OptionalDouble.class);
        assertTrue(t.isReferenceType());
        rt = (ReferenceType) t;
        assertEquals(Double.TYPE, rt.getContentType().getRawClass());
   }
}
