package com.fasterxml.jackson.databind.deser;

import java.util.Map;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

public class KeyDeser1429Test extends BaseMapTest
{
    static class FullName {
        private String _firstname, _lastname;

        private FullName(String firstname, String lastname) {
            _firstname = firstname;
            _lastname = lastname;
        }

        @JsonCreator
        public static FullName valueOf(String value) {
            String[] mySplit = value.split("\\.");
            return new FullName(mySplit[0], mySplit[1]);
        }

        public static FullName valueOf(String firstname, String lastname) {
            return new FullName(firstname, lastname);
        }

        @JsonValue
        @Override
        public String toString() {
            return _firstname + "." + _lastname;
        }
    }

    public void testDeserializeKeyViaFactory() throws Exception
    {
        Map<FullName, Double> map =
            new ObjectMapper().readValue("{\"first.last\": 42}",
                    new TypeReference<Map<FullName, Double>>() { });
        Map.Entry<FullName, Double> entry = map.entrySet().iterator().next();
        FullName key = entry.getKey();
        assertEquals(key._firstname, "first");
        assertEquals(key._lastname, "last");
        assertEquals(entry.getValue().doubleValue(), 42, 0);
    }
}
