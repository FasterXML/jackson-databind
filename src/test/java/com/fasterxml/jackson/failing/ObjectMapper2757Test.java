package com.fasterxml.jackson.failing;

import java.util.AbstractMap;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import com.fasterxml.jackson.databind.*;

public class ObjectMapper2757Test extends BaseMapTest
{
    interface MultiValueMap<K, V> extends Map<K, List<V>> { }
    abstract static class MyMultiMap<K, V> extends AbstractMap<K, List<V>>
        implements MultiValueMap<K, V> { }

    static class MyMap extends MyMultiMap<String, String> {
        public MyMap() { }

        public void setValue(StringWrapper w) { }
        public void setValue(IntWrapper w) { }

        public long getValue() { return 0L; }

        @Override
        public Set<Entry<String, List<String>>> entrySet() {
            return Collections.emptySet();
        }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    // [databind#2757]: should allow deserialization as Map despite conflicting setters
    public void testCanDeserializeMap() throws Exception
    {
//        final String json = MAPPER.writeValueAsString(new MyMap());
//        System.out.println("json: "+json);
 //       MyMap x = MAPPER.readValue(json, MyMap.class);
        final AtomicReference<Throwable> ref = new AtomicReference<>();
        final JavaType type = MAPPER.constructType(MyMap.class);

        System.err.println("Type: "+type);
        
        boolean can = MAPPER.canDeserialize(MAPPER.constructType(type),
                ref);
System.err.println(" Cause -> "+ref.get());
        if (!can) {
            ref.get().printStackTrace();
            fail("canDeserialize() returned false; underlying failure: "+ref.get());
        }
    }
}
