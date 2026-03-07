package tools.jackson.databind.node;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.Test;

import tools.jackson.core.io.SerializedString;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.exc.StreamWriteException;
import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.fail;

public class TreeBuildingGeneratorTest extends DatabindTestUtil
{
    private final ObjectMapper MAPPER = sharedMapper();
    
    @Test
    void testBasicArrayBuilding()
    {
        TreeBuildingGenerator g = _generator();
        assertFalse(g.isClosed());
        List<String> l = Arrays.asList("foo", "bar");
        g.writeStartArray(l, l.size());
        g.writeString(l.get(0));
        g.writeString(new SerializedString(l.get(1)));
        g.writeNumber((short) 123);
        g.writeNull();
        g.writeNumber(BigInteger.valueOf(999));
        g.writeBoolean(false);
        g.writeArray(new long[0], 0, 0);
        g.writeStartObject(Boolean.TRUE, 0);
        g.writeEndObject();
        g.writeEndArray();

        assertEquals(a2q("['foo','bar',123,null,999,false,[],{}]"),
                g.treeBuilt().toString());
    }

    @Test
    void testBasicObjectBuilding()
    {
        TreeBuildingGenerator g = _generator();
        g.writeStartObject("foo");
        g.writeName(new SerializedString("null"));
        g.writeNull();
        g.writeName("arr");
        g.writeStartArray("foo", 0);
        g.writeEndArray();
        g.writeName("b");
        g.writeBoolean(true);

        g.writeName("ob");
        g.writeStartObject(Boolean.TRUE, 0);
        g.writeEndObject();
        
        g.writeEndObject();

        assertEquals(a2q("{'null':null,'arr':[],'b':true,'ob':{}}"),
                g.treeBuilt().toString());
    }

    // For [databind#5528]
    @Test
    void testNumberAsString()
    {
        try (JsonGenerator g = _generator()) {
            g.writeStartArray();
            try {
                g.writeNumber("123");
                fail("Should not pass");
            } catch (StreamWriteException e) {
                verifyException(e, "TreeBuildingGenerator` does not support `writeNumber(String)`, must write Numbers as typed");
            }
        }
    }
    
    TreeBuildingGenerator _generator() {
        return TreeBuildingGenerator.forSerialization(null, MAPPER.getNodeFactory());
    }
}
