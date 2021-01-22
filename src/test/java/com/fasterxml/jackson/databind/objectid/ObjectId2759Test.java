package com.fasterxml.jackson.databind.objectid;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIdentityInfo;
import com.fasterxml.jackson.annotation.JsonIdentityReference;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.ObjectIdGenerators;

import com.fasterxml.jackson.databind.*;

public class ObjectId2759Test extends BaseMapTest
{
    static class Hive {
        public String name;
        public List<Bee> bees = new ArrayList<>();

        public Long id;

        Hive() { }

        public Hive(Long id, String name) {
            this.id = id;
            this.name = name;
        }

        public void addBee(Bee bee) {
            bees.add(bee);
        }
    }

    static class Bee {
        public Long id;

        @JsonIdentityInfo(generator = ObjectIdGenerators.PropertyGenerator.class, property = "id")
        @JsonIdentityReference(alwaysAsId = true)
        @JsonProperty("hiveId")
        Hive hive;

        public Bee() { }

        public Bee(Long id, Hive hive) {
            this.id = id;
            this.hive = hive;
        }

        public Hive getHive() {
            return hive;
        }

        public void setHive(Hive hive) {
            this.hive = hive;
        }
    }

    public void testObjectId2759() throws Exception
    {
        Hive hive = new Hive(100500L, "main hive");
        hive.addBee(new Bee(1L, hive));

        ObjectMapper mapper = newJsonMapper();
        final String json = mapper.writerWithDefaultPrettyPrinter()
                .writeValueAsString(hive);
        try {
            mapper.readerFor(JsonNode.class)
                .with(DeserializationFeature.FAIL_ON_READING_DUP_TREE_KEY)
                .readValue(json);
        } catch (DatabindException e) {
            fail("Should not have duplicates, but JSON content has: "+json);
        }
    }
}
