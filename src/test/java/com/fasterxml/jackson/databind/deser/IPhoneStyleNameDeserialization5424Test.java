package com.fasterxml.jackson.databind.deser;

import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class IPhoneStyleNameDeserialization5424Test
    extends DatabindTestUtil
{
    static class UserInfoParam {
        private String uPhone;
        private String uName;
        private Integer age;

        public String getuPhone() { return uPhone;}
        public String getuName() { return uName;}
        public Integer getAge() { return age;}
        public void setuPhone(String uPhone) { this.uPhone = uPhone;}
        public void setuName(String uName) { this.uName = uName;}
        public void setAge(Integer age) { this.age = age;}
    }

    static class UserInfoParamUpper {
        private String uPhone;
        private String uName;
        private Integer age;
        public String getUPhone() { return uPhone;}
        public String getUName() { return uName;}
        public Integer getAge() { return age;}
        public void setUPhone(String uPhone) { this.uPhone = uPhone;}
        public void setUName(String uName) { this.uName = uName;}
        public void setAge(Integer age) { this.age = age;}
    }

    private final ObjectMapper MAPPER = jsonMapperBuilder()
            .enable(MapperFeature.FIX_FIELD_NAME_UPPER_CASE_PREFIX)
            .build();

    @Test
    public void testDeserializeDefault()
            throws Exception
    {
        String JSON = "{\n" +
                       "  \"uName\": \"www\"," +
                       "  \"uPhone\": \"1234321\"," +
                       "  \"age\": 19" +
                       "}";

        UserInfoParam pojo = MAPPER.readValue(JSON, UserInfoParam.class);

        assertEquals("www", pojo.uName);
        assertEquals("1234321", pojo.uPhone);
        assertEquals(19, pojo.age);
    }

    // Failed without
    @Test
    public void testDeserializeUpperGetterSetter()
            throws Exception
    {
        String JSON = "{\n" +
                "  \"uName\": \"www\"," +
                "  \"uPhone\": \"1234321\"," +
                "  \"age\": 19" +
                "}";

        UserInfoParamUpper pojo = MAPPER.readValue(JSON, UserInfoParamUpper.class);

        assertEquals("www", pojo.uName);
        assertEquals("1234321", pojo.uPhone);
        assertEquals(19, pojo.age);
    }

}
