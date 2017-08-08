package com.fasterxml.jackson.databind.interop;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.SimpleType;

/**
 * Set of tests to ensure that changes between 2.6 and 2.7 can
 * be handled somewhat gracefully.
 */
public class DeprecatedTypeHandling1102Test extends BaseMapTest
{
    static class Point {
        public int x;

        int _y;

        public void setY(int y0) { _y = y0; }
        public int getY() { return _y; }
    }

    static class Point3D extends Point {
        public int z;
    }
    
    final ObjectMapper MAPPER = objectMapper();

    @SuppressWarnings("deprecation")
    public void testSimplePOJOType() throws Exception
    {
        JavaType elem = SimpleType.construct(Point.class);

        Point p = MAPPER.readValue(aposToQuotes("{'x':1,'y':2}"), elem);
        assertNotNull(p);
        assertEquals(1, p.x);
        assertEquals(2, p.getY());
    }

    @SuppressWarnings("deprecation")
    public void testPOJOSubType() throws Exception
    {
        JavaType elem = SimpleType.construct(Point3D.class);

        Point3D p = MAPPER.readValue(aposToQuotes("{'x':1,'z':3,'y':2}"), elem);
        assertNotNull(p);
        assertEquals(1, p.x);
        assertEquals(2, p.getY());
        assertEquals(3, p.z);
    }
}
