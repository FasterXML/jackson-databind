package tools.jackson.databind;

import org.junit.jupiter.api.Test;

import tools.jackson.core.*;
import tools.jackson.core.io.CharacterEscapes;
import tools.jackson.core.io.SerializedString;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonWriteFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class MapperViaParserTest
    extends DatabindTestUtil
{
    final static int TWO_BYTE_ESCAPED = 0x111;
    final static int THREE_BYTE_ESCAPED = 0x1111;

    final static SerializedString TWO_BYTE_ESCAPED_STRING = new SerializedString("&111;");
    final static SerializedString THREE_BYTE_ESCAPED_STRING = new SerializedString("&1111;");

    static class Pojo
    {
        int _x;

        public void setX(int x) { _x = x; }
    }

    /*
    /********************************************************
    /* Helper types
    /********************************************************
     */

    /**
     * Trivial simple custom escape definition set.
     */
    static class MyEscapes extends CharacterEscapes
    {
        private static final long serialVersionUID = 1L;

        private final int[] _asciiEscapes;

        public MyEscapes() {
            _asciiEscapes = standardAsciiEscapesForJSON();
            _asciiEscapes['a'] = 'A'; // to basically give us "\A"
            _asciiEscapes['b'] = CharacterEscapes.ESCAPE_STANDARD; // too force "\u0062"
            _asciiEscapes['d'] = CharacterEscapes.ESCAPE_CUSTOM;
        }

        @Override
        public int[] getEscapeCodesForAscii() {
            return _asciiEscapes;
        }

        @Override
        public SerializableString getEscapeSequence(int ch)
        {
            if (ch == 'd') {
                return new SerializedString("[D]");
            }
            if (ch == TWO_BYTE_ESCAPED) {
                return TWO_BYTE_ESCAPED_STRING;
            }
            if (ch == THREE_BYTE_ESCAPED) {
                return THREE_BYTE_ESCAPED_STRING;
            }
            return null;
        }
    }

    /*
    /********************************************************
    /* Unit tests
    /********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    public void testPojoReadingOkClass() throws Exception
    {
        try (JsonParser p = MAPPER.createParser(a2q("{ 'x' : 9 }"))) {
            Pojo pojo = p.readValueAs(Pojo.class);
            assertEquals(9, pojo._x);
        }
    }

    @Test
    public void testPojoReadingOkTypeRef() throws Exception
    {
        try (JsonParser p = MAPPER.createParser(a2q("{ 'x' : 7 }"))) {
            Pojo pojo = p.readValueAs(new TypeReference<Pojo>() { });
            assertEquals(7, pojo._x);
        }
    }

    @Test
    public void testPojoReadingOkJavaType() throws Exception
    {
        try (JsonParser p = MAPPER.createParser(a2q("{ 'x' : 42 }"))) {
            Pojo pojo = p.readValueAs(MAPPER.constructType(Pojo.class));
            assertEquals(42, pojo._x);
        }
    }

    @Test
    public void testTreeReadingOk() throws Exception
    {
        final String JSON = a2q("{ 'x' : 9 }");
        try (JsonParser p = MAPPER.createParser(JSON)) {
            JsonNode tree = p.readValueAsTree();
            assertEquals(MAPPER.createObjectNode().put("x", 9), tree);
        }
    }

    // // // Misc other tests
    
    @Test
    public void testEscapingUsingMapper() throws Exception
    {
        ObjectMapper mapper = JsonMapper.builder(JsonFactory.builder()
                .enable(JsonWriteFeature.ESCAPE_NON_ASCII).build())
                .build();
        final String json = mapper.writeValueAsString(String.valueOf((char) 258));
        assertEquals(q("\\u0102"), json);
    }
}
