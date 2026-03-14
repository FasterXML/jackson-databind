package tools.jackson.databind.interop;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.ObjectMapper;

import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertNotNull;

// From https://github.com/FasterXML/jackson-module-kotlin/issues/54
public class KotlinIssueGH54JsonIdentityTest
    extends DatabindTestUtil
{
    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class Entity1 {
        private final String name;
        private Entity2 entity2;
        private Entity1 parent;

        @JsonCreator
        public Entity1(
                @JsonProperty("name") String name,
                @JsonProperty("entity2") Entity2 entity2,
                @JsonProperty("parent") Entity1 parent) {
            this.name = name;
            this.entity2 = entity2;
            this.parent = parent;
        }

        public String getName() { return name; }
        public Entity2 getEntity2() { return entity2; }
        public void setEntity2(Entity2 entity2) { this.entity2 = entity2; }
        public Entity1 getParent() { return parent; }
        public void setParent(Entity1 parent) { this.parent = parent; }
    }

    @JsonIdentityInfo(generator = ObjectIdGenerators.IntSequenceGenerator.class)
    static class Entity2 {
        private final String name;
        private Entity1 entity1;

        @JsonCreator
        public Entity2(
                @JsonProperty("name") String name,
                @JsonProperty("entity1") Entity1 entity1) {
            this.name = name;
            this.entity1 = entity1;
        }

        public String getName() { return name; }
        public Entity1 getEntity1() { return entity1; }
        public void setEntity1(Entity1 entity1) { this.entity1 = entity1; }
    }

    private final ObjectMapper MAPPER = newJsonMapper();

    //@JacksonTestFailureExpected
    @Test
    void testDeserWithIdentityInfo() throws Exception {
        Entity1 entity1 = new Entity1("test_entity1", null, null);
        Entity2 entity2 = new Entity2("test_entity2", entity1);
        Entity1 rootEntity1 = new Entity1("root_entity1", entity2, null);

        entity1.setParent(rootEntity1);
        entity1.setEntity2(entity2);

        String json = MAPPER.writeValueAsString(entity1);

        Entity1 result = MAPPER.readValue(json, Entity1.class);
        assertNotNull(result);
    }
}
