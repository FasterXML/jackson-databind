package com.fasterxml.jackson.databind.deser.creators.jdk8;

import java.io.IOException;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class PersonTest extends BaseMapTest
{
    static class Person {

        // mandatory fields
        private final String name;
        private final String surname;

        // optional fields
        private String nickname;

        // 29-Jan-2018, tatu: Should (apparently?! nice) work without annotation, as long as
        //   parameter names exist
//        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        public Person(String name, String surname) {

            this.name = name;
            this.surname = surname;
        }

        public String getName() {
            return name;
        }

        public String getSurname() {
            return surname;
        }

        public String getNickname() {
            return nickname;
        }

        public void setNickname(String nickname) {
            this.nickname = nickname;
        }
    }    
    public void testPersonDeserialization() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();
        Person actual = mapper.readValue("{\"name\":\"joe\",\"surname\":\"smith\",\"nickname\":\"joey\"}", Person.class);

        assertEquals("joe", actual.getName());
        assertEquals("smith", actual.getSurname());
        assertEquals("joey", actual.getNickname());
    }
}
