package tools.jackson.databind.objectid;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import tools.jackson.core.JsonParser;

import tools.jackson.databind.*;
import tools.jackson.databind.deser.std.StdDeserializer;
import tools.jackson.databind.module.SimpleModule;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Test for [databind#2245]: UnresolvedForwardReference with
 * {@code @JsonIdentityReference(alwaysAsId=true)} and custom deserializer.
 * Passes without any changes, possibly fixed in 3.0.0
 */
public class ObjectIdWithCustomDeserializer2245Test extends DatabindTestUtil
{
    // Entity class that uses @JsonIdentityInfo
    @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
    static class Entity2245 {
        public int id;
        public String name;

        public Entity2245() { }
        public Entity2245(int id, String name) {
            this.id = id;
            this.name = name;
        }
    }

    // Container that references Entity by ID only
    static class Container2245 {
        @JsonIdentityReference(alwaysAsId = true)
        public Entity2245 entity;
    }

    // Custom deserializer that resolves ID to Entity (simulates database lookup)
    static class Entity2245Deserializer extends StdDeserializer<Entity2245> {
        public Entity2245Deserializer() {
            super(Entity2245.class);
        }

        @Override
        public Entity2245 deserialize(JsonParser p, DeserializationContext ctxt)
        {
            int id = p.getIntValue();
            // Simulate database lookup - return entity with resolved name
            return new Entity2245(id, "Resolved-" + id);
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    // [databind#2245]
    @Test
    public void testCustomDeserializerWithAlwaysAsId() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Entity2245.class, new Entity2245Deserializer());

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        String json = "{\"entity\": 99}";

        Container2245 result = mapper.readValue(json, Container2245.class);

        assertNotNull(result);
        assertNotNull(result.entity);
        assertEquals(99, result.entity.id);
        assertEquals("Resolved-99", result.entity.name);
    }

    // Test with list of IDs
    static class ContainerWithList2245 {
        @JsonIdentityReference(alwaysAsId = true)
        public java.util.List<Entity2245> entities;
    }

    @Test
    public void testCustomDeserializerWithAlwaysAsIdInList() throws Exception
    {
        SimpleModule module = new SimpleModule("test");
        module.addDeserializer(Entity2245.class, new Entity2245Deserializer());

        ObjectMapper mapper = jsonMapperBuilder()
                .addModule(module)
                .build();

        String json = "{\"entities\": [1, 2, 3]}";

        ContainerWithList2245 result = mapper.readValue(json, ContainerWithList2245.class);

        assertNotNull(result);
        assertNotNull(result.entities);
        assertEquals(3, result.entities.size());
        assertEquals(1, result.entities.get(0).id);
        assertEquals("Resolved-1", result.entities.get(0).name);
        assertEquals(2, result.entities.get(1).id);
        assertEquals(3, result.entities.get(2).id);
    }
}
