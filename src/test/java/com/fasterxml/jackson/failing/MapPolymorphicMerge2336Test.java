package com.fasterxml.jackson.failing;

import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

public class MapPolymorphicMerge2336Test extends BaseMapTest
{
    private final ObjectMapper MAPPER = sharedMapper();

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "discriminator"
            //, visible = true
    )
    @JsonSubTypes({@JsonSubTypes.Type(value = SomeClassA.class, name = "FirstConcreteImpl")})
    @JsonInclude(JsonInclude.Include.NON_NULL)
    public static abstract class SomeBaseClass {
        private String name;

        @JsonCreator
        public SomeBaseClass(@JsonProperty("name") String name) {
            this.name = name;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }
    }

    @JsonTypeName("FirstConcreteImpl")
    public static class SomeClassA extends SomeBaseClass {
        private Integer a;
        private Integer b;

        @JsonCreator
        public SomeClassA(@JsonProperty("name") String name, @JsonProperty("a") Integer a, @JsonProperty("b") Integer b) {
            super(name);
            this.a = a;
            this.b = b;
        }

        public Integer getA() {
            return a;
        }

        public void setA(Integer a) {
            this.a = a;
        }

        public Integer getB() {
            return b;
        }

        public void setB(Integer b) {
            this.b = b;
        }
    }

    public static class SomeOtherClass {
        String someprop;

        @JsonCreator
        public SomeOtherClass(@JsonProperty("someprop") String someprop) {
            this.someprop = someprop;
        }

        @JsonMerge
        private Map<String, SomeBaseClass> data = new LinkedHashMap<>();

        public void addValue(String key, SomeBaseClass value) {
            data.put(key, value);
        }

        public Map<String, SomeBaseClass> getData() {
            return data;
        }

        public void setData(
                Map<String, SomeBaseClass> data) {
            this.data = data;
        }
    }

    public void testPolymorphicMapMerge() throws Exception
    {
        // first let's just get some valid JSON
        SomeOtherClass test = new SomeOtherClass("house");
        test.addValue("SOMEKEY", new SomeClassA("fred", 1, null));
//        String serializedValue = MAPPER.writeValueAsString(test);

//System.out.println("Serialized value: " + serializedValue);

        // now create a reader specifically for merging
        ObjectReader reader = MAPPER.readerForUpdating(test);

        SomeOtherClass toBeMerged = new SomeOtherClass("house");
        toBeMerged.addValue("SOMEKEY", new SomeClassA("jim", null, 2));
        String jsonForMerging = MAPPER.writeValueAsString(toBeMerged);
System.out.println("JSON to be merged: " + jsonForMerging);
        // now try to do the merge and it blows up
        SomeOtherClass mergedResult = reader.readValue(jsonForMerging);
System.out.println("Serialized value: " + MAPPER.writeValueAsString(mergedResult));
    }
}
