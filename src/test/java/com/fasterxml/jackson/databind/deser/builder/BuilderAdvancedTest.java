package com.fasterxml.jackson.databind.deser.builder;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;
import com.fasterxml.jackson.databind.*;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;

public class BuilderAdvancedTest extends BaseMapTest
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

    @JsonDeserialize(builder=ExternalBuilder.class)
    static class ExternalBean
    {
        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public Object value;

        public ExternalBean(Object v) {
            value = v;
        }
    }

    @JsonSubTypes({ @JsonSubTypes.Type(ValueBean.class) })
    static class BaseBean {
    }

    @JsonTypeName("vbean")
    static class ValueBean extends BaseBean
    {
        public int value;

        public ValueBean() { }
        public ValueBean(int v) { value = v; }
    }

    static class ExternalBuilder
    {
        BaseBean value;

        @JsonTypeInfo(use=Id.NAME, include=As.EXTERNAL_PROPERTY, property="extType")
        public ExternalBuilder withValue(BaseBean b) {
            value = b;
            return this;
        }

        public ExternalBean build() {
              return new ExternalBean(value);
        }
    }

    /*
    /**********************************************************
    /* Unit tests
    /**********************************************************
     */

    public void testWithInjectable() throws Exception
    {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setInjectableValues(new InjectableValues.Std()
            .addValue(String.class, "stuffValue")
            );
        InjectableXY bean = mapper.readValue(a2q("{'y':3,'x':7}"),
                InjectableXY.class);
        assertEquals(8, bean._x);
        assertEquals(4, bean._y);
        assertEquals("stuffValue", bean._stuff);
    }

    public void testWithExternalTypeId() throws Exception
    {
        ObjectMapper mapper = newJsonMapper();
        final ExternalBean input = new ExternalBean(new ValueBean(13));
        String json = mapper.writeValueAsString(input);
        ExternalBean result = mapper.readValue(json, ExternalBean.class);
        assertNotNull(result.value);
        assertEquals(ValueBean.class, result.value.getClass());
        assertEquals(13, ((ValueBean) result.value).value);
    }
}
