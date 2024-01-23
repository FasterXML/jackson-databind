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

// [databind#4327] JsonAlias should respsect with Polymorphic Deduction
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
        @JsonAlias(value = {"Y", "yy", "ff", "X"})
        public int y;
    }


    private final ObjectMapper mapper = jsonMapperBuilder().build();

    @ParameterizedTest
    @ValueSource(strings = {"Y", "yy", "ff", "X"})
    public void testAliasWithPolymorphicDeduction(String field) throws Exception {
        Deduction value = mapper.readValue(a2q(
            "{'%s': 2 }".formatted(field)
        ), Deduction.class);
        assertNotNull(value);
        assertEquals(2, ((DeductionBean2) value).y);
    }
}
