package tools.jackson.databind.misc;

import java.io.StringReader;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

// [databind#5372]: Property name mismatch problem,
// caused by [core#1491]; verify that databind usage fixed as well
public class PropertyMismatch5372Test extends DatabindTestUtil
{
    static class TestObject5372 {
        private String aaaabbbbcccc;
        private String aaaabbbbcccc2;

        public String getAaaabbbbcccc2() {
            return aaaabbbbcccc2;
        }

        public void setAaaabbbbcccc2(String aaaabbbbcccc2) {
            this.aaaabbbbcccc2 = aaaabbbbcccc2;
        }

        public String getAaaabbbbcccc() {
            return aaaabbbbcccc;
        }

        public void setAaaabbbbcccc(String aaaabbbbcccc) {
            this.aaaabbbbcccc = aaaabbbbcccc;
        }
    }

    private final String KEY_1 = "aaaabbbbcccc";
    private final String KEY_2 = "aaaabbbbcccc2";

    private final String DOC_5372 = """
{
"%s": "v3",
"%s": "v4"
}
""".formatted(KEY_1, KEY_2);

    private final ObjectMapper MAPPER = newJsonMapper();

    @Test
    void testIssue5372Bytes() throws Exception
    {
        final byte[] bytes = DOC_5372.getBytes(StandardCharsets.UTF_8);
        _assert5372(MAPPER.readValue(bytes, TestObject5372.class));
        _assert5372(MAPPER.readValue(new ByteArrayInputStream(bytes), TestObject5372.class));
    }

    @Test
    void testIssue5372Chars() throws Exception
    {
        _assert5372(MAPPER.readValue(DOC_5372, TestObject5372.class));
        _assert5372(MAPPER.readValue(new StringReader(DOC_5372), TestObject5372.class));
    }

    private void _assert5372(TestObject5372 result) {
        assertEquals("v3", result.getAaaabbbbcccc());
        assertEquals("v4", result.getAaaabbbbcccc2());
    }
}
