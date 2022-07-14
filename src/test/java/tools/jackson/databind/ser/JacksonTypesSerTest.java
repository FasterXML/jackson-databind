package tools.jackson.databind.ser;

import java.io.*;
import java.util.*;

import tools.jackson.core.*;
import tools.jackson.core.io.ContentReference;
import tools.jackson.databind.*;
import tools.jackson.databind.util.TokenBuffer;

/**
 * Unit tests for those Jackson types we want to ensure can be serialized.
 */
public class JacksonTypesSerTest
    extends BaseMapTest
{
    private final ObjectMapper MAPPER = newJsonMapper();

    public void testLocation() throws IOException
    {
        File f = new File("/tmp/test.json");
        JsonLocation loc = new JsonLocation(ContentReference.rawReference(f),
                -1, 100, 13);
        Map<String,Object> result = writeAndMap(MAPPER, loc);
        // 04-Apr-2021, tatu: Jackson 2.x used to output "sourceRef"; no longer in 3.x
//        assertEquals(f.getAbsolutePath(), result.get("sourceRef"));
        assertEquals(Integer.valueOf(-1), result.get("charOffset"));
        assertEquals(Integer.valueOf(-1), result.get("byteOffset"));
        assertEquals(Integer.valueOf(100), result.get("lineNr"));
        assertEquals(Integer.valueOf(13), result.get("columnNr"));
        assertEquals(4, result.size());
    }

    /**
     * Verify that {@link TokenBuffer} can be properly serialized
     * automatically, using the "standard" JSON sample document
     */
    public void testTokenBuffer() throws Exception
    {
        // First, copy events from known good source (StringReader)
        JsonParser p = createParserUsingReader(SAMPLE_DOC_JSON_SPEC);
        TokenBuffer tb = TokenBuffer.forGeneration();
        while (p.nextToken() != null) {
            tb.copyCurrentEvent(p);
        }
        p.close();
        // Then serialize as String
        String str = MAPPER.writeValueAsString(tb);
        tb.close();
        // and verify it looks ok
        verifyJsonSpecSampleDoc(createParserUsingReader(str), true);
    }
}
