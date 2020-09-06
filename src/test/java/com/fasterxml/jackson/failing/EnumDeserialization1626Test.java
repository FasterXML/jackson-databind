package com.fasterxml.jackson.failing;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.*;

/**
 * NOTE: not assumed to be actual bug -- real numbers are not coerced into
 * Strings, and are instead assumed to always mean index numbers.
 * But test retained in case there might be ways to improve support
 * here: as is, one MUST use Creator method to resolve from number to
 * enum.
 */
public class EnumDeserialization1626Test extends BaseMapTest
{
    static class JsonResponseEnvelope<T> {
        @JsonProperty("d")
        public T data;
    }

    static class ShippingMethodInfo {
        @JsonProperty("typeId")
        public int typeId;

        @JsonProperty("value")
        ShippingMethods value;

        @JsonProperty("coverage")
        public int coverage;
    }

    enum ShippingMethods {
        @JsonProperty("0")
        SHIPPING_METHODS_UNSPECIFIED(0),

        @JsonProperty("10")
        SHIPPING_METHODS_FED_EX_PRIORITY_OVERNIGHT(10),

        @JsonProperty("17")
        SHIPPING_METHODS_FED_EX_1DAY_FREIGHT(17),
        ;

        private final int shippingMethodId;

        ShippingMethods(final int shippingMethodId) {
            this.shippingMethodId = shippingMethodId;
        }

        public int getShippingMethodId() {
            return shippingMethodId;
        }
    }

    /*
    /**********************************************************
    /* Test methods
    /**********************************************************
     */

    protected final ObjectMapper MAPPER = new ObjectMapper();

    // [databind#1626]
    public void testSparseNumericEnum626() throws Exception
    {
        String jsonResponse =
                "{\n" +
                        "    \"d\": [\n" +
                        "        {\n" +
                        "            \"typeId\": 0,\n" +
                        // NOTE! Only real number fails; quoted-as-String is bound as expected
                        "            \"value\": 17,\n" +
                        "            \"coverage\": 1"+
                        "        }\n" +
                        "     ]\n" +
                        "}";

        JsonResponseEnvelope<List<ShippingMethodInfo>> mappedResponse =
                MAPPER.readValue(jsonResponse,
                        new TypeReference<JsonResponseEnvelope<List<ShippingMethodInfo>>>() { });
        List<ShippingMethodInfo> shippingMethods = mappedResponse.data;

        assertEquals(ShippingMethods.SHIPPING_METHODS_FED_EX_1DAY_FREIGHT,
                shippingMethods.get(0).value);
    }
}
