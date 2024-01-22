package com.fasterxml.jackson.databind.deser.filter;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class ReadOnlyDeser1890Test
{
    // [databind#95]
    @JsonIgnoreProperties(value={ "computed" }, allowGetters=true)
    static class ReadOnly95Bean
    {
        public int value = 3;

        public int getComputed() { return 32; }
    }

    // [databind#1890]
    static class PersonAnnotations {
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

    static class Person {
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

   private final ObjectMapper MAPPER = newJsonMapper();

   // [databind#95]
   @Test
   public void testReadOnlyProps95() throws Exception
   {
       ObjectMapper m = new ObjectMapper();
       String json = m.writeValueAsString(new ReadOnly95Bean());
       if (json.indexOf("computed") < 0) {
           fail("Should have property 'computed', didn't: "+json);
       }
       ReadOnly95Bean bean = m.readValue(json, ReadOnly95Bean.class);
       assertNotNull(bean);
   }

   // [databind#1890]
   @Test
   public void testDeserializeAnnotationsOneField() throws Exception {
       PersonAnnotations person = MAPPER.readerFor(PersonAnnotations.class)
               .readValue("{\"testEnum\":\"abc\"}");
       // can not remain as is, so becomes `null`
       assertEquals(null, person.getTestEnum());
       assertNull(person.name);
   }

    @Test
   public void testDeserializeAnnotationsTwoFields() throws Exception {
       PersonAnnotations person = MAPPER.readerFor(PersonAnnotations.class)
               .readValue("{\"testEnum\":\"xyz\",\"name\":\"changyong\"}");
       // can not remain as is, so becomes `null`
       assertEquals(null, person.getTestEnum());
       assertEquals("changyong", person.name);
   }

    @Test
   public void testDeserializeOneField() throws Exception {
       Person person = MAPPER.readValue("{\"testEnum\":\"\"}", Person.class);
       assertEquals(TestEnum.DEFAULT, person.getTestEnum());
       assertNull(person.name);
   }

    @Test
   public void testDeserializeTwoFields() throws Exception {
       Person person = MAPPER.readValue("{\"testEnum\":\"\",\"name\":\"changyong\"}",
               Person.class);
       assertEquals(TestEnum.DEFAULT, person.getTestEnum());
       assertEquals("changyong", person.name);
   }
}
