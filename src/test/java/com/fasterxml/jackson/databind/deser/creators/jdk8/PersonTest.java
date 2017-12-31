package com.fasterxml.jackson.databind.deser.creators.jdk8;

import static org.assertj.core.api.BDDAssertions.then;

import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

public class PersonTest
{
    @Test
    public void shouldBeAbleToDeserializePerson() throws IOException
    {
        final ObjectMapper mapper = new ObjectMapper();

        // when
        Person actual = mapper.readValue("{\"name\":\"joe\",\"surname\":\"smith\",\"nickname\":\"joey\"}", Person.class);

        // then
        Person expected = new Person("joe", "smith");
        expected.setNickname("joey");
        then(actual).isEqualToComparingFieldByField(expected);

    }
}
