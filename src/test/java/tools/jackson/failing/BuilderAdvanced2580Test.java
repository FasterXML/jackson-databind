package tools.jackson.failing;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo.As;
import com.fasterxml.jackson.annotation.JsonTypeInfo.Id;

import tools.jackson.databind.*;
import tools.jackson.databind.annotation.JsonDeserialize;

// Test case for a regression in 3.0, worked in 2.11
public class BuilderAdvanced2580Test extends BaseMapTest
{
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

    // [databind#2580]: regression somewhere
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
