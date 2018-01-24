package com.fasterxml.jackson.databind.deser.filter;

import java.beans.ConstructorProperties;
import java.io.IOException;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class ReadOnlyDeser1890Test
    extends BaseMapTest
{
    public static class PersonAnnotations {
        public String name;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private TestEnum testEnum = TestEnum.DEFAULT;

        PersonAnnotations() { }

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
   }

    public static class Person {
        public String name;
        @JsonProperty(access = JsonProperty.Access.READ_ONLY)
        private TestEnum testEnum = TestEnum.DEFAULT;

        Person() { }

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
   }

   enum TestEnum{
       DEFAULT, TEST;
   }

   /*
   /**********************************************************
   /* Test methods
   /**********************************************************
    */

   private final ObjectMapper MAPPER = objectMapper();

   public void testDeserializeAnnotationsOneField() throws IOException {
       PersonAnnotations person = MAPPER.readValue("{\"testEnum\":\"\"}", PersonAnnotations.class);
       // can not remain as is, so becomes `null`
       assertEquals(null, person.getTestEnum());
       assertNull(person.name);
   }

   public void testDeserializeAnnotationsTwoFields() throws IOException {
       PersonAnnotations person = MAPPER.readValue("{\"testEnum\":\"\",\"name\":\"changyong\"}",
               PersonAnnotations.class);
       // can not remain as is, so becomes `null`
       assertEquals(null, person.getTestEnum());
       assertEquals("changyong", person.name);
   }

   public void testDeserializeOneField() throws IOException {
       Person person = MAPPER.readValue("{\"testEnum\":\"\"}", Person.class);
       assertEquals(TestEnum.DEFAULT, person.getTestEnum());
       assertNull(person.name);
   }

   public void testDeserializeTwoFields() throws IOException {
       Person person = MAPPER.readValue("{\"testEnum\":\"\",\"name\":\"changyong\"}",
               Person.class);
       assertEquals(TestEnum.DEFAULT, person.getTestEnum());
       assertEquals("changyong", person.name);
   }
}
