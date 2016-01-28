package com.fasterxml.jackson.failing;

import java.util.List;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.type.CollectionType;
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
    public void testExplicitCollectionType() throws Exception
    {
        JavaType elem = SimpleType.construct(Point.class);
        final String json = aposToQuotes("[ {'x':1,'y':2}, {'x':3,'y':6 }]");
        
        JavaType t = CollectionType.construct(List.class, elem);
        List<Point> l = MAPPER.readValue(json, t);
        assertNotNull(l);
        assertEquals(2, l.size());
        Object ob = l.get(0);
        assertEquals(Point.class, ob.getClass());
        Point p = (Point) ob;
        assertEquals(1, p.x);
        assertEquals(2, p.getY());
    }
}
