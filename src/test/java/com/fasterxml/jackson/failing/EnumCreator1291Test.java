package com.fasterxml.jackson.failing;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;

public class EnumCreator1291Test extends BaseMapTest
{
    static enum Enum1291 {

        V1("val1"),
        V2("val2"),
        V3("val3"),
        V4("val4"),
        V5("val5"),
        V6("val6");

        private final String name;

        Enum1291(String name) {
            this.name = name;
        }

        public static Enum1291 fromString(String name) {
            for (Enum1291 type : Enum1291.values()) {
                if (type.name.equals(name)) {
                    return type;
                }
            }
            return Enum1291.valueOf(name.toUpperCase());
        }

        @Override
        public String toString() {
            return name;
        }
    }

    public void testEnumCreators1291() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(Enum1291.V2);
        Enum1291 result = mapper.readValue(json, Enum1291.class);
        assertSame(Enum1291.V2, result);
    }
}
