package tools.jackson.databind.objectid;

import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

// [databind#6352]: `BeanPropertyMap.withProperty()` must not mutate the map shared by
// the cached, non-contextual `Foo4` deserializer
public class ObjectIdPropertyMapIsolation6352Test extends DatabindTestUtil
{
    static class Foo4 {
        public int id;
        public String name;
    }

    static class Holder4 {
        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        public Foo4 foo;

        public List<Foo4> plain;
    }

    @Test
    public void objectIdDoesNotLeakIntoThePlainDeserializer() throws Exception
    {
        final ObjectMapper mapper = newJsonMapper();

        // First use: the property-level Object Id descriptor replaces the "id" property
        Holder4 holder = mapper.readValue("""
                {"foo":{"id":1,"name":"a"},
                 "plain":[{"id":7,"name":"b"},{"id":7,"name":"c"}]}
                """, Holder4.class);
        assertNotNull(holder);
        assertEquals(1, holder.foo.id);
        assertEquals(List.of("b", "c"), holder.plain.stream().map(f -> f.name).toList());

        // Second use: Foo4 has no Object Id annotations, so duplicate ids are plain values
        Foo4[] plain = mapper.readValue("""
                [{"id":7,"name":"b"},{"id":7,"name":"c"}]
                """, Foo4[].class);
        assertEquals(2, plain.length);
        assertEquals(7, plain[0].id);
    }
}
