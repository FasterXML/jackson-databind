package tools.jackson.failing;

import java.beans.ConstructorProperties;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class Creator2008Test extends DatabindTestUtil
{
    public static class Person {
        @JsonIgnore
        private String etc;
        private int age;
        private String name;

        public Person(){}

        @ConstructorProperties({"etc", "age", "name"})
        public Person(String etc, int age, String name) {
             this.etc = etc;
             this.age = age;
             this.name = name;
        }

        @JsonGetter("jacksonAge")
        public int getAge() {
             return age;
        }

        @JsonSetter("jacksonAge")
        public void reviseAge(int a) {
             this.age = a + 10;
        }

        @JsonGetter("jacksonName")
        public String getName() {
             return name;
        }

        @JsonSetter("jacksonName")
        public void reviseName(String n) {
             this.name = "lee" + n;
        }

        public String getEtc() {
             return this.etc;
        }

        @Override
        public String toString() {
             return "Person{" +
                  "age=" + age +
                  ", name='" + name + '\'' +
                  '}';
        }
   }

    private ObjectMapper objectMapper = newJsonMapper();

    @Test
    public void testSimple() throws Exception {
        String json = "{\"jacksonAge\":30,\"jacksonName\":\"changyong\"}";

        Person actual = this.objectMapper.readValue(json, Person.class);
        String result = this.objectMapper.writeValueAsString(actual);
              String expected = "{\"jacksonAge\":40,\"jacksonName\":\"leechangyong\"}";

        assertEquals(expected, result);
   }
}
