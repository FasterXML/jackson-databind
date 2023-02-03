package com.fasterxml.jackson.databind.jsontype.deftyping;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.NoCheckSubTypeValidator;

// Tests to verify functionality to pass "base type" for serializing
// polymorphic types (ones where output contains Type Id to allow later
// deserialization deserialization), separate from fully forcing
// actual type of value being serialized.
public class DefaultWithBaseType1093Test extends BaseMapTest
{
    // [databind#1093]
    static class Point1093 {
        public int x, y;

        protected Point1093() { }
        public Point1093(int _x, int _y) {
            x = _x;
            y = _y;
        }
    }

    // [databind#1093]
    public void testWithDefaultTyping() throws Exception
    {
        ObjectMapper m = JsonMapper.builder()
                .activateDefaultTyping(NoCheckSubTypeValidator.instance,
                        ObjectMapper.DefaultTyping.JAVA_LANG_OBJECT)
                .build();

        final Point1093 input = new Point1093(28, 12);

        _testWithDefaultTyping(input, m.readerFor(Object.class),
                m.writer().forType(Object.class));
        _testWithDefaultTyping(input, m.readerFor(Object.class),
                m.writerFor(Object.class));
    }

    private void _testWithDefaultTyping(Point1093 input, ObjectReader r,
            ObjectWriter w) throws Exception
    {
        String json = w.writeValueAsString(input);

        Point1093 result = (Point1093) r.readValue(json);

        assertEquals(input.x, result.x);
        assertEquals(input.y, result.y);
    }
}
