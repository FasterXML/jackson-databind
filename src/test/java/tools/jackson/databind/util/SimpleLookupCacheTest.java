package tools.jackson.databind.util;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class SimpleLookupCacheTest extends DatabindTestUtil
{
    @Test
    public void testPutGet() {
        SimpleLookupCache<String, Integer> m = new SimpleLookupCache<>(5, 5);

        assertEquals(0, m.size());
        m.put("k1", 100);
        assertEquals(1, m.size());
        assertNull(m.get("nosuchkey"));
        assertEquals(Integer.valueOf(100), m.get("k1"));

        m.put("k2", 200);
        assertEquals(2, m.size());
        assertEquals(Integer.valueOf(200), m.get("k2"));
    }

    @Test
    public void testEviction() {
        SimpleLookupCache<String, Integer> m = new SimpleLookupCache<>(5, 5);

        assertEquals(0, m.size());
        m.put("k1", 100);
        assertEquals(1, m.size());
        m.put("k2", 101);
        assertEquals(2, m.size());
        m.put("k3", 102);
        assertEquals(3, m.size());
        m.put("k4", 103);
        assertEquals(4, m.size());
        m.put("k5", 104);
        assertEquals(5, m.size());
        m.put("k6", 105);
        assertEquals(5, m.size());
        m.put("k7", 106);
        assertEquals(5, m.size());
        m.put("k8", 107);
        assertEquals(5, m.size());

        assertNull(m.get("k3"));
        assertEquals(Integer.valueOf(105), m.get("k6"));
    }

    @Test
    public void testJDKSerialization() throws Exception
    {
        final int maxEntries = 32;
        SimpleLookupCache<String,Integer> map = new SimpleLookupCache<String,Integer>(16, maxEntries);
        map.put("a", 1);
        assertEquals(1, map.size());

        byte[] bytes = jdkSerialize(map);
        SimpleLookupCache<String,Integer> result = jdkDeserialize(bytes);
        // transient implementation, will be read as empty
        assertNull(result.get("a"));
        assertEquals(0, result.size());
        assertEquals(maxEntries, result._map.capacity());

        // but should be possible to re-populate
        assertNull(result.put("a", 2));
        assertEquals(Integer.valueOf(2), result.get("a"));
        assertEquals(1, result.size());
    }
}
