package com.fasterxml.jackson.databind.deser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonValue;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class EnumDeserializer5271Test
        extends DatabindTestUtil
{
    public enum MyEnum {
        T10("10%"), T20("20%"), T30("30%");

        private final String code;

        MyEnum(String code) {
            this.code = code;
        }

        @JsonValue
        public String getCode() {
            return code;
        }
    }

    @Test
    void convertStringToEnum() {
        _testConvert(
                jsonMapperBuilder().disable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                        .build()
        );
        _testConvert(jsonMapperBuilder().enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING)
                .build()
        );
    }

    private void _testConvert(JsonMapper mapper) {
        assertThat(mapper.convertValue("10%", MyEnum.class))
                .isEqualTo(MyEnum.T10);
    }

}