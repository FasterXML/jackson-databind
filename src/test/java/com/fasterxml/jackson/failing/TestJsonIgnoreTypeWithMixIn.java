package com.fasterxml.jackson.failing;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreType;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.test.BaseTest;

public class TestJsonIgnoreTypeWithMixIn extends BaseTest {

    @JsonIgnoreType
    static class Person {
        
        private String name;
        private String address;
        
        public Person(String name, String address) {
            this.name = name;
            this.address = address;
        }
        
        public String getName() {
            return name;
        }
        
        public void setName(String name) {
            this.name = name;
        }
        
        public String getAddress() {
            return address;
        }
        
        public void setAddress(String address) {
            this.address = address;
        }
    }
    
    @JsonIgnoreType
    static abstract class PersonMixin {
        
        @JsonProperty abstract String getName();
    }
    
    public void testSingle() throws Exception {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Person.class, PersonMixin.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Person person = new Person("Person 1", "Address 1");
        String json = mapper.writeValueAsString(person);
        assertEquals("{\"name\":\"Person 1\"}", json);
    }
    
    public void testList() throws Exception {
        SimpleModule module = new SimpleModule();
        module.setMixInAnnotation(Person.class, PersonMixin.class);
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(module);
        Person p1 = new Person("Person 1", "Address 1");
        Person p2 = new Person("Person 2", "Address 2");
        List<Person> persons = new ArrayList<Person>();
        persons.add(p1);
        persons.add(p2);
        String json = mapper.writeValueAsString(persons);
        assertEquals("{\"name\":\"Person 1\"},\"name\":\"Person 2\"}", json);
    }
}
