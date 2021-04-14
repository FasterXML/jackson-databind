package com.fasterxml.jackson.databind.deser.creators;

import com.fasterxml.jackson.annotation.*;

import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;

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
}
