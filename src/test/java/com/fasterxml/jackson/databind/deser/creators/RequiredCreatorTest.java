package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.core.JacksonException;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.jsontype.ext.ExternalTypeIdWithIgnoreUnknownTest;
import java.io.IOException;
import java.util.List;

public class RequiredCreatorTest extends BaseMapTest
{
    static class FascistPoint {
        int x, y;

        @JsonCreator
        public FascistPoint(@JsonProperty(value="x", required=true) int x,
                @JsonProperty(value="y", required=false) int y)
        {
            this.x = x;
            this.y = y;
        }
    }

    // [databind#2591]
    static class LoginUserResponse {
        private String otp;

        private String userType;

        @JsonCreator
        public LoginUserResponse(@JsonProperty(value = "otp", required = true) String otp,
                @JsonProperty(value = "userType", required = true) String userType) {
            this.otp = otp;
            this.userType = userType;
        }

        public String getOtp() {
            return otp;
        }

        public void setOtp(String otp) {
            this.otp = otp;
        }

        public String getUserType() {
            return userType;
        }

        public void setUserType(String userType) {
            this.userType = userType;
        }
    }

    // [databind#4201]
    static class MyDeser4201 extends StdDeserializer<String> {
        public MyDeser4201() {
            super(String.class);
        }

        @Override
        public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException, JacksonException {
            return p.getValueAsString();
        }

        @Override
        public Object getAbsentValue(DeserializationContext ctxt) {
            return "absent";
        }
    }

    static class Dto4201 {
        private final String foo;
        private final String bar;

        @JsonCreator
        Dto4201(
                @JsonProperty(value = "foo", required = false)
                @JsonDeserialize(using = MyDeser4201.class)
                String foo,
                @JsonDeserialize(using = MyDeser4201.class)
                @JsonProperty(value = "bar", required = true)
                String bar
        ) {
            this.foo = foo;
            this.bar = bar;
        }
    }

    /*
    /**********************************************************************
    /* Test methods
    /**********************************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();
    private final ObjectReader POINT_READER = MAPPER.readerFor(FascistPoint.class);

    public void testRequiredAnnotatedParam() throws Exception
    {
        FascistPoint p;

        // First: fine if both params passed
        p = POINT_READER.readValue(a2q("{'y':2,'x':1}"));
        assertEquals(1, p.x);
        assertEquals(2, p.y);
        p = POINT_READER.readValue(a2q("{'x':3,'y':4}"));
        assertEquals(3, p.x);
        assertEquals(4, p.y);

        // also fine if 'y' is MIA
        p = POINT_READER.readValue(a2q("{'x':3}"));
        assertEquals(3, p.x);
        assertEquals(0, p.y);

        // but not so good if 'x' missing
        try {
            POINT_READER.readValue(a2q("{'y':3}"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Missing required creator property 'x' (index 0)");
        }
    }

    public void testRequiredGloballyParam() throws Exception
    {
        FascistPoint p;

        // as per above, ok to miss 'y' with default settings:
        p = POINT_READER.readValue(a2q("{'x':2}"));
        assertEquals(2, p.x);
        assertEquals(0, p.y);

        // but not if global checks desired
        ObjectReader r = POINT_READER.with(DeserializationFeature.FAIL_ON_MISSING_CREATOR_PROPERTIES);
        try {
            r.readValue(a2q("{'x':6}"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Missing creator property 'y' (index 1)");
        }
    }

    // [databind#2591]
    public void testRequiredViaParameter2591() throws Exception
    {
        final String input = a2q("{'status':'OK', 'message':'Sent Successfully!'}");
        try {
            /*LoginUserResponse resp =*/ MAPPER.readValue(input, LoginUserResponse.class);
            fail("Shoud not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Missing required creator property 'otp'");
        }
    }

    // [databind#4201]
    public void testRequiredValueAbsentValueOrder() throws JsonProcessingException {
        ObjectMapper mapper = new ObjectMapper();

        Dto4201 r1 = mapper.readValue("{\"bar\":\"value\"}", Dto4201.class);
        assertEquals("absent", r1.foo);
        assertEquals("value", r1.bar);

        // -> throws MismatchedInputException: Missing required creator property 'bar' (index 1)
        Dto4201 r2 = mapper.readValue("{}", Dto4201.class);
        assertEquals("absent", r2.foo);
        assertEquals("absent", r2.bar);
    }
}
