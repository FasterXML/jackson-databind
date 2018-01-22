package com.fasterxml.jackson.failing;

import java.beans.ConstructorProperties;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadOnlyDeser1890Test
    extends BaseMapTest
{
    public static class PersonAnnotations {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private TestEnum testEnum;
        private String name;

        public PersonAnnotations() { }

        @ConstructorProperties({"testEnum", "name"})
        public PersonAnnotations(TestEnum testEnum, String name) {
             this.testEnum = testEnum;
             this.name = name;
        }

        public TestEnum getTestEnum() {
             return testEnum;
        }

        public void setTestEnum(TestEnum testEnum) {
             this.testEnum = testEnum;
        }

        public String getName() {
             return name;
        }

        public void setName(String name) {
             this.name = name;
        }
   }

   public static class Person {
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private TestEnum testEnum;
        private String name;

        public Person() {
        }

        public Person(TestEnum testEnum, String name) {
             this.testEnum = testEnum;
             this.name = name;
        }

        public TestEnum getTestEnum() {
             return testEnum;
        }

        public void setTestEnum(TestEnum testEnum) {
             this.testEnum = testEnum;
        }

        public String getName() {
             return name;
        }

        public void setName(String name) {
             this.name = name;
        }
   }

   enum TestEnum{
        TEST
   }

   /*
   /**********************************************************
   /* Test methods
   /**********************************************************
    */

   private final ObjectMapper MAPPER = objectMapper();

   public void testDeserializeAnnotationsOneField() throws IOException {
       PersonAnnotations person = MAPPER.readValue("{\"testEnum\":\"\"}", PersonAnnotations.class);
       assertNull(person.getTestEnum());
   }

   public void testDeserializeAnnotationsTwoFields() throws IOException {
       PersonAnnotations person = MAPPER.readValue("{\"testEnum\":\"\",\"name\":\"changyong\"}",
               PersonAnnotations.class);
       assertNull(person.getTestEnum());
       assertEquals("changyong", person.getName());
   }

   public void testDeserializeOneField() throws IOException {
       Person person = MAPPER.readValue("{\"testEnum\":\"\"}", Person.class);
       assertNull(person.getTestEnum());
   }

   public void testDeserializeTwoFields() throws IOException {
       Person person = MAPPER.readValue("{\"testEnum\":\"\",\"name\":\"changyong\"}",
               Person.class);
       assertNull(person.getTestEnum());
       assertEquals("changyong", person.getName());
   }
}
