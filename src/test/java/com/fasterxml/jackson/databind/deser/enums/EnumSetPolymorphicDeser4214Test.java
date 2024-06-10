package com.fasterxml.jackson.databind.deser.enums;

import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static com.fasterxml.jackson.databind.testutil.DatabindTestUtil.jsonMapperBuilder;

// For [databind#4214]
public class EnumSetPolymorphicDeser4214Test
{
    static enum MyEnum {
        ITEM_A, ITEM_B;
    }

    static class EnumSetHolder {
        public Set<MyEnum> enumSet; // use Set instead of EnumSet for type of this

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof EnumSetHolder)) {
                return false;
            }
            EnumSetHolder eh = (EnumSetHolder) o;
            return Objects.equals(enumSet, eh.enumSet);
        }
    }

    @Test
    public void testPolymorphicDeserialization4214() throws Exception
    {
        // Need to use Default Typing to trigger issue
        ObjectMapper mapper = jsonMapperBuilder()
                .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().allowIfBaseType(Object.class).build(),
                        DefaultTyping.NON_FINAL_AND_ENUMS)
                .build();

        EnumSetHolder enumSetHolder = new EnumSetHolder();
        enumSetHolder.enumSet = EnumSet.allOf(MyEnum.class);
        String json = mapper.writeValueAsString(enumSetHolder);
        EnumSetHolder result = mapper.readValue(json, EnumSetHolder.class);
        assertEquals(result, enumSetHolder);
    }
}
