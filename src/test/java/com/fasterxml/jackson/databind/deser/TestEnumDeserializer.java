package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;


public class TestEnumDeserializer  extends BaseMapTest
{
    enum NoneJsonValueEnum {
        ENUM_A,
        ENUM_B,
        ENUM_C
    }

    enum StringJsonValueEnum {
        ENUM_A("A"),
        ENUM_B("B"),
        ENUM_C("C");
        StringJsonValueEnum(String value) {
            this.value = value;
        }
        @JsonValue
        private String value;
    }

    enum IntJsonValueEnum {
        ENUM_A(1),
        ENUM_B(2),
        ENUM_C(3);
        IntJsonValueEnum(Integer value) {
            this.value = value;
        }
        @JsonValue
        private Integer value;
    }

    public static class TestDTO {
        public TestDTO() {
        }

        public NoneJsonValueEnum getNoneEnum() {
            return noneEnum;
        }

        public void setNoneEnum(NoneJsonValueEnum noneEnum) {
            this.noneEnum = noneEnum;
        }

        public StringJsonValueEnum getStringEnum() {
            return stringEnum;
        }

        public void setStringEnum(StringJsonValueEnum stringEnum) {
            this.stringEnum = stringEnum;
        }

        public IntJsonValueEnum getIntEnum() {
            return intEnum;
        }

        public void setIntEnum(IntJsonValueEnum intEnum) {
            this.intEnum = intEnum;
        }

        private NoneJsonValueEnum noneEnum;
        private StringJsonValueEnum stringEnum;
        private IntJsonValueEnum intEnum;
    }

    public void testNoneJsonValue() {
        TestDTO testDTO = new TestDTO();
        testDTO.setNoneEnum(NoneJsonValueEnum.ENUM_A);
        testDTO.setStringEnum(StringJsonValueEnum.ENUM_B);
        testDTO.setIntEnum(IntJsonValueEnum.ENUM_C);
        ObjectMapper mapper = new ObjectMapper();
        try {
            String json = mapper.writeValueAsString(testDTO);
            TestDTO obj = mapper.readValue(json, TestDTO.class);
            assertEquals(obj.noneEnum, NoneJsonValueEnum.ENUM_A);
            assertEquals(obj.stringEnum, StringJsonValueEnum.ENUM_B);
            assertEquals(obj.intEnum, IntJsonValueEnum.ENUM_C);
        } catch (JsonProcessingException ex) {
            ex.printStackTrace();
            throw new RuntimeException(ex);
        }
    }
}
