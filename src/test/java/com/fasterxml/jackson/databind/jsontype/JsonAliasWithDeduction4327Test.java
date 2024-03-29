package com.fasterxml.jackson.databind.jsontype;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.a2q;
import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

// [databind#4327] JsonAlias should be respected by Polymorphic Deduction
public class JsonAliasWithDeduction4327Test
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DeductionBean1.class),
        @JsonSubTypes.Type(value = DeductionBean2.class)
    })
    interface Deduction { }

    static class DeductionBean1 implements Deduction {
        public int x;
    }

    static class DeductionBean2 implements Deduction {
        // add "y" as redundant choice to make sure it won't break anything
        @JsonAlias(value = {"y", "Y", "yy", "ff", "X"})
        public int y;

        // IMPORTANT! Can have field and setter, but alias values are not merged;
        // highest priority one is used instead of lower if both defined (Setter
        // having higher priority than Field)
        public void setY(int y) { this.y = y; }
    }

    private final ObjectMapper mapper = jsonMapperBuilder().build();

    @ParameterizedTest
    @ValueSource(strings = {"y", "Y", "yy", "ff", "X"})
    public void testAliasWithPolymorphicDeduction(String field) throws Exception {
        String json = a2q(String.format("{'%s': 2 }", field));
        Deduction value = mapper.readValue(json, Deduction.class);
        assertNotNull(value);
        assertEquals(2, ((DeductionBean2) value).y);
    }
}
