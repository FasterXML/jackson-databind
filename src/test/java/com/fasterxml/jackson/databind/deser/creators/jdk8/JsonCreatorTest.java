package com.fasterxml.jackson.databind.deser.creators.jdk8;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

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
        ObjectMapper objectMapper = newJsonMapper();

        String json = aposToQuotes("{'first':'1st','second':'2nd'}");
        ClassWithJsonCreatorOnStaticMethod actual = objectMapper.readValue(json, ClassWithJsonCreatorOnStaticMethod.class);

        then(actual).isEqualToComparingFieldByField(new ClassWithJsonCreatorOnStaticMethod("1st", "2nd"));
    }

    static class AliasToLong {
        private final long longValue;

        private AliasToLong(long longValue) {
            this.longValue = longValue;
        }

        @JsonValue
        public long get() {
            return longValue;
        }

        @JsonCreator
        static AliasToLong of(long value) {
            return new AliasToLong(value);
        }
    }

    @Test
    public void testJsonCreatorOnStaticMethodUsesCoercion() throws Exception {
        ObjectMapper objectMapper = newJsonMapper();

        String input = "\"123\"";

        // Deserialization as a long works.
        assertEquals(123L, (long) objectMapper.readValue(input, Long.class));

        // Deserialization to 'AliasToLong' should behave the same, but does not.
        assertEquals(AliasToLong.of(123L), objectMapper.readValue(input, AliasToLong.class));
    }
}
