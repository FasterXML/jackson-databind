package com.fasterxml.jackson.databind.introspect;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.fasterxml.jackson.databind.*;

public class AccessorNamingForBuilderTest extends BaseMapTest
{
    @JsonDeserialize(builder=NoPrexixBuilderXY.class)
    static class ValueClassXY
    {
        final int _x, _y;

        protected ValueClassXY(int x, int y) {
            _x = x+1;
            _y = y+1;
        }
    }

    static class NoPrexixBuilderXY
    {
        // non-public so as not to discover without prefix-match
        protected int x, y;

        public NoPrexixBuilderXY x(int x0) {
              this.x = x0;
              return this;
        }

        public NoPrexixBuilderXY y(int y0) {
              this.y = y0;
              return this;
        }

        public ValueClassXY build() {
              return new ValueClassXY(x, y);
        }
    }

    // For [databind#2624]
    public void testAccessorCustomWithMethod() throws Exception
    {
        final String json = a2q("{'x':28,'y':72}");
        final ObjectMapper vanillaMapper = newJsonMapper();

        // First: without custom strategy, will fail:
        try {
            ValueClassXY xy = vanillaMapper.readValue(json, ValueClassXY.class);
            fail("Should not pass, got instance with x="+xy._x+", y="+xy._y);
        } catch (UnrecognizedPropertyException e) {
            verifyException(e, "Unrecognized ");
        }

        // But fine with "no-prefix"
        final ObjectMapper customMapper = jsonMapperBuilder()
                .accessorNaming(new DefaultAccessorNamingStrategy.Provider()
                        .withBuilderPrefix("")
                )
                .build();
        ValueClassXY xy = customMapper.readValue(json, ValueClassXY.class);
        assertEquals(29, xy._x);
        assertEquals(73, xy._y);
    }
}
