package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class BuilderAdvancedTest extends DatabindTestUtil
{
    @JsonDeserialize(builder=InjectableBuilderXY.class)
    static class InjectableXY
    {
        final int _x, _y;
        final String _stuff;

        protected InjectableXY(int x, int y, String stuff) {
            _x = x+1;
            _y = y+1;
            _stuff = stuff;
        }
    }

    static class InjectableBuilderXY
    {
        public int x, y;

        @JacksonInject
        protected String stuff;

        public InjectableBuilderXY withX(int x0) {
              this.x = x0;
              return this;
        }

        public InjectableBuilderXY withY(int y0) {
              this.y = y0;
              return this;
        }

        public InjectableXY build() {
              return new InjectableXY(x, y, stuff);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    @Test
    public void testWithInjectable() throws Exception
    {
        ObjectMapper mapper = jsonMapperBuilder()
                .injectableValues(new InjectableValues.Std()
                        .addValue(String.class, "stuffValue"))
                .build();
        InjectableXY bean = mapper.readValue(a2q("{'y':3,'x':7}"),
                InjectableXY.class);
        assertEquals(8, bean._x);
        assertEquals(4, bean._y);
        assertEquals("stuffValue", bean._stuff);
    }

    // NOTE: failing test from [databind#2580] moved in 3.x (included here for 2.11)
}
