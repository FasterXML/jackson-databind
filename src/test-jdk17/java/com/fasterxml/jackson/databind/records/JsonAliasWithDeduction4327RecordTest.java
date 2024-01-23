package com.fasterxml.jackson.databind.records;

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
public class JsonAliasWithDeduction4327RecordTest
{
    @JsonTypeInfo(use = JsonTypeInfo.Id.DEDUCTION)
    @JsonSubTypes({
        @JsonSubTypes.Type(value = DeductionBean1.class),
        @JsonSubTypes.Type(value = DeductionBean2.class)
    })
    interface Deduction { }

    record DeductionBean1(int x) implements Deduction { }

    record DeductionBean2(
        @JsonAlias(value = {"Y", "yy", "ff", "X"}) int y
    ) implements Deduction { }


    private final ObjectMapper mapper = jsonMapperBuilder().build();

    @ParameterizedTest
    @ValueSource(strings = {"Y", "yy", "ff", "X"})
    public void testAliasWithPolymorphicDeduction(String field) throws Exception {
        String json = a2q(String.format("{'%s': 2 }", field));
        Deduction value = mapper.readValue(json, Deduction.class);
        assertNotNull(value);
        assertEquals(2, ((DeductionBean2) value).y());
    }
}
