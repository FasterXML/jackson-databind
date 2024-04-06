package com.fasterxml.jackson.databind.ser.enums;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class EnumSerializationMixinTest extends DatabindTestUtil
{

    static enum EnumBaseA {
        ITEM_A {
            @Override
            public String toString() {
                return "A_base";
            }
        },

        @JsonAlias({"B_ORIGIN_ALIAS_1", "B_ORIGIN_ALIAS_2"})
        @JsonProperty("B_ORIGIN_PROP")
        ITEM_B,

        @JsonAlias({"C_ORIGIN_ALIAS"})
        @JsonProperty("C_COMMON")
        ITEM_C_BASE,
        
        ITEM_ORIGIN
    }

    static enum EnumMixinA {
        ITEM_A {
            @Override
            public String toString() {
                return "A_mixin";
            }
        },

        @JsonProperty("B_MIXIN_PROP")
        ITEM_B,

        @JsonAlias({"C_MIXIN_ALIAS_1", "C_MIXIN_ALIAS_2"})
        @JsonProperty("C_COMMON")
        ITEM_C_MIXIN,

        ITEM_MIXIN;

        @Override
        public String toString() {
            return "SHOULD NOT USE WITH TO STRING";
        }
    }

    @Test
    public void testSerialization() throws Exception {
        ObjectMapper mixinMapper = jsonMapperBuilder()
                .addMixIn(EnumBaseA.class, EnumMixinA.class).build();

        // equal name(), different toString() value
        assertEquals(q("ITEM_A"), _w(EnumBaseA.ITEM_A, mixinMapper));
        
        // equal name(), differnt @JsonProperty
        assertEquals(q("B_MIXIN_PROP"), _w(EnumBaseA.ITEM_B, mixinMapper));
        
        // different name(), equal @JsonProperty
        assertEquals(q("C_COMMON"), _w(EnumBaseA.ITEM_C_BASE, mixinMapper));
        
        // different name(), equal ordinal()
        assertEquals(q("ITEM_ORIGIN"), _w(EnumBaseA.ITEM_ORIGIN, mixinMapper));
    }

    /**
     * Helper method to {@link ObjectMapper#writeValueAsString(Object)}
     */
    private <T> String _w(T value, ObjectMapper mapper) throws Exception {
        return mapper.writeValueAsString(value);
    }
}
