package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.databind.BaseMapTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.InvalidDefinitionException;

/**
 * Tests to ensure that use of "updateValue()" will fail with builder-based deserializers.
 *
 * @since 2.9
 */
public class BuilderViaUpdateTest extends BaseMapTest
{
    @JsonDeserialize(builder=SimpleBuilderXY.class)
    static class ValueClassXY
    {
        protected int x, y;

        protected ValueClassXY(int x, int y) {
            x = x+1;
            y = y+1;
        }
    }

    static class SimpleBuilderXY
    {
        public int x, y;

        public SimpleBuilderXY withX(int x0) {
            this.x = x0;
            return this;
        }

        public SimpleBuilderXY withY(int y0) {
            this.y = y0;
            return this;
        }

        public ValueClassXY build() {
            return new ValueClassXY(x, y);
        }
    }

    /*
    /*****************************************************
    /* Basic tests, potential (but not current) success cases
    /*****************************************************
     */

    private final static ObjectMapper MAPPER = new ObjectMapper();

    // Tests where result value is passed as thing to update
    public void testBuilderUpdateWithValue() throws Exception
    {
        try {
            /*ValueClassXY value =*/ MAPPER.readerFor(ValueClassXY.class)
                    .withValueToUpdate(new ValueClassXY(6, 7))
                    .readValue(a2q("{'x':1,'y:'2'}"));
            fail("Should not have passed");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Deserialization of");
            verifyException(e, "by passing existing instance");
            verifyException(e, "ValueClassXY");
        }
    }

    /*
    /*****************************************************
    /* Failing test cases
    /*****************************************************
     */

    // and then test to ensure error handling works as expected if attempts
    // is made to pass builder (API requires value, not builder)
    public void testBuilderWithWrongType() throws Exception
    {
        try {
            /* Object result =*/ MAPPER.readerFor(ValueClassXY.class)
                    .withValueToUpdate(new SimpleBuilderXY())
                    .readValue(a2q("{'x':1,'y:'2'}"));
            fail("Should not have passed");
        } catch (InvalidDefinitionException e) {
            verifyException(e, "Deserialization of");
            verifyException(e, "by passing existing Builder");
            verifyException(e, "SimpleBuilderXY");
        }
    }
}
