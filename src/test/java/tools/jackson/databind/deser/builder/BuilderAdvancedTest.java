package tools.jackson.databind.deser.builder;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;
import tools.jackson.databind.testutil.DatabindTestUtil;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

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

    // [databind#2580]
    @JsonDeserialize(builder=ExternalBuilder2580.class)
    static class ExternalBean2580
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object value;

        public ExternalBean2580(Object v) {
            value = v;
        }
    }

    @JsonSubTypes({ @JsonSubTypes.Type(ValueBean2580.class) })
    static class BaseBean2580 {
    }

    @JsonTypeName("vbean")
    static class ValueBean2580 extends BaseBean2580
    {
        public int value;

        public ValueBean2580() { }
        public ValueBean2580(int v) { value = v; }
    }

    static class ExternalBuilder2580
    {
        BaseBean2580 value;

        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public ExternalBuilder2580 withValue(BaseBean2580 b) {
            value = b;
            return this;
        }

        public ExternalBean2580 build() {
              return new ExternalBean2580(value);
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

    // [databind#2580]: regression in 3.0, fixed in 3.0.4
    @Test
    public void testWithExternalTypeId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        final ExternalBean2580 input = new ExternalBean2580(new ValueBean2580(13));
        String json = mapper.writeValueAsString(input);
        ExternalBean2580 result = mapper.readValue(json, ExternalBean2580.class);
        assertNotNull(result.value);
        assertEquals(ValueBean2580.class, result.value.getClass());
        assertEquals(13, ((ValueBean2580) result.value).value);
    }

}
