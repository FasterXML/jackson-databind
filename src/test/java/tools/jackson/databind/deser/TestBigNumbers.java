package tools.jackson.databind.deser;

import tools.jackson.core.StreamReadConstraints;
import tools.jackson.core.json.JsonFactory;

import tools.jackson.databind.*;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.math.BigDecimal;
import java.math.BigInteger;

public class TestBigNumbers extends BaseMapTest
{
    static class BigDecimalWrapper {
        BigDecimal number;

        public BigDecimalWrapper() {}

        public BigDecimalWrapper(BigDecimal number) {
            this.number = number;
        }

        public void setNumber(BigDecimal number) {
            this.number = number;
        }
    }

    static class BigIntegerWrapper {
        BigInteger number;

        public BigIntegerWrapper() {}

        public BigIntegerWrapper(BigInteger number) {
            this.number = number;
        }

        public void setNumber(BigInteger number) {
            this.number = number;
        }
    }

    /*
    /**********************************************************
    /* Tests
    /**********************************************************
     */

    private final ObjectMapper MAPPER = newJsonMapper();

    private final ObjectMapper newJsonMapperWithUnlimitedNumberSizeSupport() {
        JsonFactory jsonFactory = JsonFactory.builder()
                .streamReadConstraints(StreamReadConstraints.builder().maxNumberLength(Integer.MAX_VALUE).build())
                .build();
        return JsonMapper.builder(jsonFactory).build();
    }

    public void testDouble() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("d"), DoubleWrapper.class);
        } catch (DatabindException jsonMappingException) {
            assertTrue("unexpected exception message: " + jsonMappingException.getMessage(),
                    jsonMappingException.getMessage().startsWith("Malformed numeric value ([number with 1200 characters])"));
        }
    }

    public void testDoubleUnlimited() throws Exception
    {
        DoubleWrapper dw =
            newJsonMapperWithUnlimitedNumberSizeSupport().readValue(generateJson("d"), DoubleWrapper.class);
        assertNotNull(dw);
    }

    public void testBigDecimal() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigDecimalWrapper.class);
        } catch (DatabindException jsonMappingException) {
            assertTrue("unexpected exception message: " + jsonMappingException.getMessage(),
                    jsonMappingException.getMessage().startsWith("Malformed numeric value ([number with 1200 characters])"));
        }
    }

    public void testBigDecimalUnlimited() throws Exception
    {
        BigDecimalWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigDecimalWrapper.class);
        assertNotNull(bdw);
    }

    public void testBigInteger() throws Exception
    {
        try {
            MAPPER.readValue(generateJson("number"), BigIntegerWrapper.class);
        } catch (DatabindException jsonMappingException) {
            assertTrue("unexpected exception message: " + jsonMappingException.getMessage(),
                    jsonMappingException.getMessage().startsWith("Malformed numeric value ([number with 1200 characters])"));
        }
    }

    public void testBigIntegerUnlimited() throws Exception
    {
        BigIntegerWrapper bdw =
                newJsonMapperWithUnlimitedNumberSizeSupport()
                        .readValue(generateJson("number"), BigIntegerWrapper.class);
        assertNotNull(bdw);
    }

    private String generateJson(final String fieldName) {
        final int len = 1200;
        final StringBuilder sb = new StringBuilder();
        sb.append("{\"")
                .append(fieldName)
                .append("\": ");
        for (int i = 0; i < len; i++) {
            sb.append(1);
        }
        sb.append("}");
        return sb.toString();
    }
}
