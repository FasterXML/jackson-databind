package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.newJsonMapper;

public class EnumDeserialization3369Test
{
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class Data3369 {
        public Enum3369 value;
        public String person;
        public Integer age;
    }

    enum Enum3369 {
        A("ENUM_A"), B("ENUM_B"), C("ENUM_C"), D("ENUM_D");
        final String name;

        Enum3369(String name) {
          this.name = name;
        }

        @JsonCreator
        public static Enum3369 fromName(String name) {
          if (name != null) {
            switch (name) {
              case "a":
                return A;
              case "b":
                return B;
              case "c":
                return C;
              case "d":
                return D;
            }
          }
          return null;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    // [databind#3369]
    @Test
    public void testReadEnums3369() throws Exception
    {
        final ObjectReader R = newJsonMapper().readerFor(Data3369.class);

        Data3369 data = R.readValue("{\"value\" : \"a\", \"person\" : \"Jeff\", \"age\" : 30}");
        _verify3369(data, Enum3369.A);

        data = R.readValue("{\"value\" : \"e\", \"person\" : \"Jeff\", \"age\" : 30}");
        _verify3369(data, null);

        // 01-Jun-2023, tatu: These are wrong, should not pass. See [databind#3956]
        //   for further changes

        /*
        data = R.readValue("{\"value\" : [\"a\"], \"person\" : \"Jeff\", \"age\" : 30}");
        _verify3369(data, null);

        data = R.readValue("{\"value\" : {\"a\":{}}, \"person\" : \"Jeff\", \"age\": 30}");
        _verify3369(data, null);
        */
    }

    private void _verify3369(Data3369 data, Enum3369 expEnum) {
        assertEquals("Jeff", data.person);
        assertEquals(Integer.valueOf(30), data.age);
        assertEquals(expEnum, data.value);
    }

}
