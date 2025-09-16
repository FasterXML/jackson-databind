package tools.jackson.databind.ext.desktop;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;

import tools.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class ReadOnlyDeserWithConstructorProperties1890Test
{
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

        protected Person(TestEnum testEnum, String name) {
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

   // [databind#1890]
   @Test
   public void testDeserializeAnnotationsOneField() throws Exception {
       PersonAnnotations person = MAPPER.readerFor(PersonAnnotations.class)
               .readValue("{\"testEnum\":\"abc\"}");
       // cannot remain as is, so becomes `null`
       assertEquals(null, person.getTestEnum());
       assertNull(person.name);
   }

    @Test
   public void testDeserializeAnnotationsTwoFields() throws Exception {
       PersonAnnotations person = MAPPER.readerFor(PersonAnnotations.class)
               .readValue("{\"testEnum\":\"xyz\",\"name\":\"changyong\"}");
       // cannot remain as is, so becomes `null`
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
