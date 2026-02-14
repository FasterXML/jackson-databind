package tools.jackson.databind.deser.creators;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.exc.MismatchedInputException;

import static org.junit.jupiter.api.Assertions.*;

import static tools.jackson.databind.testutil.DatabindTestUtil.*;

public class CreatorPropertyConstraintsTest
{
    static class FascistPoint {
        Integer x, y;

        @JsonCreator
        public FascistPoint(@JsonProperty(value="x", required=true) Integer x,
                @JsonProperty(value="y", isRequired=OptBoolean.FALSE) Integer y)
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
        public LoginUserResponse(@JsonProperty(value = "otp", isRequired = OptBoolean.TRUE) String otp,
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

    // [databind#2438]
    static class Creator2438 {
        String value = "";

        @JsonCreator
        public Creator2438(@JsonProperty("value") int v) {
            value = "Creator:"+ v;
        }

        // Public setter (or field) required to show the issue
        public void setValue(int v) {
            value = "Setter:" + v;
        }
    }

    // [databind#4119]: READ_ONLY for Creator param (Record or POJO)
    static class Bean4119 {
        String foo, bar;

        @JsonCreator
        public Bean4119(@JsonProperty("foo") String foo,
                        @JsonProperty(value = "bar", access = JsonProperty.Access.READ_ONLY) String bar) {
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

    @Test
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
        assertNull(p.y);

        // but not so good if 'x' missing
        try {
            POINT_READER.readValue(a2q("{'y':3}"));
            fail("Should not pass");
        } catch (MismatchedInputException e) {
            verifyException(e, "Missing required creator property 'x' (index 0)");
        }
    }

    @Test
    public void testRequiredGloballyParam() throws Exception
    {
        FascistPoint p;

        // as per above, ok to miss 'y' with default settings:
        p = POINT_READER.readValue(a2q("{'x':2}"));
        assertEquals(2, p.x);
        assertNull(p.y);

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
    @Test
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

    // [databind#2438]
    @Test
    void testCreatorFallback2438() throws Exception {
        // note: by default, duplicate-detection not enabled, so should not
        // throw exception. But should only pass second value via Creator,
        // not setter or field
        Creator2438 bean = MAPPER.readValue(a2q("{'value':1, 'value':2}"),
                Creator2438.class);
        assertEquals("Creator:2", bean.value);
    }

    // [databind#4119]: READ_ONLY for Creator param (Record or POJO)
    @Test
    void testCreatorWithReadOnly4119() throws Exception {
        Bean4119 bean = MAPPER.readerFor(Bean4119.class)
                .readValue(a2q("{'foo':'a', 'bar':'b'}"));
        assertNotNull(bean);
        assertEquals("a", bean.foo);
        // should either pass `null` (same as [databind#1890]), or, fail
        // with useful exception (and not claiming no name specified)
        assertNull(bean.bar);
    }
}
