package com.fasterxml.jackson.databind.deser.creators.jdk8;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.*;
import org.junit.*;

import static org.assertj.core.api.BDDAssertions.then;

public class JsonCreatorTest extends BaseMapTest
{
    static class ClassWithJsonCreatorOnStaticMethod {
        final String first;
        final String second;

        ClassWithJsonCreatorOnStaticMethod(String first, String second) {
             this.first = first;
             this.second = second;
        }

        @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
        static ClassWithJsonCreatorOnStaticMethod factory(String first, String second) {
             return new ClassWithJsonCreatorOnStaticMethod(first, second);
        }
   }

    @Test
    public void testJsonCreatorOnStaticMethod() throws Exception {
        ObjectMapper objectMapper = newObjectMapper();

        String json = aposToQuotes("{'first':'1st','second':'2nd'}");
        ClassWithJsonCreatorOnStaticMethod actual = objectMapper.readValue(json, ClassWithJsonCreatorOnStaticMethod.class);

        then(actual).isEqualToComparingFieldByField(new ClassWithJsonCreatorOnStaticMethod("1st", "2nd"));
    }
}
